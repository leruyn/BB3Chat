package com.bb3.bb3chat.feature.messaging.data

import com.bb3.bb3chat.feature.messaging.domain.model.DestructConfig
import com.bb3.bb3chat.feature.messaging.domain.model.DestructMode
import com.bb3.bb3chat.feature.messaging.domain.model.Message
import com.bb3.bb3chat.feature.messaging.domain.model.MessageContent
import com.bb3.bb3chat.feature.messaging.domain.model.MessageStatus
import com.bb3.bb3chat.feature.messaging.domain.model.SystemEventType
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

internal object MessageFirestoreJson {

    const val SERVER_TIMESTAMP = "__SERVER_TIMESTAMP__"

    fun parseMessages(json: String): List<Message> {
        if (json.isBlank() || json == "[]") return emptyList()
        return runCatching {
            kotlinx.serialization.json.Json.parseToJsonElement(json).jsonArray
                .mapNotNull { parseMessage(it.jsonObject) }
        }.getOrDefault(emptyList())
    }

    fun parseRoomMembers(json: String): List<String> {
        if (json.isBlank() || json == "null") return emptyList()
        return runCatching {
            val obj = kotlinx.serialization.json.Json.parseToJsonElement(json).jsonObject
            obj["memberUids"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
        }.getOrDefault(emptyList())
    }

    fun roomExists(json: String): Boolean =
        json.isNotBlank() && json != "null"

    fun buildPayload(
        id: String,
        roomId: String,
        content: MessageContent,
        destructConfig: DestructConfig?,
        senderAlias: String,
        senderUid: String,
        sentAt: Long
    ): String = buildJsonObject {
        put("id", id)
        put("roomId", roomId)
        put("senderAlias", senderAlias)
        put("senderUid", senderUid)
        put("contentType", content.typeName())
        put("sentAt", sentAt)
        put("serverAt", SERVER_TIMESTAMP)
        put("status", "SENT")
        destructConfig?.let { cfg ->
            put("destructMode", cfg.mode.name)
            put("destructTriggerAt", cfg.triggerAt)
            cfg.countdownSeconds?.let { put("destructCountdown", it) }
            put("isDestructTriggered", false)
        }
        when (content) {
            is MessageContent.Text -> {
                put("encryptedBody", content.encryptedBody)
                put("iv", content.iv)
            }
            is MessageContent.Image -> {
                put("encryptedBase64", content.encryptedBase64)
                put("iv", content.iv)
                put("mimeType", content.mimeType)
                put("originalWidth", content.originalWidth)
                put("originalHeight", content.originalHeight)
                put("compressedBytes", content.compressedBytes)
                put("blurHash", content.blurHash)
            }
            is MessageContent.Voice -> {
                put("encryptedBase64", content.encryptedBase64)
                put("iv", content.iv)
                put("durationMs", content.durationMs)
                put("waveformData", buildJsonArray {
                    content.waveformData.forEach { add(JsonPrimitive(it)) }
                })
            }
            is MessageContent.ChunkedFile -> {
                put("totalChunks", content.totalChunks)
                put("mimeType", content.mimeType)
                put("fileName", content.fileName)
                put("totalSizeBytes", content.totalSizeBytes)
                put("chunkCollectionId", content.chunkCollectionId)
            }
            is MessageContent.Sticker -> {
                put("encryptedBase64", content.encryptedBase64)
                put("iv", content.iv)
                put("packId", content.packId)
                put("stickerId", content.stickerId)
            }
            is MessageContent.SystemEvent -> {
                put("systemEventType", content.eventType.name)
                put("systemMeta", buildJsonObject {
                    content.metadata.forEach { (k, v) -> put(k, v) }
                })
            }
        }
    }.toString()

    private fun parseMessage(obj: JsonObject): Message? = runCatching {
        val id = obj.string("id") ?: return null
        val contentType = obj.string("contentType") ?: "TEXT"
        val content = when (contentType) {
            "TEXT" -> MessageContent.Text(
                encryptedBody = obj.string("encryptedBody") ?: "",
                iv            = obj.string("iv") ?: ""
            )
            "IMAGE" -> MessageContent.Image(
                encryptedBase64 = obj.string("encryptedBase64") ?: "",
                iv              = obj.string("iv") ?: "",
                mimeType        = obj.string("mimeType") ?: "image/jpeg",
                originalWidth   = obj.long("originalWidth")?.toInt() ?: 0,
                originalHeight  = obj.long("originalHeight")?.toInt() ?: 0,
                compressedBytes = obj.long("compressedBytes")?.toInt() ?: 0,
                blurHash        = obj.string("blurHash") ?: ""
            )
            "VOICE" -> MessageContent.Voice(
                encryptedBase64 = obj.string("encryptedBase64") ?: "",
                iv              = obj.string("iv") ?: "",
                durationMs      = obj.long("durationMs") ?: 0L,
                waveformData    = obj["waveformData"]?.jsonArray
                    ?.mapNotNull { it.jsonPrimitive.content.toFloatOrNull() } ?: emptyList()
            )
            else -> MessageContent.SystemEvent(SystemEventType.ROOM_CREATED)
        }
        val destructMode = obj.string("destructMode")
        val destructConfig = destructMode?.let { mode ->
            DestructConfig(
                mode             = runCatching { DestructMode.valueOf(mode) }
                    .getOrDefault(DestructMode.COUNTDOWN),
                triggerAt        = obj.long("destructTriggerAt"),
                countdownSeconds = obj.long("destructCountdown")?.toInt(),
                isTriggered      = obj.bool("isDestructTriggered") ?: false
            )
        }
        Message(
            id                = id,
            roomId            = obj.string("roomId") ?: "",
            senderAlias       = obj.string("senderAlias") ?: "unknown",
            senderAvatarIndex = obj.long("senderAvatarIndex")?.toInt() ?: 0,
            content           = content,
            destructConfig    = destructConfig,
            status            = runCatching {
                MessageStatus.valueOf(obj.string("status") ?: "SENT")
            }.getOrDefault(MessageStatus.SENT),
            readBy            = parseReadBy(obj["readBy"]),
            sentAt            = obj.long("sentAt") ?: 0L,
            serverAt          = obj.long("serverAt")
        )
    }.getOrNull()

    private fun parseReadBy(element: JsonElement?): Map<String, Long> {
        val obj = element?.jsonObject ?: return emptyMap()
        return obj.mapNotNull { (alias, value) ->
            value.longValue()?.let { alias to it }
        }.toMap()
    }

    private fun JsonObject.string(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull()

    private fun JsonObject.long(key: String): Long? =
        this[key]?.longValue()

    private fun JsonObject.bool(key: String): Boolean? =
        this[key]?.jsonPrimitive?.content?.toBooleanStrictOrNull()

    private fun JsonElement.longValue(): Long? = when (this) {
        is JsonPrimitive -> content.toLongOrNull()
            ?: content.toDoubleOrNull()?.toLong()
        else -> null
    }

    private fun JsonPrimitive.contentOrNull(): String? =
        if (this is JsonNull) null else content

    private fun MessageContent.typeName() = when (this) {
        is MessageContent.Text        -> "TEXT"
        is MessageContent.Image       -> "IMAGE"
        is MessageContent.Voice       -> "VOICE"
        is MessageContent.ChunkedFile -> "CHUNKED_FILE"
        is MessageContent.Sticker     -> "STICKER"
        is MessageContent.SystemEvent -> "SYSTEM"
    }
}
