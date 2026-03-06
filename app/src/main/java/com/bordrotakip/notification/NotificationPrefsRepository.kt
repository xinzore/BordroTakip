package com.bordrotakip.notification

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class NotificationPrefs(
    val isShiftPromptEnabled: Boolean,
    val lastShiftPromptEpochDay: Long?,
    val lastShiftLoggedEpochDay: Long?,
    val lastWeeklyOvertimePromptWeekStartEpochDay: Long?,
    val isTaxBracketAlertsEnabled: Boolean,
    val lastTaxAlertEpochDay: Long?,
    val lastTaxAlertTier: Int?,
    val lastTaxAlertBracketPercentage: Int?,
    val lastTaxAlertYear: Int?
)

@Singleton
class NotificationPrefsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.notificationDataStore

    val prefsFlow: Flow<NotificationPrefs> = dataStore.data.map { prefs ->
        NotificationPrefs(
            isShiftPromptEnabled = prefs[KEY_SHIFT_PROMPT_ENABLED] ?: false,
            lastShiftPromptEpochDay = prefs[KEY_LAST_SHIFT_PROMPT_EPOCH_DAY],
            lastShiftLoggedEpochDay = prefs[KEY_LAST_SHIFT_LOGGED_EPOCH_DAY],
            lastWeeklyOvertimePromptWeekStartEpochDay = prefs[KEY_LAST_WEEKLY_OVERTIME_PROMPT_WEEK_START_EPOCH_DAY],
            isTaxBracketAlertsEnabled = prefs[KEY_TAX_BRACKET_ALERTS_ENABLED] ?: false,
            lastTaxAlertEpochDay = prefs[KEY_LAST_TAX_ALERT_EPOCH_DAY],
            lastTaxAlertTier = prefs[KEY_LAST_TAX_ALERT_TIER],
            lastTaxAlertBracketPercentage = prefs[KEY_LAST_TAX_ALERT_BRACKET_PERCENTAGE],
            lastTaxAlertYear = prefs[KEY_LAST_TAX_ALERT_YEAR]
        )
    }

    suspend fun setShiftPromptEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_SHIFT_PROMPT_ENABLED] = enabled }
    }

    suspend fun setLastShiftPromptEpochDay(epochDay: Long) {
        dataStore.edit { it[KEY_LAST_SHIFT_PROMPT_EPOCH_DAY] = epochDay }
    }

    suspend fun setLastShiftLoggedEpochDay(epochDay: Long) {
        dataStore.edit { it[KEY_LAST_SHIFT_LOGGED_EPOCH_DAY] = epochDay }
    }

    suspend fun setLastWeeklyOvertimePromptWeekStartEpochDay(epochDay: Long) {
        dataStore.edit { it[KEY_LAST_WEEKLY_OVERTIME_PROMPT_WEEK_START_EPOCH_DAY] = epochDay }
    }

    suspend fun setTaxBracketAlertsEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_TAX_BRACKET_ALERTS_ENABLED] = enabled }
    }

    suspend fun setLastTaxAlert(
        epochDay: Long,
        tier: Int,
        bracketPercentage: Int,
        year: Int
    ) {
        dataStore.edit {
            it[KEY_LAST_TAX_ALERT_EPOCH_DAY] = epochDay
            it[KEY_LAST_TAX_ALERT_TIER] = tier
            it[KEY_LAST_TAX_ALERT_BRACKET_PERCENTAGE] = bracketPercentage
            it[KEY_LAST_TAX_ALERT_YEAR] = year
        }
    }

    private companion object {
        val KEY_SHIFT_PROMPT_ENABLED = booleanPreferencesKey("shift_prompt_enabled")
        val KEY_LAST_SHIFT_PROMPT_EPOCH_DAY = longPreferencesKey("last_shift_prompt_epoch_day")
        val KEY_LAST_SHIFT_LOGGED_EPOCH_DAY = longPreferencesKey("last_shift_logged_epoch_day")
        val KEY_LAST_WEEKLY_OVERTIME_PROMPT_WEEK_START_EPOCH_DAY =
            longPreferencesKey("last_weekly_overtime_prompt_week_start_epoch_day")

        val KEY_TAX_BRACKET_ALERTS_ENABLED = booleanPreferencesKey("tax_bracket_alerts_enabled")
        val KEY_LAST_TAX_ALERT_EPOCH_DAY = longPreferencesKey("last_tax_alert_epoch_day")
        val KEY_LAST_TAX_ALERT_TIER = intPreferencesKey("last_tax_alert_tier")
        val KEY_LAST_TAX_ALERT_BRACKET_PERCENTAGE = intPreferencesKey("last_tax_alert_bracket_percentage")
        val KEY_LAST_TAX_ALERT_YEAR = intPreferencesKey("last_tax_alert_year")
    }
}
