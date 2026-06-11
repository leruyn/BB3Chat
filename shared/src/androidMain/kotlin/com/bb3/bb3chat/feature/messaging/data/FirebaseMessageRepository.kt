package com.bb3.bb3chat.feature.messaging.data

import com.bb3.bb3chat.core.crypto.AliasCodec
import com.bb3.bb3chat.core.crypto.CryptoManager
import com.bb3.bb3chat.core.crypto.SessionManager
import com.bb3.bb3chat.feature.messaging.domain.model.DestructConfig
import com.bb3.bb3chat.feature.messaging.domain.model.DestructMode
import com.bb3.bb3chat.feature.messaging.domain.model.Message
import com.bb3.bb3chat.feature.messaging.domain.model.MessageContent
import com.bb3.bb3chat.feature.messaging.domain.model.MessageStatus
import com.bb3.bb3chat.feature.messaging.domain.repository.MessageRepository
import com.bb3.bb3chat.feature.messaging.domain.repository.RoomStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Base64
import java.util.UUID

class FirebaseMessageRepository(
    private val firestore: FirebaseFirestore,
    private val sessionManager: SessionManager
) : MessageRepository {

    override fun observeMessages(roomId: String): Flow<List<Message>> = callbackFlow {
        fun emitLocal() {
            runCatching { loadLocalMessages(roomId) }
                .onSuccess { trySend(it) }
        }

        // Offline-first: show cached messages immediately
        if (sessionManager.hasActiveSession) {
            emitLocal()
        }

        val listener = firestore
            .collection("rooms/$roomId/messages")
            .orderBy("sentAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (sessionManager.hasActiveSession.not()) return@addSnapshotListener

                if (error != null) {
                    // Network down — keep showing local cache, don't tear down the flow
                    emitLocal()
                    return@addSnapshotListener
                }

                val remote = snapshot?.documents?.mapNotNull { doc ->
                    runCatching { doc.toMessage() }.getOrNull()
                } ?: emptyList()

                remote.forEach { cacheMessageLocally(it) }
                remote.maxByOrNull { it.sentAt }?.let { latest ->
                    updateRoomPreviewFromMessage(latest.roomId, latest, bumpUnread = false)
                }
                emitLocal()
            }

        awaitClose { listener.remove() }
    }

    override suspend fun sendMessage(
        roomId: String,
        content: MessageContent,
        destructConfig: DestructConfig?
    ): String {
        val key         = sessionManager.requireRoomKey(roomId)
        val messageId   = UUID.randomUUID().toString()
        val senderAlias = currentSenderAlias()
        val sentAt      = System.currentTimeMillis()
        val encrypted   = encryptContent(content, key)

        val pending = Message(
            id          = messageId,
            roomId      = roomId,
            senderAlias = senderAlias,
            content     = encrypted,
            destructConfig = destructConfig,
            status      = MessageStatus.PENDING,
            sentAt      = sentAt
        )
        cacheMessageLocally(pending)

        val senderUid = FirebaseAuth.getInstance().currentUser?.uid
            ?: throw IllegalStateException("Firebase Auth chưa sẵn sàng")
        val payload = buildFirestorePayload(
            messageId, roomId, encrypted, destructConfig, senderAlias, senderUid, sentAt
        )

        return try {
            firestore.collection("rooms/$roomId/messages")
                .document(messageId)
                .set(payload)
                .await()

            firestore.collection("rooms").document(roomId)
                .update(
                    mapOf(
                        "lastActivityAt" to com.google.firebase.Timestamp.now(),
                        "lastMsgType"    to encrypted.typeName()
                    )
                ).await()

            cacheMessageLocally(pending.copy(status = MessageStatus.SENT))
            updateLocalRoomPreview(roomId, encrypted.typeName(), sentAt)
            messageId
        } catch (e: Exception) {
            cacheMessageLocally(pending.copy(status = MessageStatus.FAILED))
            throw e
        }
    }

    override suspend fun checkRoomStatus(roomId: String): RoomStatus {
        return runCatching {
            val doc = firestore.collection("rooms").document(roomId).get().await()
            when (doc.getString("status")) {
                "PENDING_DESTRUCT" -> RoomStatus.PENDING_DESTRUCT
                "DESTROYED"        -> RoomStatus.DESTROYED
                else               -> RoomStatus.ACTIVE
            }
        }.getOrDefault(RoomStatus.ACTIVE)
    }

    override suspend fun destroyLocalRoom(roomId: String) {
        val db = sessionManager.requireDatabase()
        db.messageQueries.deleteByRoomId(roomId)
        db.chatRoomQueries.deactivateRoom(roomId)
    }

    override suspend fun ensureRoomJoined(roomId: String, displayAlias: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
            ?: throw IllegalStateException("Firebase Auth chưa sẵn sàng")
        val key = sessionManager.requireSessionKey()
        val db  = sessionManager.requireDatabase()
        val roomRef = firestore.collection("rooms").document(roomId)
        val snapshot = roomRef.get().await()

        if (!snapshot.exists()) {
            roomRef.set(
                mapOf(
                    "creatorUid"     to uid,
                    "memberUids"     to listOf(uid),
                    "status"         to "ACTIVE",
                    "createdAt"      to com.google.firebase.Timestamp.now(),
                    "lastActivityAt" to com.google.firebase.Timestamp.now()
                )
            ).await()
        } else {
            @Suppress("UNCHECKED_CAST")
            val members = snapshot.get("memberUids") as? List<String> ?: emptyList()
            if (uid !in members) {
                roomRef.update(
                    "memberUids",
                    com.google.firebase.firestore.FieldValue.arrayUnion(uid)
                ).await()
            }
        }

        val now = System.currentTimeMillis()
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
        val now = System.currentTimeMillis()
        runCatching {
            firestore.collection("rooms/$roomId/messages").document(messageId)
                .update("readBy.$alias", now)
                .await()
        }
        patchLocalReadBy(messageId, alias, now)
    }

    override suspend fun triggerSelfDestruct(messageId: String) {
        sessionManager.requireDatabase().messageQueries.markDestroyed(messageId)
    }

    override suspend fun deleteMessage(roomId: String, messageId: String) {
        runCatching {
            firestore.collection("rooms/$roomId/messages").document(messageId).delete().await()
        }
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
                else -> {}
            }
            runCatching {
                syncRoomMessages(
                    roomId       = room.id,
                    lastKnownTs  = room.last_msg_ts ?: 0L,
                    myAlias      = myAlias
                )
            }
        }
    }

    private suspend fun syncRoomMessages(roomId: String, lastKnownTs: Long, myAlias: String) {
        val snapshot = firestore.collection("rooms/$roomId/messages")
            .orderBy("sentAt", Query.Direction.DESCENDING)
            .limit(20)
            .get()
            .await()

        val messages = snapshot.documents.mapNotNull { doc ->
            runCatching { doc.toMessage() }.getOrNull()
        }
        messages.forEach { cacheMessageLocally(it) }

        val latest = messages.maxByOrNull { it.sentAt } ?: return
        val bumpUnread = latest.senderAlias != myAlias && latest.sentAt > lastKnownTs
        updateRoomPreviewFromMessage(roomId, latest, bumpUnread)
    }

    private fun updateRoomPreviewFromMessage(roomId: String, msg: Message, bumpUnread: Boolean) {
        runCatching {
            val contentType = msg.content.typeName()
            sessionManager.requireDatabase().chatRoomQueries.updateLastMessageFromSync(
                snippet          = previewSnippet(contentType),
                content_type     = contentType,
                ts               = msg.sentAt,
                increment_unread = if (bumpUnread) 1L else 0L,
                room_id          = roomId
            )
        }
    }

    private fun loadLocalMessages(roomId: String): List<Message> {
        val db = sessionManager.requireDatabase()
        return db.messageQueries.selectByRoom(roomId)
            .executeAsList()
            .map { MessageLocalMapper.toDomain(it) }
    }

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

    private fun encryptContent(content: MessageContent, key: ByteArray): MessageContent =
        when (content) {
            is MessageContent.Text -> {
                if (content.iv.isEmpty()) {
                    val result = CryptoManager.encrypt(
                        content.encryptedBody.toByteArray(Charsets.UTF_8), key
                    )
                    MessageContent.Text(
                        encryptedBody = result.cipherBytes.toBase64(),
                        iv            = result.iv.toBase64()
                    )
                } else {
                    content
                }
            }
            else -> content
        }

    private fun buildFirestorePayload(
        id: String,
        roomId: String,
        content: MessageContent,
        destructConfig: DestructConfig?,
        senderAlias: String,
        senderUid: String,
        sentAt: Long
    ): Map<String, Any?> {
        val base = mutableMapOf<String, Any?>(
            "id"          to id,
            "roomId"      to roomId,
            "senderAlias" to senderAlias,
            "senderUid"   to senderUid,
            "contentType" to content.typeName(),
            "sentAt"      to com.google.firebase.Timestamp(sentAt / 1000, ((sentAt % 1000) * 1_000_000).toInt()),
            "serverAt"    to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            "status"      to "SENT"
        )

        destructConfig?.let {
            base["destructMode"]        = it.mode.name
            base["destructTriggerAt"]   = it.triggerAt
            base["destructCountdown"]   = it.countdownSeconds
            base["isDestructTriggered"] = false
        }

        when (content) {
            is MessageContent.Text -> {
                base["encryptedBody"] = content.encryptedBody
                base["iv"]            = content.iv
            }
            is MessageContent.Image -> {
                base["encryptedBase64"]  = content.encryptedBase64
                base["iv"]               = content.iv
                base["mimeType"]         = content.mimeType
                base["originalWidth"]    = content.originalWidth
                base["originalHeight"]   = content.originalHeight
                base["compressedBytes"]  = content.compressedBytes
                base["blurHash"]         = content.blurHash
            }
            is MessageContent.Voice -> {
                base["encryptedBase64"] = content.encryptedBase64
                base["iv"]              = content.iv
                base["durationMs"]      = content.durationMs
                base["waveformData"]    = content.waveformData
            }
            is MessageContent.ChunkedFile -> {
                base["totalChunks"]       = content.totalChunks
                base["mimeType"]          = content.mimeType
                base["fileName"]          = content.fileName
                base["totalSizeBytes"]    = content.totalSizeBytes
                base["chunkCollectionId"] = content.chunkCollectionId
            }
            is MessageContent.Sticker -> {
                base["encryptedBase64"] = content.encryptedBase64
                base["iv"]              = content.iv
                base["packId"]          = content.packId
                base["stickerId"]       = content.stickerId
            }
            is MessageContent.SystemEvent -> {
                base["systemEventType"] = content.eventType.name
                base["systemMeta"]      = content.metadata
            }
        }
        return base
    }

    private fun DocumentSnapshot.toMessage(): Message {
        val contentType = getString("contentType") ?: "TEXT"
        val content: MessageContent = when (contentType) {
            "TEXT" -> MessageContent.Text(
                encryptedBody = getString("encryptedBody") ?: "",
                iv            = getString("iv") ?: ""
            )
            "IMAGE" -> MessageContent.Image(
                encryptedBase64 = getString("encryptedBase64") ?: "",
                iv              = getString("iv") ?: "",
                mimeType        = getString("mimeType") ?: "image/jpeg",
                originalWidth   = getLong("originalWidth")?.toInt() ?: 0,
                originalHeight  = getLong("originalHeight")?.toInt() ?: 0,
                compressedBytes = getLong("compressedBytes")?.toInt() ?: 0,
                blurHash        = getString("blurHash") ?: ""
            )
            "VOICE" -> MessageContent.Voice(
                encryptedBase64 = getString("encryptedBase64") ?: "",
                iv              = getString("iv") ?: "",
                durationMs      = getLong("durationMs") ?: 0L,
                waveformData    = (get("waveformData") as? List<*>)
                    ?.filterIsInstance<Number>()?.map { it.toFloat() } ?: emptyList()
            )
            else -> MessageContent.SystemEvent(
                com.bb3.bb3chat.feature.messaging.domain.model.SystemEventType.ROOM_CREATED
            )
        }

        val destructMode = getString("destructMode")
        val destructConfig = destructMode?.let { mode ->
            DestructConfig(
                mode             = runCatching { DestructMode.valueOf(mode) }
                    .getOrDefault(DestructMode.COUNTDOWN),
                triggerAt        = getLong("destructTriggerAt"),
                countdownSeconds = getLong("destructCountdown")?.toInt(),
                isTriggered      = getBoolean("isDestructTriggered") ?: false
            )
        }

        return Message(
            id                = id,
            roomId            = getString("roomId") ?: "",
            senderAlias       = getString("senderAlias") ?: "unknown",
            senderAvatarIndex = getLong("senderAvatarIndex")?.toInt() ?: 0,
            content           = content,
            destructConfig    = destructConfig,
            status            = runCatching { MessageStatus.valueOf(getString("status") ?: "SENT") }
                .getOrDefault(MessageStatus.SENT),
            readBy            = parseReadBy(),
            sentAt            = getTimestamp("sentAt")?.toDate()?.time ?: 0L,
            serverAt          = getTimestamp("serverAt")?.toDate()?.time
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun DocumentSnapshot.parseReadBy(): Map<String, Long> {
        val raw = get("readBy") as? Map<String, Any?> ?: return emptyMap()
        return raw.mapNotNull { (alias, value) ->
            val ts = when (value) {
                is Number                        -> value.toLong()
                is com.google.firebase.Timestamp -> value.toDate().time
                else                             -> null
            }
            ts?.let { alias to it }
        }.toMap()
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
                    else                    -> null
                },
                iv                  = when (c) {
                    is MessageContent.Text  -> c.iv
                    is MessageContent.Image -> c.iv
                    is MessageContent.Voice -> c.iv
                    else                    -> null
                },
                mime_type           = (c as? MessageContent.Image)?.mimeType,
                media_width         = (c as? MessageContent.Image)?.originalWidth?.toLong(),
                media_height        = (c as? MessageContent.Image)?.originalHeight?.toLong(),
                media_size_bytes    = (c as? MessageContent.Image)?.compressedBytes?.toLong(),
                duration_ms         = (c as? MessageContent.Voice)?.durationMs,
                blur_hash           = (c as? MessageContent.Image)?.blurHash,
                waveform_json       = null,
                total_chunks        = null,
                chunk_collection_id = null,
                file_name           = null,
                pack_id             = null,
                sticker_id          = null,
                system_event_type   = (c as? MessageContent.SystemEvent)?.eventType?.name,
                system_meta_json    = null,
                reply_to_id         = msg.replyToId,
                reply_preview_json  = null,
                reactions_json      = "{}",
                destruct_mode       = msg.destructConfig?.mode?.name,
                destruct_trigger_at = msg.destructConfig?.triggerAt,
                destruct_countdown_s = msg.destructConfig?.countdownSeconds?.toLong(),
                is_destruct_triggered = if (msg.destructConfig?.isTriggered == true) 1L else 0L,
                status              = msg.status.name,
                read_by_json        = MessageLocalMapper.readByToJson(msg.readBy),
                sent_at             = msg.sentAt,
                server_at           = msg.serverAt,
                is_edited           = if (msg.isEdited) 1L else 0L,
                edited_at           = msg.editedAt,
                is_starred          = if (msg.isStarred) 1L else 0L,
                is_pinned           = if (msg.isPinned) 1L else 0L,
                is_forwarded        = if (msg.isForwarded) 1L else 0L,
                forwarded_from_room = msg.forwardedFromRoom
            )
        }
    }

    private fun MessageContent.typeName() = when (this) {
        is MessageContent.Text        -> "TEXT"
        is MessageContent.Image       -> "IMAGE"
        is MessageContent.Voice       -> "VOICE"
        is MessageContent.ChunkedFile -> "CHUNKED_FILE"
        is MessageContent.Sticker     -> "STICKER"
        is MessageContent.SystemEvent -> "SYSTEM"
    }

    private fun ByteArray.toBase64(): String = Base64.getEncoder().encodeToString(this)
}
