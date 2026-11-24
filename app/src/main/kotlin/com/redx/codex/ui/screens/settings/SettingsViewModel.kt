package com.redx.codex.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.redx.codex.data.local.PreferencesManager
import com.redx.codex.data.network.ApiClient
import com.redx.codex.data.network.WebSocketManager
import com.redx.codex.data.network.WsState
import com.redx.codex.domain.model.AppSettings
import com.redx.codex.domain.model.Model
import com.redx.codex.domain.model.ServerConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val models: List<Model> = emptyList(),
    val wsState: WsState = WsState.Disconnected,
    val isLoadingModels: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: PreferencesManager,
    private val apiClient: ApiClient,
    private val wsManager: WebSocketManager,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        prefs.settings,
        wsManager.state,
    ) { settings, wsState ->
        SettingsUiState(settings = settings, wsState = wsState)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun updateServer(host: String, port: Int, token: String?) {
        viewModelScope.launch {
            val config = ServerConfig(host = host, port = port, token = token.takeIf { !it.isNullOrBlank() })
            prefs.saveServerConfig(config)
            wsManager.connect(config)
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            wsManager.disconnect()
            prefs.clearServerConfig()
        }
    }

    fun setHaptic(enabled: Boolean) {
        viewModelScope.launch { prefs.setHaptic(enabled) }
    }

    fun setSounds(enabled: Boolean) {
        viewModelScope.launch { prefs.setSounds(enabled) }
    }

    fun setFontSize(sp: Int) {
        viewModelScope.launch { prefs.setFontSize(sp) }
    }

    fun setSendWithEnter(value: Boolean) {
        viewModelScope.launch { prefs.setSendWithEnter(value) }
    }

    fun setShowTokens(value: Boolean) {
        viewModelScope.launch { prefs.setShowTokens(value) }
    }

    fun selectModel(id: String) {
        viewModelScope.launch { prefs.saveModelId(id) }
    }

    fun loadModels() {
        viewModelScope.launch {
            runCatching {
                val resp = apiClient.getModels()
                // Publish to UI state
            }
        }
    }
}
