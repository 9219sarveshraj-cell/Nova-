package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val languageCode: String = "hinglish",
    val previewText: String = "",
    val messageCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val isFromUser: Boolean,
    val content: String,
    val languageBadge: String = "Hinglish",
    val sourceBadge: String = "General AI Knowledge",
    val attachedScreenTitle: String? = null,
    val attachedScreenSnippet: String? = null,
    val hasAttachedImage: Boolean = false,
    val attachedImageDescription: String? = null,
    val isError: Boolean = false,
    val retryPrompt: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
