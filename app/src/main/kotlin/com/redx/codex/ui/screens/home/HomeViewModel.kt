package com.redx.codex.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.redx.codex.data.local.PreferencesManager
import com.redx.codex.data.network.WebSocketManager
import com.redx.codex.data.network.WsState
import com.redx.codex.data.repository.SessionRepository
import com.redx.codex.domain.model.AppSettings
import com.redx.codex.domain.model.Folder
import com.redx.codex.domain.model.Session
import com.redx.codex.util.HapticManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val sessions: List<Session> = emptyList(),
    val folders: List<Folder> = emptyList(),
    val wsState: WsState = WsState.Disconnected,
    val settings: AppSettings = AppSettings(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val sessionRepo: SessionRepository,
    private val wsManager: WebSocketManager,
    private val prefs: PreferencesManager,
    private val hapticManager: HapticManager,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        sessionRepo.observeRootSessions(),
        sessionRepo.observeFolders(),
        wsManager.state,
        prefs.settings,
    ) { sessions, folders, wsState, settings ->
        HomeUiState(
            sessions = sessions,
            folders = folders,
            wsState = wsState,
            settings = settings,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    init {
        viewModelScope.launch {
            prefs.settings.firstOrNull()?.serverConfig?.let { config ->
                wsManager.connect(config)
                runCatching { sessionRepo.syncFromServer() }
            }
        }
    }

    fun createNewSession(title: String = "New Chat") {
        hapticManager.click()
        viewModelScope.launch {
            runCatching {
                sessionRepo.createSession(title, uiState.value.settings.selectedModelId)
            }
        }
    }

    fun deleteSession(id: String) {
        hapticManager.heavyClick()
        viewModelScope.launch {
            runCatching { sessionRepo.deleteSession(id) }
        }
    }

    fun pinSession(id: String, pinned: Boolean) {
        hapticManager.click()
        viewModelScope.launch {
            runCatching { sessionRepo.pinSession(id, pinned) }
        }
    }

    fun renameSession(id: String, title: String) {
        hapticManager.click()
        viewModelScope.launch {
            runCatching { sessionRepo.renameSession(id, title) }
        }
    }

    fun refresh() {
        hapticManager.click()
        viewModelScope.launch {
            runCatching { sessionRepo.syncFromServer() }
        }
    }
}
