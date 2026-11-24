package com.redx.codex.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// ── Room Entities (schema reversed from base.apk DEX + SQLite PRAGMA) ─────────
// Evidence: Phase 4 — Room schema, table names from string pool & class analysis

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val modelId: String?,
    val parentFolderId: String?,
    val isPinned: Boolean = false,
    val messageCount: Int = 0,
    val lastMessage: String? = null,
)

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long,
    val color: String? = null,
)

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("sessionId")],
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val role: String,          // "user" | "assistant" | "tool" | "system"
    val partsJson: String,     // JSON array of MessagePart
    val createdAt: Long,
    val updatedAt: Long,
    val status: String = "done",
    val modelId: String? = null,
    val usageJson: String? = null,  // JSON TokenUsage
    val error: String? = null,
)

@Entity(tableName = "drafts")
data class DraftEntity(
    @PrimaryKey val sessionId: String,
    val text: String,
    val attachmentPathsJson: String = "[]",
    val updatedAt: Long,
)

@Entity(tableName = "outbox")
data class OutboxEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val messageJson: String,   // full serialized Message
    val createdAt: Long,
    val attempts: Int = 0,
    val lastAttemptAt: Long? = null,
    val error: String? = null,
)
