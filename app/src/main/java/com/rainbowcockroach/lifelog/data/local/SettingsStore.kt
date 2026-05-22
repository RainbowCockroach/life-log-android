package com.rainbowcockroach.lifelog.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "lifelog_settings")

/**
 * App settings persisted via DataStore.
 *
 * Stores API base URL + API key. Both are required before any sync can run.
 *
 * Security note: API key is stored as plain Preferences. For a personal diary app this is
 * usually acceptable (app sandbox), but if you publish, encrypt it via Android Keystore
 * (EncryptedSharedPreferences or a Tink AEAD primitive).
 */
class SettingsStore(private val context: Context) {

    val baseUrl: Flow<String> = context.dataStore.data.map {
        it[KEY_BASE_URL] ?: DEFAULT_BASE_URL
    }

    val apiKey: Flow<String> = context.dataStore.data.map {
        it[KEY_API_KEY] ?: ""
    }

    suspend fun currentBaseUrl(): String = baseUrl.first()
    suspend fun currentApiKey(): String = apiKey.first()

    suspend fun setBaseUrl(value: String) {
        context.dataStore.edit { it[KEY_BASE_URL] = value.trim().trimEnd('/') }
    }

    suspend fun setApiKey(value: String) {
        context.dataStore.edit { it[KEY_API_KEY] = value.trim() }
    }

    companion object {
        private val KEY_BASE_URL = stringPreferencesKey("base_url")
        private val KEY_API_KEY = stringPreferencesKey("api_key")

        /** Emulator host loopback. Replace via Settings screen for real device or prod. */
        const val DEFAULT_BASE_URL = "http://10.0.2.2:3000"
    }
}
