package com.bordrotakip.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

internal val Context.userPrefsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_prefs"
)

