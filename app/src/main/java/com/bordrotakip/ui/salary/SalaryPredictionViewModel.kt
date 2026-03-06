package com.bordrotakip.ui.salary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bordrotakip.data.local.dao.CompanyProfileDao
import com.bordrotakip.data.repository.GrossHistoryRepository
import com.bordrotakip.data.repository.PayrollRecordRepository
import com.bordrotakip.data.repository.WorkEventRepository
import com.bordrotakip.domain.calendar.TrHolidayCalendar
import com.bordrotakip.domain.calculator.PayrollCalculator
import com.bordrotakip.domain.calculator.PayrollPeriodEngine
import com.bordrotakip.domain.calculator.TaxCalculator
import com.bordrotakip.domain.model.EmploymentType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class SalaryPredictionUiState(
    val estimatedNet: Double = 0.0,
    val totalEarnings: Double = 0.0,
    val employmentType: EmploymentType = EmploymentType.STANDARD,
    val sgkDeduction: Double = 0.0,
    val unemploymentDeduction: Double = 0.0,
    val incomeTax: Double = 0.0,
    val stampTax: Double = 0.0,
    val besDeduction: Double = 0.0,
    val currentTaxBracket: Int = 15,
    val cumulativeTaxBaseBefore: Double = 0.0,
    val cumulativeTaxBaseAfter: Double = 0.0,
    val bracketProgress: Double = 0.0,
    val monthsToNextBracket: Int = 0,
    val confidenceRate: Double = 0.95,
    val isLoading: Boolean = false,
    val currentMonth: Int = 1
)

@HiltViewModel
class SalaryPredictionViewModel @Inject constructor(
    private val workEventRepository: WorkEventRepository,
    private val grossHistoryRepository: GrossHistoryRepository,
    private val payrollRecordRepository: PayrollRecordRepository,
    private val companyProfileDao: CompanyProfileDao,
    private val payrollPeriodEngine: PayrollPeriodEngine,
    private val payrollCalculator: PayrollCalculator,
    private val taxCalculator: TaxCalculator
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SalaryPredictionUiState())
    val uiState: StateFlow<SalaryPredictionUiState> = _uiState.asStateFlow()
    
    init {
        loadPrediction()
    }
    
    private fun loadPrediction() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val profile = companyProfileDao.getProfileSync()
            payrollPeriodEngine.setCutoffDay(profile?.cutoffDay ?: PayrollPeriodEngine.DEFAULT_CUTOFF_DAY)

            val currentPeriod = payrollPeriodEngine.getCurrentPeriod()
            val events = workEventRepository.getEventsForPeriodSync(
                currentPeriod.startDate,
                currentPeriod.endDate
            )
            
            val grossHistory = grossHistoryRepository.getGrossForDate(currentPeriod.endDate)
                ?: grossHistoryRepository.getLatestGross()
            
            if (grossHistory != null) {
                val isBesEnabled = profile?.isBesEnabled ?: false

                val baselineCumulative = profile?.cumulativeTaxBase ?: 0.0
                val recordedCumulative = payrollRecordRepository.getCumulativeTaxBaseBeforePeriod(currentPeriod)
                val cumulativeBefore = baselineCumulative + recordedCumulative
                val overtimeMultiplierNormal = profile?.overtimeMultiplierNormal ?: PayrollCalculator.OVERTIME_MULTIPLIER_NORMAL
                val overtimeMultiplierWeekend = profile?.overtimeMultiplierWeekend ?: PayrollCalculator.OVERTIME_MULTIPLIER_WEEKEND
                val overtimeMultiplierHoliday = profile?.overtimeMultiplierHoliday ?: PayrollCalculator.OVERTIME_MULTIPLIER_HOLIDAY
                val nonWorkingDaysMask = profile?.nonWorkingDaysMask ?: TrHolidayCalendar.DEFAULT_NON_WORKING_DAYS_MASK
                val employmentType = EmploymentType.fromStorageValue(profile?.employmentType)
                
                val estimate = payrollCalculator.estimateNetSalary(
                    taxYear = currentPeriod.year,
                    payrollMonth = currentPeriod.month,
                    monthlyGross = grossHistory.grossAmount,
                    events = events,
                    nonWorkingDaysMask = nonWorkingDaysMask,
                    cumulativeTaxBaseBefore = cumulativeBefore,
                    overtimeMultiplierNormal = overtimeMultiplierNormal,
                    overtimeMultiplierWeekend = overtimeMultiplierWeekend,
                    overtimeMultiplierHoliday = overtimeMultiplierHoliday,
                    employmentType = employmentType,
                    isBesEnabled = isBesEnabled
                )
                
                // Bir sonraki dilim tahmini
                val monthsToNext = taxCalculator.predictNextBracketEntry(
                    year = currentPeriod.year,
                    monthlyBase = estimate.taxBase,
                    currentCumulative = estimate.cumulativeTaxBaseAfter
                ) ?: 12
                
                val bracket = estimate.taxBracket
                val progress = if (bracket.upperBound != Double.MAX_VALUE && bracket.upperBound > bracket.lowerBound) {
                    (estimate.cumulativeTaxBaseAfter - bracket.lowerBound) / (bracket.upperBound - bracket.lowerBound)
                } else 0.0
                
                _uiState.update {
                    it.copy(
                        estimatedNet = estimate.netSalary,
                        totalEarnings = estimate.totalEarnings,
                        employmentType = employmentType,
                        sgkDeduction = estimate.sgkDeduction,
                        unemploymentDeduction = estimate.unemploymentDeduction,
                        incomeTax = estimate.incomeTax,
                        stampTax = estimate.stampTax,
                        besDeduction = estimate.besDeduction,
                        currentTaxBracket = estimate.taxBracket.percentage,
                        cumulativeTaxBaseBefore = estimate.cumulativeTaxBaseBefore,
                        cumulativeTaxBaseAfter = estimate.cumulativeTaxBaseAfter,
                        bracketProgress = progress.coerceIn(0.0, 1.0),
                        monthsToNextBracket = monthsToNext,
                        isLoading = false,
                        currentMonth = LocalDate.now().monthValue
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
    
    fun refresh() {
        loadPrediction()
    }
}
