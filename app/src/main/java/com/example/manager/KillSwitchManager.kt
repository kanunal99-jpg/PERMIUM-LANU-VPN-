package com.example.manager

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object KillSwitchManager {
  private val KILL_SWITCH_KEY = booleanPreferencesKey("kill_switch_enabled")

  fun isKillSwitchEnabled(context: Context): Flow<Boolean> {
    return context.dataStore.data.map { preferences ->
      preferences[KILL_SWITCH_KEY] ?: true // default true
    }
  }

  suspend fun setKillSwitchEnabled(context: Context, enabled: Boolean) {
    context.dataStore.edit { preferences ->
      preferences[KILL_SWITCH_KEY] = enabled
    }
  }
}
