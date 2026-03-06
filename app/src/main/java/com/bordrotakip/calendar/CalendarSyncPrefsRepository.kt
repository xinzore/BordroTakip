package com.bordrotakip.calendar

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class CalendarSyncPrefs(
    val isEnabled: Boolean,
    val selectedCalendarId: Long?,
    val selectedCalendarDisplayName: String?
)

@Singleton
class CalendarSyncPrefsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.calendarSyncDataStore

    val prefsFlow: Flow<CalendarSyncPrefs> = dataStore.data.map { prefs ->
        CalendarSyncPrefs(
            isEnabled = prefs[KEY_ENABLED] ?: false,
            selectedCalendarId = prefs[KEY_SELECTED_CALENDAR_ID],
            selectedCalendarDisplayName = prefs[KEY_SELECTED_CALENDAR_NAME]
        )
    }

    suspend fun getPrefsOnce(): CalendarSyncPrefs {
        return prefsFlow.first()
    }

    suspend fun setEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_ENABLED] = enabled }
    }

    suspend fun setSelectedCalendar(calendar: DeviceCalendar) {
        dataStore.edit { prefs ->
            prefs[KEY_SELECTED_CALENDAR_ID] = calendar.id
            prefs[KEY_SELECTED_CALENDAR_NAME] = calendar.displayName
        }
    }

    suspend fun clearSelectedCalendar() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_SELECTED_CALENDAR_ID)
            prefs.remove(KEY_SELECTED_CALENDAR_NAME)
        }
    }

    private companion object {
        val KEY_ENABLED: Preferences.Key<Boolean> = booleanPreferencesKey("calendar_sync_enabled")
        val KEY_SELECTED_CALENDAR_ID: Preferences.Key<Long> = longPreferencesKey("calendar_sync_calendar_id")
        val KEY_SELECTED_CALENDAR_NAME: Preferences.Key<String> = stringPreferencesKey("calendar_sync_calendar_name")
    }
}

