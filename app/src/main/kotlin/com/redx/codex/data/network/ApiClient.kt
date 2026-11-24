package com.redx.codex.data.network

import com.redx.codex.domain.model.ServerConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

// ── REST API client using OkHttp ──────────────────────────────────────────────
// Evidence: Phase 4 — custom headers X-OpenCode-Req, X-OpenCode-Attempt,
//           X-OpenCode-Capabilities, Bearer auth

private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
    explicitNulls = false
}

@Singleton
class ApiClient @Inject constructor() {

    private val _config = MutableStateFlow<ServerConfig?>(null)
    val config: StateFlow<ServerConfig?> = _config

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val cfg = _config.value
            val builder = chain.request().newBuilder()
                .header("X-OpenCode-Req", generateRequestId())
                .header("X-OpenCode-Capabilities", "streaming,tools,vision")
            if (cfg?.token != null) {
                builder.header("Authorization", "Bearer ${cfg.token}")
            }
            chain.proceed(builder.build())
        }
        .build()

    fun setConfig(config: ServerConfig) {
        _config.value = config
    }

    fun clearConfig() {
        _config.value = null
    }

    private fun baseUrl(): String =
        _config.value?.baseUrl ?: throw IllegalStateException("Server not configured")

    private fun generateRequestId(): String =
        java.util.UUID.randomUUID().toString().replace("-", "").take(16)

    // ── GET helper ────────────────────────────────────────────────────────────
    private suspend fun get(path: String): String {
        val request = Request.Builder()
            .url("${baseUrl()}$path")
            .get()
            .build()
        return execute(request)
    }

    // ── POST helper ───────────────────────────────────────────────────────────
    private suspend fun post(path: String, body: String): String {
        val request = Request.Builder()
            .url("${baseUrl()}$path")
            .post(body.toRequestBody(JSON_MEDIA))
            .build()
        return execute(request)
    }

    // ── PUT helper ────────────────────────────────────────────────────────────
    private suspend fun put(path: String, body: String): String {
        val request = Request.Builder()
            .url("${baseUrl()}$path")
            .put(body.toRequestBody(JSON_MEDIA))
            .build()
        return execute(request)
    }

    // ── DELETE helper ─────────────────────────────────────────────────────────
    private suspend fun delete(path: String): String {
        val request = Request.Builder()
            .url("${baseUrl()}$path")
            .delete()
            .build()
        return execute(request)
    }

    private suspend fun execute(request: Request): String =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    throw ApiException(response.code, errBody)
                }
                response.body?.string() ?: ""
            }
        }

    // ── Server info ───────────────────────────────────────────────────────────
    suspend fun getServerInfo(): ServerInfoResponse =
        json.decodeFromString(get("/api/info"))

    // ── Sessions ──────────────────────────────────────────────────────────────
    suspend fun getSessions(): SessionListResponse =
        json.decodeFromString(get("/api/session"))

    suspend fun createSession(req: CreateSessionRequest): SessionResponse =
        json.decodeFromString(post("/api/session", json.encodeToString(req)))

    suspend fun getSession(id: String): SessionResponse =
        json.decodeFromString(get("/api/session/$id"))

    suspend fun renameSession(id: String, title: String): SessionResponse =
        json.decodeFromString(put("/api/session/$id", json.encodeToString(RenameSessionRequest(title))))

    suspend fun deleteSession(id: String) =
        delete("/api/session/$id")

    suspend fun pinSession(id: String, pinned: Boolean) =
        put("/api/session/$id/pin", json.encodeToString(mapOf("pinned" to pinned)))

    suspend fun moveSession(id: String, folderId: String?) =
        put("/api/session/$id/folder", json.encodeToString(MoveSessionRequest(folderId)))

    // ── Messages ──────────────────────────────────────────────────────────────
    suspend fun getMessages(sessionId: String): MessagesListResponse =
        json.decodeFromString(get("/api/session/$sessionId/message"))

    suspend fun sendMessage(req: SendMessageRequest): MessageResponse =
        json.decodeFromString(post("/api/message", json.encodeToString(req)))

    suspend fun interruptSession(sessionId: String) =
        post("/api/interrupt", json.encodeToString(InterruptRequest(sessionId)))

    // ── Models ────────────────────────────────────────────────────────────────
    suspend fun getModels(): ModelListResponse =
        json.decodeFromString(get("/api/model"))

    // ── Folders ───────────────────────────────────────────────────────────────
    suspend fun createFolder(name: String, color: String? = null): FolderResponse =
        json.decodeFromString(post("/api/folder", json.encodeToString(CreateFolderRequest(name, color))))

    suspend fun deleteFolder(id: String) =
        delete("/api/folder/$id")

    // ── Auth / ping ───────────────────────────────────────────────────────────
    suspend fun ping(): Boolean = try {
        get("/api/ping")
        true
    } catch (e: Exception) {
        false
    }

    fun getOkHttpClient(): OkHttpClient = client
}

class ApiException(val code: Int, val body: String) : IOException("HTTP $code: $body")
