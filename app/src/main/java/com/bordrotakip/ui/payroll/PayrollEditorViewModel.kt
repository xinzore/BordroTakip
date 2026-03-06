package com.bordrotakip.ui.payroll

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import android.util.Log
import com.bordrotakip.data.local.dao.CompanyProfileDao
import com.bordrotakip.data.repository.GrossHistoryRepository
import com.bordrotakip.data.repository.PayrollRecordRepository
import com.bordrotakip.ocr.MlKitTextRecognizer
import com.bordrotakip.ocr.PayrollImageStore
import com.bordrotakip.ocr.PayrollOcrParser
import com.bordrotakip.ocr.PayrollOcrResult
import com.bordrotakip.domain.calculator.PayrollCalculator
import com.bordrotakip.domain.calculator.PayrollPeriodEngine
import com.bordrotakip.domain.calculator.TaxCalculator
import com.bordrotakip.domain.model.EmploymentType
import com.bordrotakip.domain.model.PayrollRecord
import com.bordrotakip.util.sanitizeDecimalInput
import com.bordrotakip.util.toLocalizedDoubleOrNull
import com.bordrotakip.util.toLocalizedInputString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import javax.inject.Inject

data class PayrollEditorUiState(
    val year: Int = LocalDate.now().year,
    val month: Int = LocalDate.now().monthValue,
    val periodLabel: String = "",
    val photoPath: String? = null,
    val isOcrRunning: Boolean = false,
    val ocrErrorMessage: String? = null,
    val carriedTaxBase: String = "",
    val grossSalary: String = "",
    val overtimeAmount: String = "",
    val bonusAmount: String = "",
    val taxBase: String = "",
    val netSalary: String = "",
    val sgkEmployee: String = "",
    val unemployment: String = "",
    val incomeTax: String = "",
    val stampTax: String = "",
    val bes: String = "",
    val note: String = "",
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null,
    val isOcrImported: Boolean = false,
    val employmentType: EmploymentType = EmploymentType.STANDARD
)

