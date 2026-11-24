package com.redx.codex.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.redx.codex.data.local.DraftDao
import com.redx.codex.data.local.DraftEntity
import com.redx.codex.data.local.PreferencesManager
import com.redx.codex.data.network.WebSocketManager
import com.redx.codex.data.network.WsEvent
import com.redx.codex.data.network.WsEventType
import com.redx.codex.data.network.WsState
import com.redx.codex.data.repository.MessageRepository
import com.redx.codex.domain.model.*
import com.redx.codex.util.HapticManager
import com.redx.codex.util.SoundManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val draftText: String = "",
    val isStreaming: Boolean = false,
    val streamingMessageId: String? = null,
    val error: String? = null,
    val wsState: WsState = WsState.Disconnected,
    val selectedModel: Model? = null,
    val showModelPicker: Boolean = false,
    val soundEffects: Boolean = true,
    val hapticFeedback: Boolean = true,
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val messageRepo: MessageRepository,
    private val wsManager: WebSocketManager,
    private val draftDao: DraftDao,
    private val prefs: PreferencesManager,
    private val soundManager: SoundManager,
    private val hapticManager: HapticManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var currentSessionId: String? = null
    private var wsJob: Job? = null
    private var streamingBuffer = StringBuilder()

    init {
        viewModelScope.launch {
            prefs.settings.collect { settings ->
                _uiState.update {
                    it.copy(
                        soundEffects = settings.soundEffects,
                        hapticFeedback = settings.hapticFeedback,
                    )
                }
            }
        }
    }

    fun init(sessionId: String, model: Model?) {
        currentSessionId = sessionId
        _uiState.update { it.copy(selectedModel = model) }

        // Load messages
        viewModelScope.launch {
            messageRepo.observeMessages(sessionId)
                .collect { msgs ->
                    _uiState.update { it.copy(messages = msgs) }
                }
        }

        // Load draft
        viewModelScope.launch {
            val draft = draftDao.get(sessionId)
            if (draft != null) {
                _uiState.update { it.copy(draftText = draft.text) }
            }
        }

        // Sync messages
        viewModelScope.launch {
            runCatching { messageRepo.syncMessages(sessionId) }
        }

        // Subscribe to WS events
        wsJob = viewModelScope.launch {
            wsManager.events.collect { event ->
                handleWsEvent(event)
            }
        }

        // Track WS state
        viewModelScope.launch {
            wsManager.state.collect { state ->
                _uiState.update { it.copy(wsState = state) }
            }
        }
    }

    private fun handleWsEvent(event: WsEvent) {
        val sid = currentSessionId ?: return
        if (event.sessionId != null && event.sessionId != sid) return

        when (event.type) {
            WsEventType.SESSION_TEXT_DELTA -> {
                val msgId = event.messageId ?: return
                val delta = event.delta ?: return
                streamingBuffer.append(delta)
                _uiState.update { state ->
                    val msgs = state.messages.toMutableList()
                    val idx = msgs.indexOfFirst { it.id == msgId }
                    if (idx >= 0) {
                        val existing = msgs[idx]
                        val updatedParts = listOf(MessagePart.Text(streamingBuffer.toString()))
                        msgs[idx] = existing.copy(parts = updatedParts, status = MessageStatus.STREAMING)
                    } else {
                        msgs.add(
                            Message(
                                id = msgId, sessionId = sid, role = MessageRole.ASSISTANT,
                                parts = listOf(MessagePart.Text(streamingBuffer.toString())),
                                createdAt = System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis(),
                                status = MessageStatus.STREAMING,
                            )
                        )
                    }
                    state.copy(messages = msgs, isStreaming = true, streamingMessageId = msgId)
                }
            }

            WsEventType.SESSION_MESSAGE_DONE -> {
                streamingBuffer.clear()
                if (_uiState.value.soundEffects) soundManager.playReplyDone()
                if (_uiState.value.hapticFeedback) hapticManager.doubleClick()

                val updatedMsg = event.message
                if (updatedMsg != null) {
                    viewModelScope.launch {
                        runCatching { messageRepo.syncMessages(sid) }
                    }
                }
                _uiState.update { it.copy(isStreaming = false, streamingMessageId = null) }
            }

            WsEventType.SESSION_ERROR -> {
                streamingBuffer.clear()
                if (_uiState.value.soundEffects) soundManager.playError()
                if (_uiState.value.hapticFeedback) hapticManager.error()
                _uiState.update {
                    it.copy(isStreaming = false, error = event.error ?: "Stream error")
                }
            }

            WsEventType.SESSION_TOOL_CALLED -> {
                if (_uiState.value.soundEffects) soundManager.playNeedsInput()
                val msgId = event.messageId ?: return
                _uiState.update { state ->
                    val msgs = state.messages.toMutableList()
                    val toolPart = MessagePart.ToolCall(
                        toolCallId = event.toolCallId ?: "",
                        toolName = event.toolName ?: "tool",
                        args = event.args ?: "{}",
                    )
                    val existing = msgs.indexOfFirst { it.id == msgId }
                    if (existing >= 0) {
                        val parts = msgs[existing].parts + toolPart
                        msgs[existing] = msgs[existing].copy(parts = parts)
                    }
                    state.copy(messages = msgs)
                }
            }
        }
    }

    fun sendMessage(text: String) {
        val sid = currentSessionId ?: return
        if (text.isBlank()) return

        if (_uiState.value.soundEffects) soundManager.playSend()
        if (_uiState.value.hapticFeedback) hapticManager.click()

        val optimisticMsg = Message(
            id = UUID.randomUUID().toString(),
            sessionId = sid,
            role = MessageRole.USER,
            parts = listOf(MessagePart.Text(text.trim())),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            status = MessageStatus.DONE,
        )

        viewModelScope.launch {
            messageRepo.insertOptimistic(optimisticMsg)
            updateDraft("")
            _uiState.update { it.copy(isStreaming = true, error = null) }

            runCatching {
                messageRepo.sendMessage(sid, text.trim(), _uiState.value.selectedModel?.id)
            }.onFailure { e ->
                _uiState.update { it.copy(isStreaming = false, error = e.message) }
                messageRepo.enqueueOutbox(sid, optimisticMsg)
            }
        }
    }

    fun interruptStreaming() {
        if (_uiState.value.hapticFeedback) hapticManager.heavyClick()
        viewModelScope.launch {
            currentSessionId?.let {
                runCatching { messageRepo.interruptSession(it) }
            }
            _uiState.update { it.copy(isStreaming = false) }
        }
    }

    fun updateDraft(text: String) {
        _uiState.update { it.copy(draftText = text) }
        val sid = currentSessionId ?: return
        viewModelScope.launch {
            if (text.isNotEmpty()) {
                draftDao.upsert(DraftEntity(sid, text, updatedAt = System.currentTimeMillis()))
            } else {
                draftDao.delete(sid)
            }
        }
    }

    fun selectModel(model: Model) {
        if (_uiState.value.hapticFeedback) hapticManager.click()
        _uiState.update { it.copy(selectedModel = model, showModelPicker = false) }
    }

    fun toggleModelPicker() {
        if (_uiState.value.hapticFeedback) hapticManager.click()
        _uiState.update { it.copy(showModelPicker = !it.showModelPicker) }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    override fun onCleared() {
        super.onCleared()
        wsJob?.cancel()
    }
}
