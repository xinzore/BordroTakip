package com.bordrotakip.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.bordrotakip.data.local.entity.CompanyProfileEntity
import com.bordrotakip.domain.model.EmploymentType
import com.bordrotakip.util.formatLocalizedCurrency
import com.bordrotakip.util.toLocalizedDoubleOrNull
import kotlinx.coroutines.launch
import java.time.DayOfWeek

private enum class SettingsSection(val title: String) {
    HOME("Ayarlar"),
    PROFILE("Şirket Profili"),
    PAYROLL("Bordro Kuralları"),
    NOTIFICATIONS("Bildirimler"),
    SYNC("Senkronizasyon"),
    ABOUT("Geliştirici Hakkında")
}

private enum class NotificationPermissionTarget {
    SHIFT_PROMPT,
    TAX_ALERTS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val notificationViewModel: NotificationSettingsViewModel = hiltViewModel()
    val notificationUiState by notificationViewModel.uiState.collectAsState()
    val calendarSyncViewModel: CalendarSyncSettingsViewModel = hiltViewModel()
    val calendarSyncUiState by calendarSyncViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val snackbarHostState = remember { SnackbarHostState() }
    var showCalendarPicker by remember { mutableStateOf(false) }
    var currentSection by rememberSaveable { mutableStateOf(SettingsSection.HOME) }
    var pendingNotificationPermissionTarget by remember { mutableStateOf<NotificationPermissionTarget?>(null) }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            snackbarHostState.showSnackbar(message = "Kaydedildi")
            viewModel.consumeSaveSuccess()
        }
    }

    LaunchedEffect(calendarSyncUiState.message) {
        val message = calendarSyncUiState.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message = message)
        calendarSyncViewModel.consumeMessage()
    }

    val requestNotificationsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            when (pendingNotificationPermissionTarget) {
                NotificationPermissionTarget.SHIFT_PROMPT -> notificationViewModel.setShiftPromptEnabled(true)
                NotificationPermissionTarget.TAX_ALERTS -> notificationViewModel.setTaxBracketAlertsEnabled(true)
                null -> Unit
            }
        } else {
            scope.launch { snackbarHostState.showSnackbar(message = "Bildirim izni verilmedi") }
        }
        pendingNotificationPermissionTarget = null
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

    LaunchedEffect(showCalendarPicker) {
        if (showCalendarPicker) {
            calendarSyncViewModel.refreshCalendars()
        }
    }

    val canNavigateInsideSettings = currentSection != SettingsSection.HOME

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(currentSection.title) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (canNavigateInsideSettings) {
                                currentSection = SettingsSection.HOME
                            } else {
                                onNavigateBack()
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (currentSection) {
                SettingsSection.HOME -> SettingsHomeSection(
                    uiState = uiState,
                    notificationUiState = notificationUiState,
                    calendarSyncUiState = calendarSyncUiState,
                    onOpenProfile = { currentSection = SettingsSection.PROFILE },
                    onOpenPayroll = { currentSection = SettingsSection.PAYROLL },
                    onOpenNotifications = { currentSection = SettingsSection.NOTIFICATIONS },
                    onOpenSync = { currentSection = SettingsSection.SYNC },
                    onOpenAbout = { currentSection = SettingsSection.ABOUT }
                )

                SettingsSection.PROFILE -> ProfileSettingsSection(
                    uiState = uiState,
                    onCompanyNameChange = viewModel::updateCompanyName,
                    onGrossSalaryChange = viewModel::updateGrossSalary,
                    onEmploymentTypeChange = viewModel::updateEmploymentType,
                    onCumulativeTaxBaseChange = viewModel::updateCumulativeTaxBase
                )

                SettingsSection.PAYROLL -> PayrollRulesSection(
                    uiState = uiState,
                    onCutoffDayChange = viewModel::updateCutoffDay,
                    onToggleNonWorkingDay = viewModel::toggleNonWorkingDay,
                    onOvertimeMultiplierNormalChange = viewModel::updateOvertimeMultiplierNormal,
                    onOvertimeMultiplierWeekendChange = viewModel::updateOvertimeMultiplierWeekend,
                    onOvertimeMultiplierHolidayChange = viewModel::updateOvertimeMultiplierHoliday,
                    onBesEnabledChange = viewModel::updateBesEnabled
                )

                SettingsSection.NOTIFICATIONS -> NotificationSettingsSection(
                    uiState = notificationUiState,
                    onShiftPromptToggle = { enabled ->
                        if (!enabled) {
                            notificationViewModel.setShiftPromptEnabled(false)
                            return@NotificationSettingsSection
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
                            pendingNotificationPermissionTarget = NotificationPermissionTarget.SHIFT_PROMPT
                            requestNotificationsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    onTaxAlertToggle = { enabled ->
                        if (!enabled) {
                            notificationViewModel.setTaxBracketAlertsEnabled(false)
                            return@NotificationSettingsSection
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
                            pendingNotificationPermissionTarget = NotificationPermissionTarget.TAX_ALERTS
                            requestNotificationsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                )

                SettingsSection.SYNC -> CalendarSyncSection(
                    uiState = calendarSyncUiState,
                    onEnabledToggle = { enabled ->
                        if (!enabled) {
                            calendarSyncViewModel.setEnabled(false)
                            return@CalendarSyncSection
                        }

                        val hasPermission =
                            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED

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
                    onOpenCalendarPicker = { showCalendarPicker = true },
                    onSyncNow = calendarSyncViewModel::syncNow
                )

                SettingsSection.ABOUT -> DeveloperAboutSection()
            }

            Spacer(modifier = Modifier.height(8.dp))

            val shouldShowSaveButton = currentSection == SettingsSection.HOME ||
                currentSection == SettingsSection.PROFILE ||
                currentSection == SettingsSection.PAYROLL

            if (shouldShowSaveButton) {
                Button(
                    onClick = viewModel::saveSettings,
                    enabled = !uiState.isSaving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    Text(if (uiState.isSaving) "Kaydediliyor" else "Değişiklikleri Kaydet")
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
private fun SettingsHomeSection(
    uiState: SettingsUiState,
    notificationUiState: NotificationSettingsUiState,
    calendarSyncUiState: CalendarSyncSettingsUiState,
    onOpenProfile: () -> Unit,
    onOpenPayroll: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenSync: () -> Unit,
    onOpenAbout: () -> Unit
) {
    Text(
        text = "Ayarları kategori bazında düzenleyebilirsin.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    val companyName = uiState.companyName.ifBlank { "Şirket adı yok" }
    val grossSummary = uiState.grossSalary.toLocalizedDoubleOrNull()?.let(::formatLocalizedCurrency) ?: "Brüt maaş yok"
    val employmentSummary = when (uiState.employmentType) {
        EmploymentType.STANDARD -> "Normal (4A)"
        EmploymentType.RETIRED -> "Emekli (SGDP)"
    }
    val cumulativeSummary = uiState.cumulativeTaxBase.toLocalizedDoubleOrNull()?.let(::formatLocalizedCurrency) ?: "Girili değil"

    val nonWorkingDays = nonWorkingDaysSummary(uiState.nonWorkingDaysMask)
    val payrollSummary = buildString {
        append("Kesim günü: ${uiState.cutoffDay}. gün")
        append(" • Tatil: $nonWorkingDays")
        append(" • Mesai: ${formatMultiplier(uiState.overtimeMultiplierNormal)}/")
        append("${formatMultiplier(uiState.overtimeMultiplierWeekend)}/")
        append("${formatMultiplier(uiState.overtimeMultiplierHoliday)}x")
    }

    val notificationsSummary = when {
        notificationUiState.isShiftPromptEnabled && notificationUiState.isTaxBracketAlertsEnabled ->
            "Vardiya + vergi uyarıları açık"
        notificationUiState.isShiftPromptEnabled ->
            "Sadece vardiya bildirimi açık"
        notificationUiState.isTaxBracketAlertsEnabled ->
            "Sadece vergi bildirimi açık"
        else -> "Bildirimler kapalı"
    }

    val syncSummary = if (calendarSyncUiState.isEnabled) {
        "Açık • ${calendarSyncUiState.selectedCalendarDisplayName ?: "Takvim seçilmedi"}"
    } else {
        "Kapalı"
    }

    SettingsSectionCard(
        title = "Şirket Profili",
        summary = "$companyName • $grossSummary • $employmentSummary • Kümülatif: $cumulativeSummary",
        onClick = onOpenProfile
    )
    SettingsSectionCard(
        title = "Bordro Kuralları",
        summary = payrollSummary,
        onClick = onOpenPayroll
    )
    SettingsSectionCard(
        title = "Bildirimler",
        summary = notificationsSummary,
        onClick = onOpenNotifications
    )
    SettingsSectionCard(
        title = "Senkronizasyon",
        summary = syncSummary,
        onClick = onOpenSync
    )
    SettingsSectionCard(
        title = "Geliştirici Hakkında",
        summary = "xinzore • GitHub ve website bağlantıları",
        onClick = onOpenAbout
    )
}

@Composable
private fun SettingsSectionCard(
    title: String,
    summary: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProfileSettingsSection(
    uiState: SettingsUiState,
    onCompanyNameChange: (String) -> Unit,
    onGrossSalaryChange: (String) -> Unit,
    onEmploymentTypeChange: (EmploymentType) -> Unit,
    onCumulativeTaxBaseChange: (String) -> Unit
) {
    val moneyKeyboardOptions = remember { KeyboardOptions(keyboardType = KeyboardType.Decimal) }

    Text(
        text = "Şirket ve ücret bilgilerini buradan düzenleyebilirsin.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    OutlinedTextField(
        value = uiState.companyName,
        onValueChange = onCompanyNameChange,
        label = { Text("Şirket Adı") },
        modifier = Modifier.fillMaxWidth()
    )

    OutlinedTextField(
        value = uiState.grossSalary,
        onValueChange = onGrossSalaryChange,
        label = { Text("Aylık Brüt (TL)") },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = moneyKeyboardOptions,
        suffix = { Text("₺") }
    )

    Text(
        text = "Çalışma Statüsü",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
    Text(
        text = "Emekli seçilirse SGK/işsizlik yerine SGDP (%7,5) uygulanır.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
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

    OutlinedTextField(
        value = uiState.cumulativeTaxBase,
        onValueChange = onCumulativeTaxBaseChange,
        label = { Text("Kümülatif GV Matrahı (Opsiyonel)") },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = moneyKeyboardOptions,
        supportingText = { Text("Yıl ortasında başladıysan son bordrodaki kümülatif matrahı gir.") },
        suffix = { Text("₺") }
    )
}

@Composable
private fun PayrollRulesSection(
    uiState: SettingsUiState,
    onCutoffDayChange: (Int) -> Unit,
    onToggleNonWorkingDay: (DayOfWeek) -> Unit,
    onOvertimeMultiplierNormalChange: (Float) -> Unit,
    onOvertimeMultiplierWeekendChange: (Float) -> Unit,
    onOvertimeMultiplierHolidayChange: (Float) -> Unit,
    onBesEnabledChange: (Boolean) -> Unit
) {
    Text(
        text = "Bordro dönemini, hafta tatilini ve mesai katsayılarını ayarla.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Text(
        text = "Bordro Kesim Günü",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
    Text(
        text = "Ayın ${uiState.cutoffDay}. günü",
        style = MaterialTheme.typography.bodyLarge
    )
    Slider(
        value = uiState.cutoffDay.toFloat(),
        onValueChange = { onCutoffDayChange(it.toInt()) },
        valueRange = 20f..28f,
        steps = 7
    )

    HorizontalDivider()

    Text(
        text = "Hafta Tatili",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
    Text(
        text = "Tatil günlerini seç (mesaiyi normal/hafta sonu sınıflandırması için).",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

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
                    onClick = { onToggleNonWorkingDay(day) },
                    label = { Text(label) }
                )
            }
        }
    }

    HorizontalDivider()

    Text(
        text = "Mesai Katsayıları",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )

    Text(
        text = "Normal: ${formatMultiplier(uiState.overtimeMultiplierNormal)}x",
        style = MaterialTheme.typography.bodyLarge
    )
    Slider(
        value = uiState.overtimeMultiplierNormal,
        onValueChange = onOvertimeMultiplierNormalChange,
        valueRange = 1f..3f,
        steps = 3
    )

    Text(
        text = "Hafta sonu: ${formatMultiplier(uiState.overtimeMultiplierWeekend)}x",
        style = MaterialTheme.typography.bodyLarge
    )
    Slider(
        value = uiState.overtimeMultiplierWeekend,
        onValueChange = onOvertimeMultiplierWeekendChange,
        valueRange = 1f..3f,
        steps = 3
    )

    Text(
        text = "Resmi tatil: ${formatMultiplier(uiState.overtimeMultiplierHoliday)}x",
        style = MaterialTheme.typography.bodyLarge
    )
    Slider(
        value = uiState.overtimeMultiplierHoliday,
        onValueChange = onOvertimeMultiplierHolidayChange,
        valueRange = 1f..3f,
        steps = 3
    )

    HorizontalDivider()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Otomatik Katılım (BES)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Maaştan %3 kesinti yapılır",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = uiState.isBesEnabled,
            onCheckedChange = onBesEnabledChange
        )
    }
}

@Composable
private fun NotificationSettingsSection(
    uiState: NotificationSettingsUiState,
    onShiftPromptToggle: (Boolean) -> Unit,
    onTaxAlertToggle: (Boolean) -> Unit
) {
    Text(
        text = "Bildirimler anında kaydedilir.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    LabeledSwitchRow(
        title = "Vardiya hatırlatma",
        description = "Her gün 20:00 civarı: “Bugün hangi vardiyadaydın?”",
        checked = uiState.isShiftPromptEnabled,
        onCheckedChange = onShiftPromptToggle
    )

    LabeledSwitchRow(
        title = "Vergi dilimi uyarıları",
        description = "80% / 95% / dilime girdin hatırlatmaları",
        checked = uiState.isTaxBracketAlertsEnabled,
        onCheckedChange = onTaxAlertToggle
    )
}

@Composable
private fun CalendarSyncSection(
    uiState: CalendarSyncSettingsUiState,
    onEnabledToggle: (Boolean) -> Unit,
    onOpenCalendarPicker: () -> Unit,
    onSyncNow: () -> Unit
) {
    Text(
        text = "Takvim ayarları anında kaydedilir.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    LabeledSwitchRow(
        title = "Google Takvim senkronu",
        description = "Vardiya/mesai/izin kayıtlarını seçtiğin takvime yazar.",
        checked = uiState.isEnabled,
        onCheckedChange = onEnabledToggle
    )

    if (uiState.isEnabled) {
        OutlinedButton(
            onClick = onOpenCalendarPicker,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(uiState.selectedCalendarDisplayName ?: "Takvim seç")
        }

        Button(
            onClick = onSyncNow,
            enabled = !uiState.isSyncing && uiState.selectedCalendarId != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState.isSyncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
            }
            Text(if (uiState.isSyncing) "Senkronize ediliyor" else "Şimdi senkronize et")
        }
    }
}

@Composable
private fun DeveloperAboutSection() {
    val uriHandler = LocalUriHandler.current

    Text(
        text = "Uygulama ve geliştirici bağlantıları:",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(text = "Geliştirici", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                text = "https://github.com/xinzore",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { uriHandler.openUri("https://github.com/xinzore") }
            )

            Text(text = "Uygulama GitHub", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                text = "https://github.com/xinzore/BordroTakip",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { uriHandler.openUri("https://github.com/xinzore/BordroTakip") }
            )

            Text(text = "Websitesi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                text = "https://xinzore.vercel.app",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { uriHandler.openUri("https://xinzore.vercel.app") }
            )
        }
    }
}

@Composable
private fun LabeledSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

private fun nonWorkingDaysSummary(mask: Int): String {
    val dayLabels = listOf(
        DayOfWeek.MONDAY to "Pzt",
        DayOfWeek.TUESDAY to "Sal",
        DayOfWeek.WEDNESDAY to "Çar",
        DayOfWeek.THURSDAY to "Per",
        DayOfWeek.FRIDAY to "Cum",
        DayOfWeek.SATURDAY to "Cmt",
        DayOfWeek.SUNDAY to "Paz"
    )
    val selected = dayLabels
        .filter { (day, _) ->
            val bit = 1 shl (day.value - 1)
            (mask and bit) != 0
        }
        .map { it.second }

    return if (selected.isEmpty()) {
        val defaultMask = CompanyProfileEntity.DEFAULT_NON_WORKING_DAYS_MASK
        if (mask == defaultMask) "Paz" else "Yok"
    } else {
        selected.joinToString(", ")
    }
}

private fun formatMultiplier(value: Float): String {
    val intValue = value.toInt()
    return if (value == intValue.toFloat()) {
        intValue.toString()
    } else {
        value.toString().trimEnd('0').trimEnd('.').replace('.', ',')
    }
}
