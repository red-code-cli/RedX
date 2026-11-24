package com.redx.codex.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── Core domain models reversed from OpenCode DEX / network traffic ──────────
// Evidence: Phase 4 — decompiled data classes, Room schema, WebSocket protocol

@Serializable
data class Session(
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val modelId: String?,
    val parentFolderId: String?,
    val isPinned: Boolean = false,
    val messageCount: Int = 0,
    val lastMessage: String? = null,
)

@Serializable
data class Folder(
    val id: String,
    val name: String,
    val createdAt: Long,
    val color: String? = null,
    val sessionIds: List<String> = emptyList(),
)

@Serializable
data class Message(
    val id: String,
    val sessionId: String,
    val role: MessageRole,
    val parts: List<MessagePart>,
    val createdAt: Long,
    val updatedAt: Long,
    val status: MessageStatus = MessageStatus.DONE,
    val modelId: String? = null,
    val usage: TokenUsage? = null,
    val error: String? = null,
)

@Serializable
enum class MessageRole {
    @SerialName("user") USER,
    @SerialName("assistant") ASSISTANT,
    @SerialName("tool") TOOL,
    @SerialName("system") SYSTEM,
}

@Serializable
enum class MessageStatus {
    @SerialName("streaming") STREAMING,
    @SerialName("done") DONE,
    @SerialName("error") ERROR,
    @SerialName("interrupted") INTERRUPTED,
}

@Serializable
sealed class MessagePart {
    @Serializable
    @SerialName("text")
    data class Text(val text: String) : MessagePart()

    @Serializable
    @SerialName("image")
    data class Image(val url: String, val mimeType: String = "image/jpeg") : MessagePart()

    @Serializable
    @SerialName("tool_call")
    data class ToolCall(
        val toolCallId: String,
        val toolName: String,
        val args: String,         // JSON string
        val result: String? = null,
        val isError: Boolean = false,
    ) : MessagePart()

    @Serializable
    @SerialName("code")
    data class Code(val language: String, val content: String) : MessagePart()
}

@Serializable
data class TokenUsage(
    val inputTokens: Int,
    val outputTokens: Int,
    val totalTokens: Int,
)

@Serializable
data class Model(
    val id: String,
    val name: String,
    val provider: String,
    val contextLength: Int = 0,
    val supportsVision: Boolean = false,
    val supportsTools: Boolean = true,
    val isDefault: Boolean = false,
)

@Serializable
data class ServerConfig(
    val host: String,
    val port: Int = 4096,
    val token: String? = null,
    val useTls: Boolean = false,
) {
    val baseUrl: String get() =
        "${if (useTls) "https" else "http"}://$host:$port"
    val wsUrl: String get() =
        "${if (useTls) "wss" else "ws"}://$host:$port"
}

@Serializable
data class AppSettings(
    val serverConfig: ServerConfig? = null,
    val selectedModelId: String? = null,
    val hapticFeedback: Boolean = true,
    val soundEffects: Boolean = true,
    val messageFontSizeSp: Int = 15,
    val sendWithEnter: Boolean = false,
    val showTokenUsage: Boolean = false,
)

// Draft / Composer state
@Serializable
data class ComposerDraft(
    val sessionId: String,
    val text: String,
    val attachmentPaths: List<String> = emptyList(),
)

// QR pairing payload
@Serializable
data class PairingPayload(
    val host: String,
    val port: Int,
    val token: String? = null,
    val version: String = "1",
)
