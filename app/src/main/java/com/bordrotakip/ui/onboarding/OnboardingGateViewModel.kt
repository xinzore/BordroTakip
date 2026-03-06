package com.bordrotakip.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bordrotakip.data.local.dao.CompanyProfileDao
import com.bordrotakip.data.repository.GrossHistoryRepository
import com.bordrotakip.prefs.UserPrefsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingGateUiState(
    val isReady: Boolean = false,
    val shouldShowOnboarding: Boolean = false
)

@HiltViewModel
class OnboardingGateViewModel @Inject constructor(
    private val userPrefsRepository: UserPrefsRepository,
    private val companyProfileDao: CompanyProfileDao,
    private val grossHistoryRepository: GrossHistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingGateUiState())
    val uiState: StateFlow<OnboardingGateUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            var didBackfillForExistingUser = false
            userPrefsRepository.prefsFlow.collect { prefs ->
                if (!prefs.isOnboardingCompleted && !didBackfillForExistingUser) {
                    didBackfillForExistingUser = true
                    val hasExistingData =
                        companyProfileDao.getProfileSync() != null || grossHistoryRepository.getLatestGross() != null
                    if (hasExistingData) {
                        userPrefsRepository.setOnboardingCompleted(true)
                        return@collect
                    }
                }

                _uiState.update {
                    it.copy(
                        isReady = true,
                        shouldShowOnboarding = !prefs.isOnboardingCompleted
                    )
                }
            }
        }
    }
}

