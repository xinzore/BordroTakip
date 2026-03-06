package com.bordrotakip.ui.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.bordrotakip.domain.model.EmploymentType
import com.bordrotakip.ui.settings.CalendarSyncSettingsViewModel
import com.bordrotakip.ui.settings.NotificationSettingsViewModel
import com.bordrotakip.util.toLocalizedDoubleOrNull
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
    notificationViewModel: NotificationSettingsViewModel = hiltViewModel(),
    calendarSyncViewModel: CalendarSyncSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val notificationUiState by notificationViewModel.uiState.collectAsState()
    val calendarSyncUiState by calendarSyncViewModel.uiState.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var pendingEnableShiftPrompt by remember { mutableStateOf(false) }
    var pendingEnableTaxAlerts by remember { mutableStateOf(false) }

    val requestNotificationsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            if (pendingEnableShiftPrompt) notificationViewModel.setShiftPromptEnabled(true)
            if (pendingEnableTaxAlerts) notificationViewModel.setTaxBracketAlertsEnabled(true)
        } else {
            scope.launch { snackbarHostState.showSnackbar(message = "Bildirim izni verilmedi") }
        }
        pendingEnableShiftPrompt = false
        pendingEnableTaxAlerts = false
    }

    val requestCalendarPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants.values.all { it }
        if (granted) {
            calendarSyncViewModel.setEnabled(true)
        } else {
            scope.launch { snackbarHostState.showSnackbar(message = "Takvim izni verilmedi") }
        }
    }

    var showCalendarPicker by remember { mutableStateOf(false) }

    LaunchedEffect(showCalendarPicker) {
        if (showCalendarPicker) {
            calendarSyncViewModel.refreshCalendars()
        }
    }

    LaunchedEffect(calendarSyncUiState.message) {
        val message = calendarSyncUiState.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message = message)
        calendarSyncViewModel.consumeMessage()
    }

    LaunchedEffect(uiState.isCompleted) {
        if (uiState.isCompleted) onFinished()
    }

    val canProceedFromSalary = (uiState.grossSalary.toLocalizedDoubleOrNull() ?: 0.0) > 0.0
    val canFinishEarly = uiState.stepIndex >= 2 && canProceedFromSalary && !uiState.isSaving

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Kurulum (${uiState.stepIndex + 1}/6)") },
                actions = {
                    if (canFinishEarly) {
                        TextButton(onClick = { viewModel.finish() }) { Text("Bitir") }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (uiState.stepIndex) {
                0 -> StepSalaryAndCutoff(
                    uiState = uiState,
                    onGrossChange = viewModel::updateGrossSalary,
                    onCutoffChange = viewModel::updateCutoffDay,
                    onEmploymentTypeChange = viewModel::updateEmploymentType
                )
                1 -> StepWeeklyRest(uiState = uiState, onToggleDay = viewModel::toggleNonWorkingDay, onSetMask = viewModel::setNonWorkingDaysMask)
                2 -> StepDefaultShiftHours(uiState = uiState, onHoursChange = viewModel::updateDefaultShiftHours)
                3 -> StepBes(uiState = uiState, onBesChange = viewModel::updateBesEnabled)
                4 -> StepNotifications(
                    isShiftPromptEnabled = notificationUiState.isShiftPromptEnabled,
                    isTaxAlertsEnabled = notificationUiState.isTaxBracketAlertsEnabled,
                    onShiftPromptToggle = { enabled ->
                        if (!enabled) {
                            notificationViewModel.setShiftPromptEnabled(false)
                            return@StepNotifications
                        }

                        val hasPermission = if (Build.VERSION.SDK_INT >= 33) {
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                                PackageManager.PERMISSION_GRANTED
                        } else {
                            true
                        }

                        if (hasPermission) {
                            notificationViewModel.setShiftPromptEnabled(true)
                        } else {
                            pendingEnableShiftPrompt = true
                            requestNotificationsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    onTaxAlertsToggle = { enabled ->
                        if (!enabled) {
                            notificationViewModel.setTaxBracketAlertsEnabled(false)
                            return@StepNotifications
                        }

                        val hasPermission = if (Build.VERSION.SDK_INT >= 33) {
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                                PackageManager.PERMISSION_GRANTED
                        } else {
                            true
                        }

                        if (hasPermission) {
                            notificationViewModel.setTaxBracketAlertsEnabled(true)
                        } else {
                            pendingEnableTaxAlerts = true
                            requestNotificationsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                )
                5 -> StepCalendarSync(
                    uiState = calendarSyncUiState,
                    onEnabledChange = { enabled ->
                        if (!enabled) {
                            calendarSyncViewModel.setEnabled(false)
                            return@StepCalendarSync
                        }

                        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
                            PackageManager.PERMISSION_GRANTED &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) ==
                            PackageManager.PERMISSION_GRANTED

                        if (hasPermission) {
                            calendarSyncViewModel.setEnabled(true)
                        } else {
                            requestCalendarPermissionsLauncher.launch(
                                arrayOf(
                                    Manifest.permission.READ_CALENDAR,
                                    Manifest.permission.WRITE_CALENDAR
                                )
                            )
                        }
                    },
                    onPickCalendar = { showCalendarPicker = true },
                    onSyncNow = { calendarSyncViewModel.syncNow() }
                )
            }

            val errorMessage = uiState.errorMessage
            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { viewModel.prevStep() },
                    enabled = uiState.stepIndex > 0 && !uiState.isSaving
                ) {
                    Text("Geri")
                }

                Button(
                    onClick = {
                        if (uiState.stepIndex == OnboardingViewModel.LAST_STEP_INDEX) {
                            viewModel.finish()
                        } else {
                            viewModel.nextStep()
                        }
                    },
                    enabled = !uiState.isSaving && (uiState.stepIndex != 0 || canProceedFromSalary)
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.size(10.dp))
                    }
                    Text(
                        if (uiState.stepIndex == OnboardingViewModel.LAST_STEP_INDEX) "Başla" else "Devam"
                    )
                }
            }
        }
    }

    if (showCalendarPicker) {
        AlertDialog(
            onDismissRequest = { showCalendarPicker = false },
            title = { Text("Takvim Seç") },
            text = {
                if (calendarSyncUiState.isLoadingCalendars) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (calendarSyncUiState.availableCalendars.isEmpty()) {
                    Text("Takvim bulunamadı (izin verildi mi?)")
                } else {
                    Column {
                        calendarSyncUiState.availableCalendars.forEach { cal ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = calendarSyncUiState.selectedCalendarId == cal.id,
                                    onClick = {
                                        calendarSyncViewModel.selectCalendar(cal)
                                        showCalendarPicker = false
                                    }
                                )
                                Text(cal.getDisplayLabel())
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCalendarPicker = false }) { Text("Kapat") }
            }
        )
    }
}

