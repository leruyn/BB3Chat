package com.bb3.bb3chat.feature.messaging.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.bb3.bb3chat.core.crypto.AliasCodec
import com.bb3.bb3chat.core.crypto.SessionManager
import com.bb3.bb3chat.db.ChatRoom
import com.bb3.bb3chat.feature.messaging.domain.model.InboxRoom
import com.bb3.bb3chat.feature.messaging.domain.repository.MessageRepository
import com.bb3.bb3chat.feature.messaging.domain.repository.RoomRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class LocalRoomRepository(
    private val sessionManager: SessionManager,
    private val messageRepository: MessageRepository
) : RoomRepository {

    override fun observeActiveRooms(): Flow<List<InboxRoom>> = flow {
        if (!sessionManager.hasActiveSession) {
            emit(emptyList())
            return@flow
        }
        val db  = sessionManager.requireDatabase()
        val key = sessionManager.requireSessionKey()
        db.chatRoomQueries.selectAllActive()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toInboxRoom(key) } }
            .collect { emit(it) }
    }

    override suspend fun syncInboxFromRemote() {
        messageRepository.syncInboxFromRemote()
    }

    private fun ChatRoom.toInboxRoom(sessionKey: ByteArray): InboxRoom = InboxRoom(
        id            = id,
        alias         = AliasCodec.decrypt(encrypted_alias, sessionKey),
        avatarIndex   = avatar_index.toInt(),
        lastSnippet   = last_msg_snippet ?: "",
        lastTimestamp = last_msg_ts ?: 0L,
        unreadCount   = unread_count.toInt(),
        isPinned      = is_pinned == 1L,
        isMuted       = is_muted == 1L
    )
}
