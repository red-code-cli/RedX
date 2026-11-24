package com.redx.codex.data.network

import com.redx.codex.domain.model.*
import kotlinx.serialization.Serializable

// ── API DTOs — exact contract reversed from Phase 4 network analysis ──────────
// Evidence: 16 REST endpoints + WebSocket event protocol

// ─── REST Request/Response DTOs ───────────────────────────────────────────────

@Serializable
data class CreateSessionRequest(
    val title: String = "New Chat",
    val modelId: String? = null,
    val parentFolderId: String? = null,
)

@Serializable
data class SessionResponse(
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val modelId: String? = null,
    val parentFolderId: String? = null,
    val isPinned: Boolean = false,
    val messageCount: Int = 0,
    val lastMessage: String? = null,
)

@Serializable
data class SessionListResponse(
    val sessions: List<SessionResponse>,
    val folders: List<FolderResponse> = emptyList(),
)

@Serializable
data class FolderResponse(
    val id: String,
    val name: String,
    val createdAt: Long,
    val color: String? = null,
    val sessionIds: List<String> = emptyList(),
)

@Serializable
data class MessageResponse(
    val id: String,
    val sessionId: String,
    val role: String,
    val parts: List<PartResponse>,
    val createdAt: Long,
    val updatedAt: Long,
    val status: String = "done",
    val modelId: String? = null,
    val usage: UsageResponse? = null,
    val error: String? = null,
)

@Serializable
data class PartResponse(
    val type: String,
    val text: String? = null,
    val url: String? = null,
    val mimeType: String? = null,
    val toolCallId: String? = null,
    val toolName: String? = null,
    val args: String? = null,
    val result: String? = null,
    val isError: Boolean = false,
    val language: String? = null,
    val content: String? = null,
)

@Serializable
data class UsageResponse(
    val inputTokens: Int,
    val outputTokens: Int,
    val totalTokens: Int,
)

@Serializable
data class MessagesListResponse(
    val messages: List<MessageResponse>,
)

@Serializable
data class SendMessageRequest(
    val sessionId: String,
    val text: String,
    val attachments: List<AttachmentRequest> = emptyList(),
    val modelId: String? = null,
)

@Serializable
data class AttachmentRequest(
    val type: String,          // "image" | "file"
    val base64Data: String,
    val mimeType: String,
    val name: String,
)

@Serializable
data class ModelResponse(
    val id: String,
    val name: String,
    val provider: String,
    val contextLength: Int = 0,
    val supportsVision: Boolean = false,
    val supportsTools: Boolean = true,
    val isDefault: Boolean = false,
)

@Serializable
data class ModelListResponse(
    val models: List<ModelResponse>,
    val defaultModelId: String? = null,
)

@Serializable
data class ServerInfoResponse(
    val version: String,
    val capabilities: List<String> = emptyList(),
    val requiresAuth: Boolean = false,
)

@Serializable
data class AuthResponse(
    val token: String,
    val expiresAt: Long? = null,
)

@Serializable
data class ErrorResponse(
    val error: String,
    val code: String? = null,
)

@Serializable
data class InterruptRequest(
    val sessionId: String,
)

@Serializable
data class RenameSessionRequest(
    val title: String,
)

@Serializable
data class CreateFolderRequest(
    val name: String,
    val color: String? = null,
)

@Serializable
data class MoveSessionRequest(
    val folderId: String?,
)

// ─── WebSocket Event Protocol ─────────────────────────────────────────────────
// Evidence: Phase 4 — 10 event types from WS reverse engineering

@Serializable
data class WsEvent(
    val type: String,
    val sessionId: String? = null,
    val messageId: String? = null,
    val delta: String? = null,
    val message: MessageResponse? = null,
    val session: SessionResponse? = null,
    val error: String? = null,
    val toolCallId: String? = null,
    val toolName: String? = null,
    val args: String? = null,
    val result: String? = null,
    val isError: Boolean = false,
)

object WsEventType {
    const val SESSION_TEXT_DELTA = "session.text.delta"       // streaming text
    const val SESSION_MESSAGE_DONE = "session.message.done"   // message finalized
    const val SESSION_TOOL_CALLED = "session.tool.called"     // tool invocation
    const val SESSION_TOOL_RESULT = "session.tool.result"     // tool completed
    const val SESSION_ERROR = "session.error"                 // stream error
    const val SESSION_INTERRUPTED = "session.interrupted"     // user interrupted
    const val SESSION_UPDATED = "session.updated"             // metadata changed
    const val SESSION_CREATED = "session.created"             // new session
    const val SESSION_DELETED = "session.deleted"             // session removed
    const val SERVER_PING = "server.ping"                     // keepalive
}
