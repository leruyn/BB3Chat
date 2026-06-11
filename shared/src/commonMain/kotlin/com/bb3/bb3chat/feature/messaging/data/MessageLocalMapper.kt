package com.bb3.bb3chat.feature.messaging.data

import com.bb3.bb3chat.db.Message as DbMessage
import com.bb3.bb3chat.feature.messaging.domain.model.DestructConfig
import com.bb3.bb3chat.feature.messaging.domain.model.DestructMode
import com.bb3.bb3chat.feature.messaging.domain.model.Message
import com.bb3.bb3chat.feature.messaging.domain.model.MessageContent
import com.bb3.bb3chat.feature.messaging.domain.model.MessageStatus
import com.bb3.bb3chat.feature.messaging.domain.model.SystemEventType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object MessageLocalMapper {

    private val json = Json { ignoreUnknownKeys = true }

    fun toDomain(row: DbMessage): Message {
        val content = row.toContent()
        val destructConfig = row.destruct_mode?.let { mode ->
            DestructConfig(
                mode             = runCatching { DestructMode.valueOf(mode) }
                    .getOrDefault(DestructMode.COUNTDOWN),
                triggerAt        = row.destruct_trigger_at,
                countdownSeconds = row.destruct_countdown_s?.toInt(),
                isTriggered      = row.is_destruct_triggered == 1L
            )
        }
        return Message(
            id                = row.id,
            roomId            = row.room_id,
            senderAlias       = row.sender_alias,
            senderAvatarIndex = row.sender_avatar_index.toInt(),
            content           = content,
            replyToId         = row.reply_to_id,
            destructConfig    = destructConfig,
            status            = runCatching { MessageStatus.valueOf(row.status) }
                .getOrDefault(MessageStatus.SENT),
            readBy            = parseReadBy(row.read_by_json),
            sentAt            = row.sent_at,
            serverAt          = row.server_at,
            isEdited          = row.is_edited == 1L,
            editedAt          = row.edited_at,
            isStarred         = row.is_starred == 1L,
            isPinned          = row.is_pinned == 1L,
            isForwarded       = row.is_forwarded == 1L,
            forwardedFromRoom = row.forwarded_from_room
        )
    }

    fun readByToJson(readBy: Map<String, Long>): String =
        json.encodeToString(readBy)

    fun parseReadBy(raw: String): Map<String, Long> =
        runCatching {
            json.decodeFromString<Map<String, Long>>(raw)
        }.getOrDefault(emptyMap())

    private fun DbMessage.toContent(): MessageContent = when (content_type) {
        "TEXT" -> MessageContent.Text(
            encryptedBody = encrypted_body ?: "",
            iv            = iv ?: ""
        )
        "IMAGE" -> MessageContent.Image(
            encryptedBase64 = encrypted_base64 ?: "",
            iv              = iv ?: "",
            mimeType        = mime_type ?: "image/jpeg",
            originalWidth   = media_width?.toInt() ?: 0,
            originalHeight  = media_height?.toInt() ?: 0,
            compressedBytes = media_size_bytes?.toInt() ?: 0,
            blurHash        = blur_hash ?: ""
        )
        "VOICE" -> MessageContent.Voice(
            encryptedBase64 = encrypted_base64 ?: "",
            iv              = iv ?: "",
            durationMs      = duration_ms ?: 0L,
            waveformData    = emptyList()
        )
        "SYSTEM" -> MessageContent.SystemEvent(
            eventType = runCatching {
                SystemEventType.valueOf(system_event_type ?: "ROOM_CREATED")
            }.getOrDefault(SystemEventType.ROOM_CREATED)
        )
        else -> MessageContent.Text(encryptedBody = "", iv = "")
    }
}
