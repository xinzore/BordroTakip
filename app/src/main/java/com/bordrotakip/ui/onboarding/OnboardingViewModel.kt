package com.bordrotakip.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bordrotakip.data.local.dao.CompanyProfileDao
import com.bordrotakip.data.local.entity.CompanyProfileEntity
import com.bordrotakip.data.repository.GrossHistoryRepository
import com.bordrotakip.domain.model.EmploymentType
import com.bordrotakip.domain.model.GrossHistory
import com.bordrotakip.prefs.UserPrefsRepository
import com.bordrotakip.util.sanitizeDecimalInput
import com.bordrotakip.util.toLocalizedDoubleOrNull
import com.bordrotakip.util.toLocalizedInputString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject

data class OnboardingUiState(
    val stepIndex: Int = 0,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isCompleted: Boolean = false,
    val grossSalary: String = "",
    val employmentType: EmploymentType = EmploymentType.STANDARD,
    val cutoffDay: Int = 24,
    val nonWorkingDaysMask: Int = CompanyProfileEntity.DEFAULT_NON_WORKING_DAYS_MASK,
    val defaultShiftHours: Float = 7.5f,
    val isBesEnabled: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val companyProfileDao: CompanyProfileDao,
    private val grossHistoryRepository: GrossHistoryRepository,
    private val userPrefsRepository: UserPrefsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private var loadedProfile: CompanyProfileEntity? = null

    init {
        viewModelScope.launch {
            val profile = companyProfileDao.getProfileSync()
            val gross = grossHistoryRepository.getCurrentGross()
            val userPrefs = userPrefsRepository.prefsFlow.first()

            loadedProfile = profile

            _uiState.update {
                it.copy(
                    isLoading = false,
                    cutoffDay = profile?.cutoffDay ?: 24,
                    employmentType = EmploymentType.fromStorageValue(profile?.employmentType),
                    nonWorkingDaysMask = profile?.nonWorkingDaysMask ?: CompanyProfileEntity.DEFAULT_NON_WORKING_DAYS_MASK,
                    isBesEnabled = profile?.isBesEnabled ?: false,
                    grossSalary = gross?.grossAmount?.toLocalizedInputString() ?: "",
                    defaultShiftHours = userPrefs.defaultShiftHours.toFloat().coerceIn(1.0f, 12.0f)
                )
            }
        }
    }

    fun updateGrossSalary(value: String) {
        val filtered = sanitizeDecimalInput(value)
        _uiState.update { it.copy(grossSalary = filtered, errorMessage = null) }
    }

    fun updateCutoffDay(day: Int) {
        _uiState.update { it.copy(cutoffDay = day, errorMessage = null) }
    }

    fun updateEmploymentType(type: EmploymentType) {
        _uiState.update { it.copy(employmentType = type, errorMessage = null) }
    }

    fun toggleNonWorkingDay(dayOfWeek: DayOfWeek) {
        val bit = 1 shl (dayOfWeek.value - 1)
        _uiState.update { state ->
            state.copy(nonWorkingDaysMask = state.nonWorkingDaysMask xor bit, errorMessage = null)
        }
    }

    fun setNonWorkingDaysMask(mask: Int) {
        _uiState.update { it.copy(nonWorkingDaysMask = mask, errorMessage = null) }
    }

    fun updateDefaultShiftHours(hours: Float) {
        _uiState.update { it.copy(defaultShiftHours = hours.coerceIn(1.0f, 12.0f), errorMessage = null) }
    }

    fun updateBesEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isBesEnabled = enabled, errorMessage = null) }
    }

    fun prevStep() {
        _uiState.update { it.copy(stepIndex = (it.stepIndex - 1).coerceAtLeast(0)) }
    }

    fun nextStep() {
        val state = _uiState.value
        if (state.stepIndex == 0) {
            val gross = state.grossSalary.toLocalizedDoubleOrNull() ?: 0.0
            if (gross <= 0.0) {
                _uiState.update { it.copy(errorMessage = "Brüt maaş girmen gerekiyor.") }
                return
            }
        }
        _uiState.update { it.copy(stepIndex = (it.stepIndex + 1).coerceAtMost(LAST_STEP_INDEX)) }
    }

    fun finish() {
        viewModelScope.launch {
            val gross = _uiState.value.grossSalary.toLocalizedDoubleOrNull() ?: 0.0
            if (gross <= 0.0) {
                _uiState.update { it.copy(errorMessage = "Brüt maaş girmen gerekiyor.") }
                return@launch
            }

            _uiState.update { it.copy(isSaving = true, errorMessage = null) }

            val existing = loadedProfile ?: CompanyProfileEntity(id = 1)
            val updatedProfile = existing.copy(
                cutoffDay = _uiState.value.cutoffDay,
                employmentType = _uiState.value.employmentType.name,
                nonWorkingDaysMask = _uiState.value.nonWorkingDaysMask,
                isBesEnabled = _uiState.value.isBesEnabled,
                updatedAt = System.currentTimeMillis()
            )
            companyProfileDao.insert(updatedProfile)

            grossHistoryRepository.insert(
                GrossHistory(
                    grossAmount = gross,
                    validFrom = LocalDate.now(),
                    note = "Kurulum"
                )
            )

            userPrefsRepository.setDefaultShiftHours(_uiState.value.defaultShiftHours.toDouble())
            userPrefsRepository.setOnboardingCompleted(true)

            _uiState.update { it.copy(isSaving = false, isCompleted = true) }
        }
    }

    companion object {
        const val LAST_STEP_INDEX: Int = 5

        val NON_WORKING_SUNDAY_ONLY: Int = 1 shl (DayOfWeek.SUNDAY.value - 1)
        val NON_WORKING_SAT_SUN: Int =
            (1 shl (DayOfWeek.SATURDAY.value - 1)) or (1 shl (DayOfWeek.SUNDAY.value - 1))
    }
}
