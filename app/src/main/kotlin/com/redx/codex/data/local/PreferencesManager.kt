package com.redx.codex.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.redx.codex.domain.model.AppSettings
import com.redx.codex.domain.model.ServerConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "redx_prefs")

private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val store = context.dataStore

    companion object {
        val KEY_SERVER_CONFIG = stringPreferencesKey("server_config")
        val KEY_MODEL_ID = stringPreferencesKey("model_id")
        val KEY_HAPTIC = booleanPreferencesKey("haptic")
        val KEY_SOUNDS = booleanPreferencesKey("sounds")
        val KEY_FONT_SIZE = intPreferencesKey("font_size")
        val KEY_SEND_ENTER = booleanPreferencesKey("send_enter")
        val KEY_SHOW_TOKENS = booleanPreferencesKey("show_tokens")
    }

    val settings: Flow<AppSettings> = store.data.map { prefs ->
        AppSettings(
            serverConfig = prefs[KEY_SERVER_CONFIG]?.let {
                runCatching { json.decodeFromString<ServerConfig>(it) }.getOrNull()
            },
            selectedModelId = prefs[KEY_MODEL_ID],
            hapticFeedback = prefs[KEY_HAPTIC] ?: true,
            soundEffects = prefs[KEY_SOUNDS] ?: true,
            messageFontSizeSp = prefs[KEY_FONT_SIZE] ?: 15,
            sendWithEnter = prefs[KEY_SEND_ENTER] ?: false,
            showTokenUsage = prefs[KEY_SHOW_TOKENS] ?: false,
        )
    }

    suspend fun saveServerConfig(config: ServerConfig) {
        store.edit { it[KEY_SERVER_CONFIG] = json.encodeToString(config) }
    }

    suspend fun clearServerConfig() {
        store.edit { it.remove(KEY_SERVER_CONFIG) }
    }

    suspend fun saveModelId(id: String) {
        store.edit { it[KEY_MODEL_ID] = id }
    }

    suspend fun setHaptic(enabled: Boolean) {
        store.edit { it[KEY_HAPTIC] = enabled }
    }

    suspend fun setSounds(enabled: Boolean) {
        store.edit { it[KEY_SOUNDS] = enabled }
    }

    suspend fun setFontSize(sp: Int) {
        store.edit { it[KEY_FONT_SIZE] = sp }
    }

    suspend fun setSendWithEnter(value: Boolean) {
        store.edit { it[KEY_SEND_ENTER] = value }
    }

    suspend fun setShowTokens(value: Boolean) {
        store.edit { it[KEY_SHOW_TOKENS] = value }
    }
}
