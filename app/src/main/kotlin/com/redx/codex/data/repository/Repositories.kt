package com.redx.codex.data.repository

import com.redx.codex.data.local.*
import com.redx.codex.data.network.*
import com.redx.codex.domain.model.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
}

@Singleton
class SessionRepository @Inject constructor(
    private val sessionDao: SessionDao,
    private val folderDao: FolderDao,
    private val apiClient: ApiClient,
) {
    fun observeSessions(): Flow<List<Session>> =
        sessionDao.observeAll().map { it.map(::entityToModel) }

    fun observeRootSessions(): Flow<List<Session>> =
        sessionDao.observeRoot().map { it.map(::entityToModel) }

    fun observeFolders(): Flow<List<Folder>> =
        folderDao.observeAll().map { it.map(::folderEntityToModel) }

    suspend fun syncFromServer() {
        val resp = apiClient.getSessions()
        sessionDao.upsertAll(resp.sessions.map(::dtoToEntity))
        folderDao.upsertAll(resp.folders.map(::folderDtoToEntity))
    }

    suspend fun createSession(title: String, modelId: String? = null): Session {
        val resp = apiClient.createSession(CreateSessionRequest(title = title, modelId = modelId))
        val entity = dtoToEntity(resp)
        sessionDao.upsert(entity)
        return entityToModel(entity)
    }

    suspend fun deleteSession(id: String) {
        apiClient.deleteSession(id)
        sessionDao.delete(id)
    }

    suspend fun renameSession(id: String, title: String) {
        val resp = apiClient.renameSession(id, title)
        sessionDao.rename(id, title, resp.updatedAt)
    }

    suspend fun pinSession(id: String, pinned: Boolean) {
        apiClient.pinSession(id, pinned)
        sessionDao.setPin(id, pinned)
    }

    suspend fun createFolder(name: String, color: String? = null): Folder {
        val resp = apiClient.createFolder(name, color)
        val entity = folderDtoToEntity(resp)
        folderDao.upsert(entity)
        return folderEntityToModel(entity)
    }

    // ── Mappers ───────────────────────────────────────────────────────────────
    private fun dtoToEntity(dto: SessionResponse) = SessionEntity(
        id = dto.id, title = dto.title, createdAt = dto.createdAt, updatedAt = dto.updatedAt,
        modelId = dto.modelId, parentFolderId = dto.parentFolderId, isPinned = dto.isPinned,
        messageCount = dto.messageCount, lastMessage = dto.lastMessage,
    )

    private fun entityToModel(e: SessionEntity) = Session(
        id = e.id, title = e.title, createdAt = e.createdAt, updatedAt = e.updatedAt,
        modelId = e.modelId, parentFolderId = e.parentFolderId, isPinned = e.isPinned,
        messageCount = e.messageCount, lastMessage = e.lastMessage,
    )

    private fun folderDtoToEntity(dto: FolderResponse) = FolderEntity(
        id = dto.id, name = dto.name, createdAt = dto.createdAt, color = dto.color,
    )

    private fun folderEntityToModel(e: FolderEntity) = Folder(
        id = e.id, name = e.name, createdAt = e.createdAt, color = e.color,
    )
}

