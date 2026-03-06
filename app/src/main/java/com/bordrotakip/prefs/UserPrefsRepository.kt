package com.bordrotakip.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class UserPrefs(
    val isOnboardingCompleted: Boolean,
    val defaultShiftHours: Double
)

@Singleton
class UserPrefsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val prefsFlow: Flow<UserPrefs> = context.userPrefsDataStore.data.map { prefs ->
        val defaultShiftHours = prefs[KEY_DEFAULT_SHIFT_HOURS] ?: DEFAULT_SHIFT_HOURS
        UserPrefs(
            isOnboardingCompleted = prefs[KEY_ONBOARDING_COMPLETED] ?: false,
            defaultShiftHours = defaultShiftHours.toDouble()
        )
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.userPrefsDataStore.edit { it[KEY_ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setDefaultShiftHours(hours: Double) {
        context.userPrefsDataStore.edit {
            it[KEY_DEFAULT_SHIFT_HOURS] = hours.toFloat().coerceIn(1.0f, 12.0f)
        }
    }

    private companion object {
        const val DEFAULT_SHIFT_HOURS: Float = 7.5f

        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val KEY_DEFAULT_SHIFT_HOURS = floatPreferencesKey("default_shift_hours")
    }
}

