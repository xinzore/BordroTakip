package com.bordrotakip.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bordrotakip.data.local.dao.CompanyProfileDao
import com.bordrotakip.data.local.entity.CompanyProfileEntity
import com.bordrotakip.data.repository.GrossHistoryRepository
import com.bordrotakip.domain.calculator.PayrollCalculator
import com.bordrotakip.domain.model.EmploymentType
import com.bordrotakip.domain.model.GrossHistory
import com.bordrotakip.util.sanitizeDecimalInput
import com.bordrotakip.util.toLocalizedDoubleOrNull
import com.bordrotakip.util.toLocalizedInputString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject
import kotlin.math.round

data class SettingsUiState(
    val companyName: String = "",
    val grossSalary: String = "",
    val employmentType: EmploymentType = EmploymentType.STANDARD,
    val cumulativeTaxBase: String = "", // Kümülatif Vergi Matrahı
    val cutoffDay: Int = 24,
    val nonWorkingDaysMask: Int = CompanyProfileEntity.DEFAULT_NON_WORKING_DAYS_MASK, // Haftalık tatil günleri
    val isBesEnabled: Boolean = true,
    val overtimeMultiplierNormal: Float = PayrollCalculator.OVERTIME_MULTIPLIER_NORMAL.toFloat(),
    val overtimeMultiplierWeekend: Float = PayrollCalculator.OVERTIME_MULTIPLIER_WEEKEND.toFloat(),
    val overtimeMultiplierHoliday: Float = PayrollCalculator.OVERTIME_MULTIPLIER_HOLIDAY.toFloat(),
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val companyProfileDao: CompanyProfileDao,
    private val grossHistoryRepository: GrossHistoryRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    init {
        loadSettings()
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            val profile = companyProfileDao.getProfileSync()
            val gross = grossHistoryRepository.getCurrentGross()
            
            _uiState.update {
                it.copy(
                    companyName = profile?.companyName ?: "",
                    cutoffDay = profile?.cutoffDay ?: 24,
                    employmentType = EmploymentType.fromStorageValue(profile?.employmentType),
                    nonWorkingDaysMask = profile?.nonWorkingDaysMask ?: CompanyProfileEntity.DEFAULT_NON_WORKING_DAYS_MASK,
                    isBesEnabled = profile?.isBesEnabled ?: true,
                    overtimeMultiplierNormal = (profile?.overtimeMultiplierNormal ?: PayrollCalculator.OVERTIME_MULTIPLIER_NORMAL).toFloat(),
                    overtimeMultiplierWeekend = (profile?.overtimeMultiplierWeekend ?: PayrollCalculator.OVERTIME_MULTIPLIER_WEEKEND).toFloat(),
                    overtimeMultiplierHoliday = (profile?.overtimeMultiplierHoliday ?: PayrollCalculator.OVERTIME_MULTIPLIER_HOLIDAY).toFloat(),
                    cumulativeTaxBase = if ((profile?.cumulativeTaxBase ?: 0.0) > 0) 
                        profile?.cumulativeTaxBase?.toLocalizedInputString() ?: ""
                        else "",
                    grossSalary = gross?.grossAmount?.toLocalizedInputString() ?: ""
                )
            }
        }
    }
    
    fun updateCompanyName(name: String) {
        _uiState.update { it.copy(companyName = name) }
    }
    
    fun updateGrossSalary(salary: String) {
        val filtered = sanitizeDecimalInput(salary)
        _uiState.update { it.copy(grossSalary = filtered) }
    }

    fun updateEmploymentType(type: EmploymentType) {
        _uiState.update { it.copy(employmentType = type) }
    }
    
    fun updateCumulativeTaxBase(value: String) {
        val filtered = sanitizeDecimalInput(value)
        _uiState.update { it.copy(cumulativeTaxBase = filtered) }
    }
    
    fun updateCutoffDay(day: Int) {
        _uiState.update { it.copy(cutoffDay = day) }
    }

    fun toggleNonWorkingDay(dayOfWeek: DayOfWeek) {
        val bit = 1 shl (dayOfWeek.value - 1)
        _uiState.update { state ->
            state.copy(nonWorkingDaysMask = state.nonWorkingDaysMask xor bit)
        }
    }
    
    fun updateBesEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isBesEnabled = enabled) }
    }

    fun updateOvertimeMultiplierNormal(value: Float) {
        _uiState.update { it.copy(overtimeMultiplierNormal = snapMultiplier(value)) }
    }

    fun updateOvertimeMultiplierWeekend(value: Float) {
        _uiState.update { it.copy(overtimeMultiplierWeekend = snapMultiplier(value)) }
    }

    fun updateOvertimeMultiplierHoliday(value: Float) {
        _uiState.update { it.copy(overtimeMultiplierHoliday = snapMultiplier(value)) }
    }

    private fun snapMultiplier(value: Float): Float {
        val step = 0.5f
        val snapped = round(value / step) * step
        return snapped.coerceIn(1.0f, 3.0f)
    }
    
    fun saveSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveSuccess = false) }
            
            val cumulativeBase = _uiState.value.cumulativeTaxBase.toLocalizedDoubleOrNull() ?: 0.0
            
            val profile = CompanyProfileEntity(
                id = 1,
                companyName = _uiState.value.companyName,
                cutoffDay = _uiState.value.cutoffDay,
                isBesEnabled = _uiState.value.isBesEnabled,
                overtimeMultiplierNormal = _uiState.value.overtimeMultiplierNormal.toDouble(),
                overtimeMultiplierWeekend = _uiState.value.overtimeMultiplierWeekend.toDouble(),
                overtimeMultiplierHoliday = _uiState.value.overtimeMultiplierHoliday.toDouble(),
                employmentType = _uiState.value.employmentType.name,
                nonWorkingDaysMask = _uiState.value.nonWorkingDaysMask,
                cumulativeTaxBase = cumulativeBase
            )
            companyProfileDao.insert(profile)
            
            val grossAmount = _uiState.value.grossSalary.toLocalizedDoubleOrNull()
            if (grossAmount != null && grossAmount > 0) {
                val currentGross = grossHistoryRepository.getCurrentGross()
                
                if (currentGross == null || currentGross.grossAmount != grossAmount) {
                    grossHistoryRepository.insert(
                        GrossHistory(
                            grossAmount = grossAmount,
                            validFrom = LocalDate.now(),
                            note = "Ayarlardan güncellendi"
                        )
                    )
                }
            }
            
            _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
        }
    }

    fun consumeSaveSuccess() {
        _uiState.update { it.copy(saveSuccess = false) }
    }
}
