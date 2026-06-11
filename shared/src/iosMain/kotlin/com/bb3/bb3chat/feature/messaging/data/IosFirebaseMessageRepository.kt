package com.bb3.bb3chat.feature.messaging.data

import com.bb3.bb3chat.core.bridge.AuthBridgeHolder
import com.bb3.bb3chat.core.crypto.AliasCodec
import com.bb3.bb3chat.core.crypto.CryptoManager
import com.bb3.bb3chat.core.crypto.SessionManager
import com.bb3.bb3chat.feature.messaging.domain.model.DestructConfig
import com.bb3.bb3chat.feature.messaging.domain.model.Message
import com.bb3.bb3chat.feature.messaging.domain.model.MessageContent
import com.bb3.bb3chat.feature.messaging.domain.model.MessageStatus
import com.bb3.bb3chat.feature.messaging.domain.repository.MessageRepository
import com.bb3.bb3chat.feature.messaging.domain.repository.RoomStatus
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.datetime.Clock
import platform.Foundation.NSUUID

class IosFirebaseMessageRepository(
    private val sessionManager: SessionManager
) : MessageRepository {

    override fun observeMessages(roomId: String): Flow<List<Message>> = callbackFlow {
        fun emitLocal() {
            runCatching { loadLocalMessages(roomId) }
                .onSuccess { trySend(it) }
        }

        if (sessionManager.hasActiveSession) emitLocal()

        val cancel = FirestoreBridgeHolder.observeMessages(roomId) { json ->
            if (!sessionManager.hasActiveSession) return@observeMessages
            val remote = MessageFirestoreJson.parseMessages(json)
            remote.forEach { cacheMessageLocally(it) }
            remote.maxByOrNull { it.sentAt }?.let { latest ->
                updateRoomPreviewFromMessage(latest.roomId, latest, bumpUnread = false)
            }
            emitLocal()
        }

        awaitClose { cancel() }
    }

    override suspend fun sendMessage(
        roomId: String,
        content: MessageContent,
        destructConfig: DestructConfig?
    ): String {
        val key         = sessionManager.requireRoomKey(roomId)
        val messageId   = newMessageId()
        val senderAlias = currentSenderAlias()
        val sentAt      = nowMs()
        val encrypted   = encryptContent(content, key)

        val pending = Message(
            id             = messageId,
            roomId         = roomId,
            senderAlias    = senderAlias,
            content        = encrypted,
            destructConfig = destructConfig,
            status         = MessageStatus.PENDING,
            sentAt         = sentAt
        )
        cacheMessageLocally(pending)

        val payload = MessageFirestoreJson.buildPayload(
            messageId, roomId, encrypted, destructConfig, senderAlias, sentAt
        )

        return try {
            awaitSend(roomId, messageId, payload)
            awaitUpdateRoomActivity(roomId, encrypted.typeName())
            cacheMessageLocally(pending.copy(status = MessageStatus.SENT))
            updateLocalRoomPreview(roomId, encrypted.typeName(), sentAt)
            messageId
        } catch (e: Exception) {
            cacheMessageLocally(pending.copy(status = MessageStatus.FAILED))
            throw e
        }
    }

    override suspend fun checkRoomStatus(roomId: String): RoomStatus =
        suspendCancellableCoroutine { cont ->
            FirestoreBridgeHolder.checkRoomStatus(
                roomId,
                onResult = { status ->
                    cont.resume(
                        when (status) {
                            "PENDING_DESTRUCT" -> RoomStatus.PENDING_DESTRUCT
                            "DESTROYED"        -> RoomStatus.DESTROYED
                            else               -> RoomStatus.ACTIVE
                        }
                    )
                },
                onError = { cont.resume(RoomStatus.ACTIVE) }
            )
        }

    override suspend fun destroyLocalRoom(roomId: String) {
        val db = sessionManager.requireDatabase()
        db.messageQueries.deleteByRoomId(roomId)
        db.chatRoomQueries.deactivateRoom(roomId)
    }

    override suspend fun ensureRoomJoined(roomId: String, displayAlias: String) {
        AuthBridgeHolder.ensureSignedIn()
        val uid = AuthBridgeHolder.getCurrentUid()
            ?: throw IllegalStateException("Firebase Auth chưa sẵn sàng")
        val key = sessionManager.requireSessionKey()
        val db  = sessionManager.requireDatabase()

        val roomJson = awaitGetRoomJson(roomId)
        if (!MessageFirestoreJson.roomExists(roomJson)) {
            awaitCreateRoom(roomId, uid)
        } else {
            val members = MessageFirestoreJson.parseRoomMembers(roomJson)
            if (uid !in members) awaitJoinRoom(roomId, uid)
        }

        val now = nowMs()
        val encryptedAlias = AliasCodec.encrypt(displayAlias, key)
        db.chatRoomQueries.insertRoom(
            id                 = roomId,
            encrypted_alias    = encryptedAlias,
            avatar_index       = ((roomId.hashCode() and Int.MAX_VALUE) % 8).toLong(),
            room_type          = "PRIVATE",
            member_aliases     = "[]",
            shared_key_ver     = 1L,
            key_rotated_at     = null,
            status             = "ACTIVE",
            safe_hours_json    = null,
            created_at         = now,
            last_activity_at   = now,
            unread_count       = 0L,
            is_muted           = 0L,
            is_pinned          = 0L,
            is_archived        = 0L,
            last_msg_snippet   = null,
            last_msg_type      = null,
            last_msg_ts        = null
        )
    }

    override suspend fun markMessageRead(roomId: String, messageId: String, alias: String) {
        val ts = nowMs()
        runCatching { awaitMarkRead(roomId, messageId, alias, ts) }
        patchLocalReadBy(messageId, alias, ts)
    }

    override suspend fun triggerSelfDestruct(messageId: String) {
        sessionManager.requireDatabase().messageQueries.markDestroyed(messageId)
    }

    override suspend fun deleteMessage(roomId: String, messageId: String) {
        runCatching { awaitDeleteMessage(roomId, messageId) }
        sessionManager.requireDatabase().messageQueries.markDestroyed(messageId)
    }

    override suspend fun syncInboxFromRemote() {
        if (!sessionManager.hasActiveSession) return
        val db      = sessionManager.requireDatabase()
        val myAlias = currentSenderAlias()
        val rooms   = db.chatRoomQueries.selectAllActive().executeAsList()

        for (room in rooms) {
            when (checkRoomStatus(room.id)) {
                RoomStatus.PENDING_DESTRUCT, RoomStatus.DESTROYED -> {
                    destroyLocalRoom(room.id)
                    continue
                }
                else -> Unit
            }
            runCatching {
                syncRoomMessages(
                    roomId      = room.id,
                    lastKnownTs = room.last_msg_ts ?: 0L,
                    myAlias     = myAlias
                )
            }
        }
    }

    private suspend fun syncRoomMessages(roomId: String, lastKnownTs: Long, myAlias: String) {
        val json = awaitFetchRecent(roomId, 20)
        val messages = MessageFirestoreJson.parseMessages(json)
        messages.forEach { cacheMessageLocally(it) }
        val latest = messages.maxByOrNull { it.sentAt } ?: return
        val bumpUnread = latest.senderAlias != myAlias && latest.sentAt > lastKnownTs
        updateRoomPreviewFromMessage(roomId, latest, bumpUnread)
    }

    private fun updateRoomPreviewFromMessage(roomId: String, msg: Message, bumpUnread: Boolean) {
        runCatching {
            sessionManager.requireDatabase().chatRoomQueries.updateLastMessageFromSync(
                snippet          = previewSnippet(msg.content.typeName()),
                content_type     = msg.content.typeName(),
                ts               = msg.sentAt,
                increment_unread = if (bumpUnread) 1L else 0L,
                room_id          = roomId
            )
        }
    }

    private fun loadLocalMessages(roomId: String): List<Message> =
        sessionManager.requireDatabase().messageQueries.selectByRoom(roomId)
            .executeAsList()
            .map { MessageLocalMapper.toDomain(it) }

    private fun patchLocalReadBy(messageId: String, alias: String, timestamp: Long) {
        runCatching {
            val db  = sessionManager.requireDatabase()
            val row = db.messageQueries.selectById(messageId).executeAsOneOrNull() ?: return
            val updated = MessageLocalMapper.parseReadBy(row.read_by_json).toMutableMap()
            updated[alias] = timestamp
            db.messageQueries.markRead(
                read_by_json = MessageLocalMapper.readByToJson(updated),
                id = messageId
            )
        }
    }

    private fun updateLocalRoomPreview(roomId: String, contentType: String, ts: Long) {
        runCatching {
            sessionManager.requireDatabase().chatRoomQueries.updateLastMessageOutgoing(
                snippet      = previewSnippet(contentType),
                content_type = contentType,
                ts           = ts,
                room_id      = roomId
            )
        }
    }

    private fun previewSnippet(contentType: String): String = when (contentType) {
        "TEXT"  -> "Tin nhắn mới"
        "IMAGE" -> "📷 Ảnh"
        "VOICE" -> "🎙 Voice"
        else    -> "Tin nhắn"
    }

    private fun currentSenderAlias(): String =
        runCatching {
            sessionManager.requireDatabase()
                .appConfigQueries.selectByKey("user_alias")
                .executeAsOneOrNull()
        }.getOrNull() ?: "unknown"

    @OptIn(ExperimentalEncodingApi::class)
    private fun encryptContent(content: MessageContent, key: ByteArray): MessageContent =
        when (content) {
            is MessageContent.Text -> {
                if (content.iv.isEmpty()) {
                    val result = CryptoManager.encrypt(
                        content.encryptedBody.encodeToByteArray(), key
                    )
                    MessageContent.Text(
                        encryptedBody = result.cipherBytes.toBase64(),
                        iv            = result.iv.toBase64()
                    )
                } else content
            }
            else -> content
        }

    private fun cacheMessageLocally(msg: Message) {
        runCatching {
            val db = sessionManager.requireDatabase()
            val c  = msg.content
            db.messageQueries.insertMessage(
                id                  = msg.id,
                room_id             = msg.roomId,
                sender_alias        = msg.senderAlias,
                sender_avatar_index = msg.senderAvatarIndex.toLong(),
                content_type        = c.typeName(),
                encrypted_body      = (c as? MessageContent.Text)?.encryptedBody,
                encrypted_base64    = when (c) {
                    is MessageContent.Image -> c.encryptedBase64
                    is MessageContent.Voice -> c.encryptedBase64
                    else -> null
                },
                iv = when (c) {
                    is MessageContent.Text  -> c.iv
                    is MessageContent.Image -> c.iv
                    is MessageContent.Voice -> c.iv
                    else -> null
                },
                mime_type        = (c as? MessageContent.Image)?.mimeType,
                media_width      = (c as? MessageContent.Image)?.originalWidth?.toLong(),
                media_height     = (c as? MessageContent.Image)?.originalHeight?.toLong(),
                media_size_bytes = (c as? MessageContent.Image)?.compressedBytes?.toLong(),
                duration_ms      = (c as? MessageContent.Voice)?.durationMs,
                blur_hash        = (c as? MessageContent.Image)?.blurHash,
                waveform_json    = null,
                total_chunks     = null,
                chunk_collection_id = null,
                file_name        = null,
                pack_id          = null,
                sticker_id       = null,
                system_event_type = (c as? MessageContent.SystemEvent)?.eventType?.name,
                system_meta_json = null,
                reply_to_id      = msg.replyToId,
                reply_preview_json = null,
                reactions_json   = "{}",
                destruct_mode    = msg.destructConfig?.mode?.name,
                destruct_trigger_at = msg.destructConfig?.triggerAt,
                destruct_countdown_s = msg.destructConfig?.countdownSeconds?.toLong(),
                is_destruct_triggered = if (msg.destructConfig?.isTriggered == true) 1L else 0L,
                status           = msg.status.name,
                read_by_json     = MessageLocalMapper.readByToJson(msg.readBy),
                sent_at          = msg.sentAt,
                server_at        = msg.serverAt,
                is_edited        = if (msg.isEdited) 1L else 0L,
                edited_at        = msg.editedAt,
                is_starred       = if (msg.isStarred) 1L else 0L,
                is_pinned        = if (msg.isPinned) 1L else 0L,
                is_forwarded     = if (msg.isForwarded) 1L else 0L,
                forwarded_from_room = msg.forwardedFromRoom
            )
        }
    }

    private suspend fun awaitSend(roomId: String, messageId: String, payload: String) =
        suspendCancellableCoroutine { cont ->
            FirestoreBridgeHolder.sendMessage(
                roomId, messageId, payload,
                onSuccess = { cont.resume(Unit) },
                onError = { cont.resumeWithException(IllegalStateException(it)) }
            )
        }

    private suspend fun awaitUpdateRoomActivity(roomId: String, contentType: String) =
        suspendCancellableCoroutine { cont ->
            FirestoreBridgeHolder.updateRoomActivity(
                roomId, contentType,
                onSuccess = { cont.resume(Unit) },
                onError = { cont.resumeWithException(IllegalStateException(it)) }
            )
        }

    private suspend fun awaitGetRoomJson(roomId: String): String =
        suspendCancellableCoroutine { cont ->
            FirestoreBridgeHolder.getRoomJson(
                roomId,
                onResult = { cont.resume(it) },
                onError = { cont.resumeWithException(IllegalStateException(it)) }
            )
        }

    private suspend fun awaitCreateRoom(roomId: String, uid: String) =
        suspendCancellableCoroutine { cont ->
            FirestoreBridgeHolder.createRoom(
                roomId, uid,
                onSuccess = { cont.resume(Unit) },
                onError = { cont.resumeWithException(IllegalStateException(it)) }
            )
        }

    private suspend fun awaitJoinRoom(roomId: String, uid: String) =
        suspendCancellableCoroutine { cont ->
            FirestoreBridgeHolder.joinRoom(
                roomId, uid,
                onSuccess = { cont.resume(Unit) },
                onError = { cont.resumeWithException(IllegalStateException(it)) }
            )
        }

    private suspend fun awaitMarkRead(
        roomId: String,
        messageId: String,
        alias: String,
        ts: Long
    ) = suspendCancellableCoroutine { cont ->
        FirestoreBridgeHolder.markMessageRead(
            roomId, messageId, alias, ts,
            onDone = { cont.resume(Unit) },
            onError = { cont.resumeWithException(IllegalStateException(it)) }
        )
    }

    private suspend fun awaitDeleteMessage(roomId: String, messageId: String) =
        suspendCancellableCoroutine { cont ->
            FirestoreBridgeHolder.deleteMessage(
                roomId, messageId,
                onDone = { cont.resume(Unit) },
                onError = { cont.resumeWithException(IllegalStateException(it)) }
            )
        }

    private suspend fun awaitFetchRecent(roomId: String, limit: Int): String =
        suspendCancellableCoroutine { cont ->
            FirestoreBridgeHolder.fetchRecentMessages(
                roomId, limit,
                onResult = { cont.resume(it) },
                onError = { cont.resumeWithException(IllegalStateException(it)) }
            )
        }

    @OptIn(ExperimentalEncodingApi::class)
    private fun ByteArray.toBase64(): String = Base64.Default.encode(this)

    private fun MessageContent.typeName() = when (this) {
        is MessageContent.Text        -> "TEXT"
        is MessageContent.Image       -> "IMAGE"
        is MessageContent.Voice       -> "VOICE"
        is MessageContent.ChunkedFile -> "CHUNKED_FILE"
        is MessageContent.Sticker     -> "STICKER"
        is MessageContent.SystemEvent -> "SYSTEM"
    }

    private fun newMessageId(): String = NSUUID().UUIDString().lowercase()

    private fun nowMs(): Long = Clock.System.now().toEpochMilliseconds()
}
