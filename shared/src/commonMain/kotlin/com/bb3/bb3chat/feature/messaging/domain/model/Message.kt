package com.bb3.bb3chat.feature.messaging.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val id: String,
    val roomId: String,
    val senderAlias: String,
    val senderAvatarIndex: Int = 0,
    val content: MessageContent,
    val replyToId: String?          = null,
    val replyPreview: ReplyPreview? = null,
    val reactions: Map<String, List<String>> = emptyMap(),
    val destructConfig: DestructConfig?      = null,
    val status: MessageStatus = MessageStatus.PENDING,
    val readBy: Map<String, Long> = emptyMap(),
    val sentAt: Long,
    val serverAt: Long?  = null,
    val isEdited: Boolean = false,
    val editedAt: Long?   = null,
    val isStarred: Boolean = false,
    val isPinned: Boolean  = false,
    val isForwarded: Boolean = false,
    val forwardedFromRoom: String? = null
)

@Serializable
sealed class MessageContent {
    @Serializable data class Text(val encryptedBody: String, val iv: String) : MessageContent()

    @Serializable data class Image(
        val encryptedBase64: String,
        val iv: String,
        val mimeType: String,
        val originalWidth: Int   = 0,
        val originalHeight: Int  = 0,
        val compressedBytes: Int = 0,
        val blurHash: String     = ""
    ) : MessageContent()

    @Serializable data class Voice(
        val encryptedBase64: String,
        val iv: String,
        val durationMs: Long,
        val waveformData: List<Float> = emptyList()
    ) : MessageContent()

    @Serializable data class ChunkedFile(
        val totalChunks: Int,
        val mimeType: String,
        val fileName: String,
        val totalSizeBytes: Long,
        val chunkCollectionId: String
    ) : MessageContent()

    @Serializable data class Sticker(
        val encryptedBase64: String,
        val iv: String,
        val packId: String,
        val stickerId: String
    ) : MessageContent()

    @Serializable data class SystemEvent(
        val eventType: SystemEventType,
        val metadata: Map<String, String> = emptyMap()
    ) : MessageContent()
}

@Serializable
data class ReplyPreview(
    val messageId: String,
    val senderAlias: String,
    val snippet: String,
    val contentType: String
)

@Serializable
data class DestructConfig(
    val mode: DestructMode,
    val triggerAt: Long?         = null,
    val countdownSeconds: Int?   = null,
    val isTriggered: Boolean     = false
)

enum class DestructMode { TIMED_AUTO, COUNTDOWN, ON_READ, MANUAL_REMOTE, PANIC_WIPE }
enum class MessageStatus { PENDING, SENT, DELIVERED, READ, FAILED, DESTROYED }
enum class SystemEventType {
    ROOM_CREATED, MEMBER_JOINED, MEMBER_LEFT,
    KEY_ROTATED, SELF_DESTRUCT_TRIGGERED, PANIC_WIPE_EXECUTED
}