@Composable
private fun StepSalaryAndCutoff(
    uiState: OnboardingUiState,
    onGrossChange: (String) -> Unit,
    onCutoffChange: (Int) -> Unit,
    onEmploymentTypeChange: (EmploymentType) -> Unit
) {
    Text(
        text = "Brüt Maaş & Kesim Günü",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
    )
    Text(
        text = "Tahminlerin doğru olması için brüt maaşını girmen gerekiyor.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    OutlinedTextField(
        value = uiState.grossSalary,
        onValueChange = onGrossChange,
        label = { Text("Aylık Brüt (TL)") },
        modifier = Modifier.fillMaxWidth(),
        suffix = { Text("₺") }
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "Çalışma statüsü",
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Medium
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = uiState.employmentType == EmploymentType.STANDARD,
            onClick = { onEmploymentTypeChange(EmploymentType.STANDARD) },
            label = { Text("Normal (4A)") }
        )
        FilterChip(
            selected = uiState.employmentType == EmploymentType.RETIRED,
            onClick = { onEmploymentTypeChange(EmploymentType.RETIRED) },
            label = { Text("Emekli (SGDP)") }
        )
    }

    Text(
        text = "Emekli seçilirse SGDP (%7,5) uygulanır, işsizlik kesintisi alınmaz.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Text(
        text = "Bordro kesim günü: Ayın ${uiState.cutoffDay}. günü",
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Medium
    )
    Slider(
        value = uiState.cutoffDay.toFloat(),
        onValueChange = { onCutoffChange(it.toInt()) },
        valueRange = 20f..28f,
        steps = 7
    )
    Text(
        text = "Örn: Kesim günü 24 ise dönem genelde 25 → 24 arasıdır.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun StepWeeklyRest(
    uiState: OnboardingUiState,
    onToggleDay: (DayOfWeek) -> Unit,
    onSetMask: (Int) -> Unit
) {
    Text(
        text = "Hafta Tatili",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
    )
    Text(
        text = "Tatil günlerini seç (mesaiyi normal/hafta sonu sınıflandırması için). Örn: sadece Pazar.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = { onSetMask(OnboardingViewModel.NON_WORKING_SUNDAY_ONLY) },
            modifier = Modifier.weight(1f)
        ) {
            Text("Sadece Pazar")
        }
        OutlinedButton(
            onClick = { onSetMask(OnboardingViewModel.NON_WORKING_SAT_SUN) },
            modifier = Modifier.weight(1f)
        ) {
            Text("Cmt + Paz")
        }
    }

    val dayChips = listOf(
        DayOfWeek.MONDAY to "Pzt",
        DayOfWeek.TUESDAY to "Sal",
        DayOfWeek.WEDNESDAY to "Çar",
        DayOfWeek.THURSDAY to "Per",
        DayOfWeek.FRIDAY to "Cum",
        DayOfWeek.SATURDAY to "Cmt",
        DayOfWeek.SUNDAY to "Paz"
    )

    val chipRows = listOf(dayChips.take(4), dayChips.drop(4))
    chipRows.forEach { row ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            row.forEach { (day, label) ->
                val bit = 1 shl (day.value - 1)
                val selected = (uiState.nonWorkingDaysMask and bit) != 0
                FilterChip(
                    selected = selected,
                    onClick = { onToggleDay(day) },
                    label = { Text(label) }
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun StepDefaultShiftHours(
    uiState: OnboardingUiState,
    onHoursChange: (Float) -> Unit
) {
    Text(
        text = "Vardiya Süresi",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
    )
    Text(
        text = "Vardiya eklerken açılan ekranda saat çubuğunun varsayılanı.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    val hourOptions = remember {
        listOf(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 7.5, 8.0, 9.0, 10.0, 11.0, 12.0)
    }
    var hoursIndexValue by rememberSaveable(uiState.stepIndex) {
        mutableStateOf(findClosestIndex(hourOptions, uiState.defaultShiftHours.toDouble()).toFloat())
    }

    val index = hoursIndexValue.roundToInt().coerceIn(0, hourOptions.lastIndex)
    val selectedHours = hourOptions[index]
    Text(
        text = "Varsayılan: ${formatHours(selectedHours)} saat",
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Medium
    )

    Slider(
        value = hoursIndexValue,
        onValueChange = {
            hoursIndexValue = it
            val i = it.roundToInt().coerceIn(0, hourOptions.lastIndex)
            onHoursChange(hourOptions[i].toFloat())
        },
        valueRange = 0f..hourOptions.lastIndex.toFloat(),
        steps = (hourOptions.size - 2).coerceAtLeast(0)
    )

    Text(
        text = "Not: Mesai eklerken varsayılan 8 saat olarak kalır.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun StepBes(
    uiState: OnboardingUiState,
    onBesChange: (Boolean) -> Unit
) {
    Text(
        text = "BES (Otomatik Katılım)",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
    )
    Text(
        text = "Açıksa maaştan %3 kesinti yapılır.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("BES aktif", style = MaterialTheme.typography.titleMedium)
        Switch(checked = uiState.isBesEnabled, onCheckedChange = onBesChange)
    }
}

@Composable
private fun StepNotifications(
    isShiftPromptEnabled: Boolean,
    isTaxAlertsEnabled: Boolean,
    onShiftPromptToggle: (Boolean) -> Unit,
    onTaxAlertsToggle: (Boolean) -> Unit
) {
    Text(
        text = "Bildirimler (Opsiyonel)",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
    )
    Text(
        text = "İstersen sonra Ayarlar’dan da açabilirsin.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Vardiya hatırlatma", style = MaterialTheme.typography.titleMedium)
            Text("Her gün 20:00 civarı: “Bugün hangi vardiyadaydın?”", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = isShiftPromptEnabled, onCheckedChange = onShiftPromptToggle)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Vergi dilimi uyarıları", style = MaterialTheme.typography.titleMedium)
            Text("80% / 95% / dilime girdin hatırlatmaları", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = isTaxAlertsEnabled, onCheckedChange = onTaxAlertsToggle)
    }
}

@Composable
private fun StepCalendarSync(
    uiState: com.bordrotakip.ui.settings.CalendarSyncSettingsUiState,
    onEnabledChange: (Boolean) -> Unit,
    onPickCalendar: () -> Unit,
    onSyncNow: () -> Unit
) {
    Text(
        text = "Google Takvim (Opsiyonel)",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
    )
    Text(
        text = "Vardiyalarını Google Takvim’e senkronize etmek istersen açabilirsin.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Senkronizasyon", style = MaterialTheme.typography.titleMedium)
        Switch(checked = uiState.isEnabled, onCheckedChange = onEnabledChange)
    }

    if (uiState.isEnabled) {
        OutlinedButton(onClick = onPickCalendar, modifier = Modifier.fillMaxWidth()) {
            Text(uiState.selectedCalendarDisplayName ?: "Takvim seç")
        }

        Button(
            onClick = onSyncNow,
            enabled = !uiState.isSyncing && uiState.selectedCalendarId != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState.isSyncing) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.size(10.dp))
            }
            Text(if (uiState.isSyncing) "Senkronize ediliyor" else "Şimdi senkronize et")
        }
    }
}

private fun findClosestIndex(options: List<Double>, value: Double): Int {
    var bestIndex = 0
    var bestDistance = Double.MAX_VALUE
    options.forEachIndexed { index, option ->
        val distance = abs(option - value)
        if (distance < bestDistance) {
            bestIndex = index
            bestDistance = distance
        }
    }
    return bestIndex
}

private fun formatHours(hours: Double): String {
    val rounded = (hours * 2.0).roundToInt() / 2.0
    val isInt = abs(rounded - rounded.toLong().toDouble()) < 1e-9
    return if (isInt) rounded.toLong().toString() else rounded.toString().replace('.', ',')
}
