package com.redx.codex.data.network

import android.util.Log
import com.redx.codex.domain.model.ServerConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import okhttp3.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

private const val TAG = "WebSocketManager"
private val json = Json { ignoreUnknownKeys = true; isLenient = true }

sealed class WsState {
    object Disconnected : WsState()
    object Connecting : WsState()
    data class Connected(val config: ServerConfig) : WsState()
    data class Error(val message: String) : WsState()
}

@Singleton
class WebSocketManager @Inject constructor(
    private val apiClient: ApiClient,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow<WsState>(WsState.Disconnected)
    val state: StateFlow<WsState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<WsEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<WsEvent> = _events.asSharedFlow()

    private var ws: WebSocket? = null
    private var reconnectJob: Job? = null
    private var reconnectDelay = 1000L

    // ── Connect ───────────────────────────────────────────────────────────────
    fun connect(config: ServerConfig) {
        apiClient.setConfig(config)
        reconnectJob?.cancel()
        reconnectDelay = 1000L
        doConnect(config)
    }

    private fun doConnect(config: ServerConfig) {
        _state.value = WsState.Connecting
        val url = "${config.wsUrl}/api/event"
        val reqBuilder = Request.Builder().url(url)
        if (config.token != null) {
            reqBuilder.header("Authorization", "Bearer ${config.token}")
        }
        reqBuilder.header("X-OpenCode-Capabilities", "streaming,tools,vision")

        ws = apiClient.getOkHttpClient().newWebSocket(reqBuilder.build(), object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WS connected to $url")
                reconnectDelay = 1000L
                _state.value = WsState.Connected(config)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                scope.launch {
                    try {
                        val event = json.decodeFromString<WsEvent>(text)
                        if (event.type != WsEventType.SERVER_PING) {
                            _events.emit(event)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to parse WS event: $text", e)
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w(TAG, "WS failure: ${t.message}")
                _state.value = WsState.Error(t.message ?: "Connection failed")
                scheduleReconnect(config)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WS closed: $code $reason")
                if (_state.value is WsState.Connected) {
                    _state.value = WsState.Disconnected
                    scheduleReconnect(config)
                }
            }
        })
    }

    private fun scheduleReconnect(config: ServerConfig) {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            Log.d(TAG, "Reconnecting in ${reconnectDelay}ms...")
            delay(reconnectDelay)
            reconnectDelay = min(reconnectDelay * 2, 30_000L)
            doConnect(config)
        }
    }

    // ── Disconnect ────────────────────────────────────────────────────────────
    fun disconnect() {
        reconnectJob?.cancel()
        ws?.close(1000, "User disconnected")
        ws = null
        apiClient.clearConfig()
        _state.value = WsState.Disconnected
    }

    // ── Send raw WS message (e.g., interrupt) ─────────────────────────────────
    fun sendRaw(text: String): Boolean = ws?.send(text) ?: false
}
