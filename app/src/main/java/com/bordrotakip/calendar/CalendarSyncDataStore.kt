package com.bordrotakip.calendar

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

internal val Context.calendarSyncDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "calendar_sync_prefs"
)

