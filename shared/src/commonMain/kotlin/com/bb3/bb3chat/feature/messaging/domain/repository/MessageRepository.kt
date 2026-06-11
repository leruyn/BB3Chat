package com.bb3.bb3chat.feature.messaging.domain.repository

import com.bb3.bb3chat.feature.messaging.domain.model.DestructConfig
import com.bb3.bb3chat.feature.messaging.domain.model.Message
import com.bb3.bb3chat.feature.messaging.domain.model.MessageContent
import kotlinx.coroutines.flow.Flow

enum class RoomStatus { ACTIVE, PENDING_DESTRUCT, DESTROYED }

interface MessageRepository {
    fun observeMessages(roomId: String): Flow<List<Message>>
    suspend fun sendMessage(
        roomId: String,
        content: MessageContent,
        destructConfig: DestructConfig? = null
    ): String
    suspend fun checkRoomStatus(roomId: String): RoomStatus
    suspend fun destroyLocalRoom(roomId: String)
    suspend fun ensureRoomJoined(roomId: String, displayAlias: String)
    suspend fun markMessageRead(roomId: String, messageId: String, alias: String)
    suspend fun triggerSelfDestruct(messageId: String)
    suspend fun deleteMessage(roomId: String, messageId: String)
    suspend fun syncInboxFromRemote()
}
