package com.bordrotakip.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bordrotakip.notification.NotificationPrefsRepository
import com.bordrotakip.notification.ShiftPromptScheduler
import com.bordrotakip.notification.TaxBracketAlertScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationSettingsUiState(
    val isShiftPromptEnabled: Boolean = false,
    val isTaxBracketAlertsEnabled: Boolean = false
)

@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    private val prefsRepository: NotificationPrefsRepository,
    private val shiftPromptScheduler: ShiftPromptScheduler,
    private val taxBracketAlertScheduler: TaxBracketAlertScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationSettingsUiState())
    val uiState: StateFlow<NotificationSettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            prefsRepository.prefsFlow.collect { prefs ->
                _uiState.update {
                    it.copy(
                        isShiftPromptEnabled = prefs.isShiftPromptEnabled,
                        isTaxBracketAlertsEnabled = prefs.isTaxBracketAlertsEnabled
                    )
                }
            }
        }
    }

    fun setShiftPromptEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepository.setShiftPromptEnabled(enabled)
            if (enabled) {
                shiftPromptScheduler.scheduleDailyShiftPrompt()
            } else {
                shiftPromptScheduler.cancelDailyShiftPrompt()
            }
        }
    }

    fun setTaxBracketAlertsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepository.setTaxBracketAlertsEnabled(enabled)
            if (enabled) {
                taxBracketAlertScheduler.scheduleDailyTaxBracketAlert()
            } else {
                taxBracketAlertScheduler.cancelDailyTaxBracketAlert()
            }
        }
    }
}