@Singleton
class MessageRepository @Inject constructor(
    private val messageDao: MessageDao,
    private val outboxDao: OutboxDao,
    private val apiClient: ApiClient,
    private val wsManager: WebSocketManager,
) {
    fun observeMessages(sessionId: String): Flow<List<Message>> =
        messageDao.observeBySession(sessionId).map { it.map(::entityToModel) }

    suspend fun syncMessages(sessionId: String) {
        val resp = apiClient.getMessages(sessionId)
        messageDao.upsertAll(resp.messages.map(::dtoToEntity))
    }

    suspend fun sendMessage(sessionId: String, text: String, modelId: String?): Message {
        val req = SendMessageRequest(sessionId = sessionId, text = text, modelId = modelId)
        val resp = apiClient.sendMessage(req)
        val entity = dtoToEntity(resp)
        messageDao.upsert(entity)
        return entityToModel(entity)
    }

    // Saves optimistic user message immediately
    suspend fun insertOptimistic(message: Message) {
        messageDao.upsert(modelToEntity(message))
    }

    suspend fun updateMessage(message: Message) {
        messageDao.upsert(modelToEntity(message))
    }

    suspend fun deleteMessage(id: String) {
        messageDao.delete(id)
    }

    suspend fun interruptSession(sessionId: String) {
        apiClient.interruptSession(sessionId)
    }

    // ── Outbox (offline queue) ─────────────────────────────────────────────────
    suspend fun enqueueOutbox(sessionId: String, message: Message) {
        outboxDao.insert(
            OutboxEntity(
                sessionId = sessionId,
                messageJson = json.encodeToString(message),
                createdAt = System.currentTimeMillis(),
            )
        )
    }

    suspend fun flushOutbox() {
        val items = outboxDao.getAll()
        for (item in items) {
            try {
                val msg = json.decodeFromString<Message>(item.messageJson)
                // Extract text from first Text part
                val text = msg.parts.filterIsInstance<MessagePart.Text>()
                    .firstOrNull()?.text ?: continue
                sendMessage(item.sessionId, text, null)
                outboxDao.delete(item.id)
            } catch (e: Exception) {
                outboxDao.recordAttempt(item.id, System.currentTimeMillis(), e.message)
            }
        }
    }

    // ── Mappers ───────────────────────────────────────────────────────────────
    private fun dtoToEntity(dto: MessageResponse) = MessageEntity(
        id = dto.id, sessionId = dto.sessionId, role = dto.role,
        partsJson = json.encodeToString(dto.parts.map(::partDtoToModel)),
        createdAt = dto.createdAt, updatedAt = dto.updatedAt,
        status = dto.status, modelId = dto.modelId,
        usageJson = dto.usage?.let { json.encodeToString(TokenUsage(it.inputTokens, it.outputTokens, it.totalTokens)) },
        error = dto.error,
    )

    private fun entityToModel(e: MessageEntity) = Message(
        id = e.id, sessionId = e.sessionId,
        role = MessageRole.entries.firstOrNull { it.name.lowercase() == e.role } ?: MessageRole.USER,
        parts = runCatching { json.decodeFromString<List<MessagePart>>(e.partsJson) }.getOrDefault(emptyList()),
        createdAt = e.createdAt, updatedAt = e.updatedAt,
        status = MessageStatus.entries.firstOrNull { it.name.lowercase() == e.status } ?: MessageStatus.DONE,
        modelId = e.modelId,
        usage = e.usageJson?.let { runCatching { json.decodeFromString<TokenUsage>(it) }.getOrNull() },
        error = e.error,
    )

    private fun modelToEntity(m: Message) = MessageEntity(
        id = m.id, sessionId = m.sessionId, role = m.role.name.lowercase(),
        partsJson = json.encodeToString(m.parts),
        createdAt = m.createdAt, updatedAt = m.updatedAt,
        status = m.status.name.lowercase(), modelId = m.modelId,
        usageJson = m.usage?.let { json.encodeToString(it) },
        error = m.error,
    )

    private fun partDtoToModel(dto: PartResponse): MessagePart = when (dto.type) {
        "text" -> MessagePart.Text(dto.text ?: "")
        "image" -> MessagePart.Image(dto.url ?: "", dto.mimeType ?: "image/jpeg")
        "tool_call" -> MessagePart.ToolCall(
            toolCallId = dto.toolCallId ?: "",
            toolName = dto.toolName ?: "",
            args = dto.args ?: "{}",
            result = dto.result,
            isError = dto.isError,
        )
        "code" -> MessagePart.Code(dto.language ?: "text", dto.content ?: "")
        else -> MessagePart.Text(dto.text ?: "")
    }
}
