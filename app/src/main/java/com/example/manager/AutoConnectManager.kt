package com.example.manager

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "lanu_settings")

object AutoConnectManager {
  private val AUTO_CONNECT_KEY = booleanPreferencesKey("auto_connect_enabled")

  fun isAutoConnectEnabled(context: Context): Flow<Boolean> {
    return context.dataStore.data.map { preferences ->
      preferences[AUTO_CONNECT_KEY] ?: false
    }
  }

  suspend fun setAutoConnectEnabled(context: Context, enabled: Boolean) {
    context.dataStore.edit { preferences ->
      preferences[AUTO_CONNECT_KEY] = enabled
    }
  }
}
