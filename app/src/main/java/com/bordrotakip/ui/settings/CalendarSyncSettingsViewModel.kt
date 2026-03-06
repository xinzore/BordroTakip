package com.bordrotakip.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bordrotakip.calendar.CalendarProviderRepository
import com.bordrotakip.calendar.CalendarSyncManager
import com.bordrotakip.calendar.CalendarSyncPrefsRepository
import com.bordrotakip.calendar.DeviceCalendar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CalendarSyncSettingsUiState(
    val isEnabled: Boolean = false,
    val selectedCalendarId: Long? = null,
    val selectedCalendarDisplayName: String? = null,
    val availableCalendars: List<DeviceCalendar> = emptyList(),
    val isLoadingCalendars: Boolean = false,
    val isSyncing: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class CalendarSyncSettingsViewModel @Inject constructor(
    private val prefsRepository: CalendarSyncPrefsRepository,
    private val calendarProviderRepository: CalendarProviderRepository,
    private val calendarSyncManager: CalendarSyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarSyncSettingsUiState())
    val uiState: StateFlow<CalendarSyncSettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            prefsRepository.prefsFlow.collect { prefs ->
                _uiState.update {
                    it.copy(
                        isEnabled = prefs.isEnabled,
                        selectedCalendarId = prefs.selectedCalendarId,
                        selectedCalendarDisplayName = prefs.selectedCalendarDisplayName
                    )
                }
            }
        }
    }

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepository.setEnabled(enabled)
        }
    }

    fun refreshCalendars() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingCalendars = true) }
            val calendars = runCatching { calendarProviderRepository.listWritableCalendars() }.getOrDefault(emptyList())
            _uiState.update { it.copy(availableCalendars = calendars, isLoadingCalendars = false) }
        }
    }

    fun selectCalendar(calendar: DeviceCalendar) {
        viewModelScope.launch {
            prefsRepository.setSelectedCalendar(calendar)
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            val calendarId = _uiState.value.selectedCalendarId
            if (calendarId == null) {
                _uiState.update { it.copy(message = "Önce bir takvim seç") }
                return@launch
            }

            _uiState.update { it.copy(isSyncing = true) }
            val result = runCatching { calendarSyncManager.syncAll(calendarId) }.getOrNull()
            _uiState.update {
                it.copy(
                    isSyncing = false,
                    message = if (result == null) {
                        "Senkron başarısız"
                    } else {
                        "Senkron tamamlandı (${result.syncedCount} kayıt)"
                    }
                )
            }
        }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }
}