@HiltViewModel
class PayrollEditorViewModel @Inject constructor(
    private val companyProfileDao: CompanyProfileDao,
    private val grossHistoryRepository: GrossHistoryRepository,
    private val payrollRecordRepository: PayrollRecordRepository,
    private val payrollPeriodEngine: PayrollPeriodEngine,
    private val payrollCalculator: PayrollCalculator,
    private val taxCalculator: TaxCalculator,
    private val imageStore: PayrollImageStore,
    private val textRecognizer: MlKitTextRecognizer,
    private val ocrParser: PayrollOcrParser
) : ViewModel() {

    private val _uiState = MutableStateFlow(PayrollEditorUiState())
    val uiState: StateFlow<PayrollEditorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val profile = companyProfileDao.getProfileSync()
            payrollPeriodEngine.setCutoffDay(profile?.cutoffDay ?: PayrollPeriodEngine.DEFAULT_CUTOFF_DAY)

            val currentPeriod = payrollPeriodEngine.getCurrentPeriod()
            val gross = grossHistoryRepository.getGrossForDate(currentPeriod.endDate)
                ?: grossHistoryRepository.getLatestGross()

            _uiState.update {
                it.copy(
                    year = currentPeriod.year,
                    month = currentPeriod.month,
                    periodLabel = currentPeriod.detailedLabel,
                    grossSalary = gross?.grossAmount?.toLocalizedInputString() ?: "",
                    employmentType = EmploymentType.fromStorageValue(profile?.employmentType)
                )
            }
        }
    }

    fun importFromImageUri(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isOcrRunning = true, ocrErrorMessage = null) }

            try {
                val text = textRecognizer.recognizeText(uri)
                var parsed = ocrParser.parse(text)

                if (parsed.taxBase == null || parsed.carriedTaxBase == null) {
                    val taxRegionText = runCatching { textRecognizer.recognizeTaxRegionText(uri) }
                        .onFailure { Log.w(TAG, "Tax bölge OCR başarısız: $uri", it) }
                        .getOrNull()

                    if (!taxRegionText.isNullOrBlank()) {
                        val parsedFromTaxRegion = ocrParser.parse(taxRegionText)
                        val merged = mergeOcrResults(parsed, parsedFromTaxRegion)
                        if (parsed.taxBase == null && merged.taxBase != null) {
                            Log.d(TAG, "Tax base ROI OCR ile kurtarıldı: ${merged.taxBase}")
                        }
                        if (parsed.carriedTaxBase == null && merged.carriedTaxBase != null) {
                            Log.d(TAG, "Devreden matrah ROI OCR ile kurtarıldı: ${merged.carriedTaxBase}")
                        }
                        parsed = merged
                    }
                }

                parsed = backfillCalculatedTaxBaseIfMissing(parsed)
                val photoPath = runCatching { imageStore.copyToInternalStorage(uri).absolutePath }
                    .onFailure { Log.w(TAG, "OCR başarılı ama fotoğraf kopyalama başarısız: $uri", it) }
                    .getOrNull()

                val parsedGross = parsed.grossSalary
                val parsedTotal = parsed.totalEarnings
                val parsedOvertime = parsed.overtimeTotal

                val computedOvertime = parsedOvertime
                    ?: run {
                        if (parsedGross != null && parsedTotal != null) {
                            (parsedTotal - parsedGross).coerceAtLeast(0.0)
                        } else null
                    }

                val computedBonus = run {
                    if (parsedGross != null && parsedTotal != null) {
                        val overtime = parsedOvertime ?: 0.0
                        (parsedTotal - parsedGross - overtime).takeIf { it > 0.0 }
                    } else null
                }

                _uiState.update { current ->
                    current.copy(
                        photoPath = photoPath,
                        isOcrRunning = false,
                        isOcrImported = true,
                        carriedTaxBase = parsed.carriedTaxBase?.toLocalizedInputString() ?: current.carriedTaxBase,
                        // OCR ile bulduklarımız (bulunmayanlar eski haliyle kalır)
                        grossSalary = parsedGross?.toLocalizedInputString() ?: current.grossSalary,
                        overtimeAmount = computedOvertime?.toLocalizedInputString() ?: current.overtimeAmount,
                        bonusAmount = computedBonus?.toLocalizedInputString() ?: current.bonusAmount,
                        netSalary = parsed.netSalary?.toLocalizedInputString() ?: current.netSalary,
                        taxBase = parsed.taxBase?.toLocalizedInputString() ?: current.taxBase,
                        sgkEmployee = parsed.sgkEmployee?.toLocalizedInputString() ?: current.sgkEmployee,
                        unemployment = parsed.unemployment?.toLocalizedInputString() ?: current.unemployment,
                        incomeTax = parsed.incomeTax?.toLocalizedInputString() ?: current.incomeTax,
                        stampTax = parsed.stampTax?.toLocalizedInputString() ?: current.stampTax,
                        bes = parsed.bes?.toLocalizedInputString() ?: current.bes
                    )
                }

                // Dönem etiketi (yıl/ay) OCR'den gelirse güncelle
                if (parsed.year != null && parsed.month != null) {
                    updateYear(parsed.year)
                    updateMonth(parsed.month)
                }
            } catch (e: Exception) {
                Log.e(TAG, "OCR import başarısız: $uri", e)
                val message = e.message
                    ?.takeIf { it.isNotBlank() }
                    ?.let { "OCR başarısız: $it" }
                    ?: "OCR başarısız. Farklı bir fotoğrafla tekrar dene."
                _uiState.update { it.copy(isOcrRunning = false, ocrErrorMessage = message) }
            }
        }
    }

    fun clearImportedPhoto() {
        viewModelScope.launch {
            val path = _uiState.value.photoPath
            if (!path.isNullOrBlank()) {
                runCatching { File(path).delete() }
            }
            _uiState.update { it.copy(photoPath = null, isOcrImported = false, ocrErrorMessage = null) }
        }
    }

    fun updateYear(year: Int) {
        if (year !in 2000..2100) return
        _uiState.update { it.copy(year = year) }
        refreshPeriodLabel()
    }

    fun updateMonth(month: Int) {
        if (month !in 1..12) return
        _uiState.update { it.copy(month = month) }
        refreshPeriodLabel()
    }

    private fun refreshPeriodLabel() {
        val year = _uiState.value.year
        val month = _uiState.value.month
        val period = payrollPeriodEngine.getPeriodForMonth(year, month)
        _uiState.update { it.copy(periodLabel = period.detailedLabel) }
    }

    fun updateGrossSalary(value: String) = updateMoneyField(value) { v -> copy(grossSalary = v) }
    fun updateOvertimeAmount(value: String) = updateMoneyField(value) { v -> copy(overtimeAmount = v) }
    fun updateBonusAmount(value: String) = updateMoneyField(value) { v -> copy(bonusAmount = v) }
    fun updateTaxBase(value: String) = updateMoneyField(value) { v -> copy(taxBase = v) }
    fun updateNetSalary(value: String) = updateMoneyField(value) { v -> copy(netSalary = v) }
    fun updateSgkEmployee(value: String) = updateMoneyField(value) { v -> copy(sgkEmployee = v) }
    fun updateUnemployment(value: String) = updateMoneyField(value) { v -> copy(unemployment = v) }
    fun updateIncomeTax(value: String) = updateMoneyField(value) { v -> copy(incomeTax = v) }
    fun updateStampTax(value: String) = updateMoneyField(value) { v -> copy(stampTax = v) }
    fun updateBes(value: String) = updateMoneyField(value) { v -> copy(bes = v) }

    fun updateNote(note: String) {
        _uiState.update { it.copy(note = note) }
    }

    private inline fun updateMoneyField(raw: String, crossinline reducer: PayrollEditorUiState.(String) -> PayrollEditorUiState) {
        val filtered = sanitizeDecimalInput(raw)
        _uiState.update { it.reducer(filtered) }
    }

    fun save() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, saveSuccess = false) }

        val year = _uiState.value.year
        val month = _uiState.value.month
        val period = payrollPeriodEngine.getPeriodForMonth(year, month)

        val grossSalary = _uiState.value.grossSalary.toLocalizedDoubleOrNull() ?: 0.0
        val overtimeAmount = _uiState.value.overtimeAmount.toLocalizedDoubleOrNull() ?: 0.0
        val bonusAmount = _uiState.value.bonusAmount.toLocalizedDoubleOrNull() ?: 0.0
        val taxBaseInput = _uiState.value.taxBase.toLocalizedDoubleOrNull() ?: 0.0
        val netSalaryInput = _uiState.value.netSalary.toLocalizedDoubleOrNull() ?: 0.0

            if (grossSalary <= 0.0) {
                _uiState.update { it.copy(isSaving = false, errorMessage = "Brüt maaş boş olamaz.") }
                return@launch
            }
            if (taxBaseInput <= 0.0) {
                _uiState.update { it.copy(isSaving = false, errorMessage = "GV matrahı boş olamaz.") }
                return@launch
            }
            if (netSalaryInput <= 0.0) {
                _uiState.update { it.copy(isSaving = false, errorMessage = "Net maaş boş olamaz.") }
                return@launch
            }

        val totalEarnings = grossSalary + overtimeAmount + bonusAmount

        val profile = companyProfileDao.getProfileSync()
        val carriedTaxBase = _uiState.value.carriedTaxBase.toLocalizedDoubleOrNull()
        val baselineCumulative = profile?.cumulativeTaxBase ?: 0.0
        val recordedCumulative = payrollRecordRepository.getCumulativeTaxBaseBeforePeriod(period)
        val cumulativeBefore = carriedTaxBase ?: (baselineCumulative + recordedCumulative)
        val cumulativeAfter = cumulativeBefore + taxBaseInput
        val employmentType = EmploymentType.fromStorageValue(profile?.employmentType)

        val (sgkCalculated, unemploymentCalculated) = payrollCalculator.calculateSgkDeductions(
            period.year,
            totalEarnings,
            employmentType
        )
        val sgkEmployee = _uiState.value.sgkEmployee.toLocalizedDoubleOrNull() ?: sgkCalculated
        val unemployment = _uiState.value.unemployment.toLocalizedDoubleOrNull() ?: unemploymentCalculated

            val incomeTaxCalculated = taxCalculator.calculateIncomeTax(period.year, cumulativeBefore, taxBaseInput, isExempt = true).taxAmount
            val incomeTax = _uiState.value.incomeTax.toLocalizedDoubleOrNull() ?: incomeTaxCalculated

            val stampTaxCalculated = run {
                val raw = totalEarnings * PayrollCalculator.STAMP_TAX_RATE
                val exempt = taxCalculator.calculateStampTaxExemption(period.year)
                (raw - exempt).coerceAtLeast(0.0)
            }
            val stampTax = _uiState.value.stampTax.toLocalizedDoubleOrNull() ?: stampTaxCalculated

            val besCalculated = if (profile?.isBesEnabled == true) totalEarnings * (profile.besRate) else 0.0
            val bes = _uiState.value.bes.toLocalizedDoubleOrNull() ?: besCalculated

            val totalDeductions = sgkEmployee + unemployment + incomeTax + stampTax + bes

            val record = PayrollRecord(
                period = period,
                grossSalary = grossSalary,
                overtimeAmount = overtimeAmount,
                bonusAmount = bonusAmount,
                totalEarnings = totalEarnings,
                sgkEmployee = sgkEmployee,
                unemployment = unemployment,
                incomeTax = incomeTax,
                stampTax = stampTax,
                bes = bes,
                totalDeductions = totalDeductions,
                netSalary = netSalaryInput,
                sgkBase = minOf(totalEarnings, taxCalculator.getMinWageGross(period.year) * 7.5),
                taxBase = taxBaseInput,
                cumulativeTaxBase = cumulativeAfter,
                note = _uiState.value.note,
                photoPath = _uiState.value.photoPath,
                isManualEntry = !_uiState.value.isOcrImported
            )

            payrollRecordRepository.insert(record)
            _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
        }
    }

    companion object {
        private const val TAG = "PayrollEditorVM"
    }

    private fun mergeOcrResults(primary: PayrollOcrResult, secondary: PayrollOcrResult): PayrollOcrResult {
        return primary.copy(
            grossSalary = primary.grossSalary ?: secondary.grossSalary,
            totalEarnings = primary.totalEarnings ?: secondary.totalEarnings,
            overtimeTotal = primary.overtimeTotal ?: secondary.overtimeTotal,
            netSalary = primary.netSalary ?: secondary.netSalary,
            carriedTaxBase = primary.carriedTaxBase ?: secondary.carriedTaxBase,
            taxBase = primary.taxBase ?: secondary.taxBase,
            sgkEmployee = primary.sgkEmployee ?: secondary.sgkEmployee,
            unemployment = primary.unemployment ?: secondary.unemployment,
            incomeTax = primary.incomeTax ?: secondary.incomeTax,
            stampTax = primary.stampTax ?: secondary.stampTax,
            bes = primary.bes ?: secondary.bes,
            year = primary.year ?: secondary.year,
            month = primary.month ?: secondary.month
        )
    }

    private fun backfillCalculatedTaxBaseIfMissing(parsed: PayrollOcrResult): PayrollOcrResult {
        if (parsed.taxBase != null) return parsed

        val totalEarnings = parsed.totalEarnings
            ?: parsed.grossSalary?.let { gross ->
                gross + (parsed.overtimeTotal ?: 0.0)
            }
            ?: return parsed

        val (sgkCalculated, unemploymentCalculated) = payrollCalculator.calculateSgkDeductions(
            taxYear = _uiState.value.year,
            grossAmount = totalEarnings,
            employmentType = _uiState.value.employmentType
        )

        val sgkValue = parsed.sgkEmployee ?: sgkCalculated
        val unemploymentValue = when (_uiState.value.employmentType) {
            EmploymentType.RETIRED -> 0.0
            EmploymentType.STANDARD -> parsed.unemployment ?: unemploymentCalculated
        }
        val calculatedTaxBase = (totalEarnings - sgkValue - unemploymentValue).coerceAtLeast(0.0)

        Log.d(TAG, "Tax base hesapla fallback kullanıldı: $calculatedTaxBase")
        return parsed.copy(
            taxBase = calculatedTaxBase,
            sgkEmployee = parsed.sgkEmployee ?: sgkValue,
            unemployment = when (_uiState.value.employmentType) {
                EmploymentType.RETIRED -> parsed.unemployment
                EmploymentType.STANDARD -> parsed.unemployment ?: unemploymentValue
            }
        )
    }
}
