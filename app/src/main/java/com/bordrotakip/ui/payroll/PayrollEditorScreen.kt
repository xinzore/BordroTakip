package com.bordrotakip.ui.payroll

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.bordrotakip.domain.model.EmploymentType
import com.bordrotakip.util.formatLocalizedCurrency
import com.bordrotakip.util.toLocalizedDoubleOrNull
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayrollEditorScreen(
    onNavigateBack: () -> Unit,
    viewModel: PayrollEditorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            viewModel.importFromImageUri(it)
        }
    }

    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            viewModel.importFromImageUri(uri)
        }
    }

    fun launchCamera() {
        val imagesDir = File(context.cacheDir, "images").apply { mkdirs() }
        val imageFile = File(imagesDir, "payroll_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
        pendingCameraUri = uri
        takePictureLauncher.launch(uri)
    }

    val requestCameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchCamera()
        }
    }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) onNavigateBack()
    }

    var showAdvanced by remember { mutableStateOf(false) }
    var monthMenuExpanded by remember { mutableStateOf(false) }

    val monthNames = listOf(
        "Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran",
        "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık"
    )
    val moneyKeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bordro Ekle") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
            if (uiState.errorMessage != null) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(
                        text = uiState.errorMessage ?: "",
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Text(
                text = "Dönem",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.updateYear(uiState.year - 1) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Önceki yıl")
                    }
                    Text(text = uiState.year.toString(), style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { viewModel.updateYear(uiState.year + 1) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Sonraki yıl")
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = monthMenuExpanded,
                    onExpandedChange = { monthMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        readOnly = true,
                        value = monthNames.getOrNull(uiState.month - 1) ?: uiState.month.toString(),
                        onValueChange = {},
                        label = { Text("Ay") },
                        trailingIcon = {
                            Icon(Icons.Default.ExpandMore, contentDescription = null)
                        },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                    )
                    ExposedDropdownMenu(
                        expanded = monthMenuExpanded,
                        onDismissRequest = { monthMenuExpanded = false }
                    ) {
                        monthNames.forEachIndexed { index, name ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    viewModel.updateMonth(index + 1)
                                    monthMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            if (uiState.periodLabel.isNotBlank()) {
                Text(
                    text = uiState.periodLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider()

            Text(
                text = "Bordro Fotoğrafı (OCR)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Fotoğraf ekleyince uygulama alanları otomatik doldurur. Sonra kontrol edip düzelt.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { pickImageLauncher.launch(arrayOf("image/*")) },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isOcrRunning
                ) {
                    Text("Galeriden Seç")
                }
                OutlinedButton(
                    onClick = {
                        val hasPermission = context.checkSelfPermission(Manifest.permission.CAMERA) ==
                            android.content.pm.PackageManager.PERMISSION_GRANTED
                        if (hasPermission) {
                            launchCamera()
                        } else {
                            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isOcrRunning
                ) {
                    Text("Foto Çek")
                }
            }

            if (uiState.photoPath != null) {
                val previewBitmap = remember(uiState.photoPath) {
                    uiState.photoPath?.let { decodePreviewBitmap(it, maxSizePx = 900) }
                }

                previewBitmap?.let { bitmap ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Bordro Fotoğrafı",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 140.dp, max = 240.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (uiState.isOcrRunning) "OCR okunuyor…" else "Fotoğraf eklendi",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                TextButton(
                                    onClick = viewModel::clearImportedPhoto,
                                    enabled = !uiState.isOcrRunning
                                ) {
                                    Text("Kaldır")
                                }
                            }
                            if (uiState.isOcrRunning) {
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            }

            if (uiState.ocrErrorMessage != null) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(
                        text = uiState.ocrErrorMessage ?: "",
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            if (uiState.carriedTaxBase.isNotBlank()) {
                val carriedTaxBaseLabel = uiState.carriedTaxBase.toLocalizedDoubleOrNull()
                    ?.let(::formatLocalizedCurrency)
                    ?: uiState.carriedTaxBase
                Text(
                    text = "OCR: Dev. Vergi Matrahı $carriedTaxBaseLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider()

            OutlinedTextField(
                value = uiState.grossSalary,
                onValueChange = viewModel::updateGrossSalary,
                label = { Text("Brüt Maaş (TL)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = moneyKeyboardOptions,
                suffix = { Text("₺") }
            )

            OutlinedTextField(
                value = uiState.overtimeAmount,
                onValueChange = viewModel::updateOvertimeAmount,
                label = { Text("Mesai (TL) - opsiyonel") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = moneyKeyboardOptions,
                suffix = { Text("₺") }
            )

            OutlinedTextField(
                value = uiState.bonusAmount,
                onValueChange = viewModel::updateBonusAmount,
                label = { Text("Prim/İkramiye (TL) - opsiyonel") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = moneyKeyboardOptions,
                suffix = { Text("₺") }
            )

            OutlinedTextField(
                value = uiState.taxBase,
                onValueChange = viewModel::updateTaxBase,
                label = { Text("GV Matrahı (TL)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = moneyKeyboardOptions,
                supportingText = { Text("Bordrodaki “Gelir Vergisi Matrahı” alanı.") },
                suffix = { Text("₺") }
            )

            OutlinedTextField(
                value = uiState.netSalary,
                onValueChange = viewModel::updateNetSalary,
                label = { Text("Net Maaş (TL)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = moneyKeyboardOptions,
                suffix = { Text("₺") }
            )

            TextButton(onClick = { showAdvanced = !showAdvanced }) {
                Text(if (showAdvanced) "Detayları gizle" else "Detayları göster (opsiyonel)")
            }

            if (showAdvanced) {
                val socialSecurityLabel = when (uiState.employmentType) {
                    EmploymentType.RETIRED -> "SGDP İşçi (TL)"
                    EmploymentType.STANDARD -> "SGK İşçi (TL)"
                }

                OutlinedTextField(
                    value = uiState.sgkEmployee,
                    onValueChange = viewModel::updateSgkEmployee,
                    label = { Text(socialSecurityLabel) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = moneyKeyboardOptions,
                    suffix = { Text("₺") }
                )
                if (uiState.employmentType != EmploymentType.RETIRED) {
                    OutlinedTextField(
                        value = uiState.unemployment,
                        onValueChange = viewModel::updateUnemployment,
                        label = { Text("İşsizlik (TL)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = moneyKeyboardOptions,
                        suffix = { Text("₺") }
                    )
                }
                OutlinedTextField(
                    value = uiState.incomeTax,
                    onValueChange = viewModel::updateIncomeTax,
                    label = { Text("Gelir Vergisi (TL)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = moneyKeyboardOptions,
                    suffix = { Text("₺") }
                )
                OutlinedTextField(
                    value = uiState.stampTax,
                    onValueChange = viewModel::updateStampTax,
                    label = { Text("Damga Vergisi (TL)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = moneyKeyboardOptions,
                    suffix = { Text("₺") }
                )
                OutlinedTextField(
                    value = uiState.bes,
                    onValueChange = viewModel::updateBes,
                    label = { Text("BES (TL)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = moneyKeyboardOptions,
                    suffix = { Text("₺") }
                )
            }

            OutlinedTextField(
                value = uiState.note,
                onValueChange = viewModel::updateNote,
                label = { Text("Not - opsiyonel") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            Button(
                onClick = viewModel::save,
                enabled = !uiState.isSaving && !uiState.isOcrRunning,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Kaydet")
            }
        }
    }
}

private fun decodePreviewBitmap(path: String, maxSizePx: Int): Bitmap? {
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, options)

    val (w, h) = options.outWidth to options.outHeight
    if (w <= 0 || h <= 0) return null

    var sampleSize = 1
    while ((w / sampleSize) > maxSizePx || (h / sampleSize) > maxSizePx) {
        sampleSize *= 2
    }

    val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    return BitmapFactory.decodeFile(path, decodeOptions)
}
