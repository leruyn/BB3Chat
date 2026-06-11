package com.bb3.bb3chat.messaging

import com.bb3.bb3chat.feature.messaging.domain.model.DestructConfig
import com.bb3.bb3chat.feature.messaging.domain.model.Message
import com.bb3.bb3chat.feature.messaging.domain.model.MessageContent
import com.bb3.bb3chat.feature.messaging.domain.repository.MessageRepository
import com.bb3.bb3chat.feature.messaging.domain.repository.RoomStatus
import com.bb3.bb3chat.feature.messaging.domain.usecase.SendMessageUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SendMessageUseCaseTest {

    private var capturedContent: MessageContent? = null

    private val fakeRepo = object : MessageRepository {
        override fun observeMessages(roomId: String): Flow<List<Message>> = emptyFlow()
        override suspend fun sendMessage(
            roomId: String,
            content: MessageContent,
            destructConfig: DestructConfig?
        ): String {
            capturedContent = content
            return "msg-id"
        }
        override suspend fun checkRoomStatus(roomId: String): RoomStatus = RoomStatus.ACTIVE
        override suspend fun destroyLocalRoom(roomId: String) {}
        override suspend fun ensureRoomJoined(roomId: String, displayAlias: String) {}
        override suspend fun markMessageRead(roomId: String, messageId: String, alias: String) {}
        override suspend fun triggerSelfDestruct(messageId: String) {}
        override suspend fun deleteMessage(roomId: String, messageId: String) {}
        override suspend fun syncInboxFromRemote() {}
    }

    @Test
    fun `passes plaintext to repository without pre-encrypting`() = runTest {
        val useCase = SendMessageUseCase(fakeRepo)
        useCase(roomId = "room1", plainText = "hello")

        val content = capturedContent
        assertTrue(content is MessageContent.Text)
        assertEquals("hello", content.encryptedBody)
        assertEquals("", content.iv)
    }
}
