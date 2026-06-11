package com.bb3.bb3chat.messaging

import com.bb3.bb3chat.core.crypto.SessionManager
import com.bb3.bb3chat.testutil.NoopSqlDriver
import com.bb3.bb3chat.core.platform.ImageProcessor
import com.bb3.bb3chat.feature.messaging.domain.model.DestructConfig
import com.bb3.bb3chat.feature.messaging.domain.model.Message
import com.bb3.bb3chat.feature.messaging.domain.model.MessageContent
import com.bb3.bb3chat.feature.messaging.domain.repository.MessageRepository
import com.bb3.bb3chat.feature.messaging.domain.repository.RoomStatus
import com.bb3.bb3chat.feature.messaging.domain.usecase.SendImageUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

class SendImageUseCaseTest {

    // ── Fakes ─────────────────────────────────────────────────────────────────

    private var capturedContent: MessageContent? = null

    private val fakeRepo = object : MessageRepository {
        override fun observeMessages(roomId: String): Flow<List<Message>> = emptyFlow()
        override suspend fun sendMessage(
            roomId: String,
            content: MessageContent,
            destructConfig: DestructConfig?
        ): String {
            capturedContent = content
            return "fake-msg-id"
        }
        override suspend fun checkRoomStatus(roomId: String): RoomStatus = RoomStatus.ACTIVE
        override suspend fun destroyLocalRoom(roomId: String) {}
        override suspend fun ensureRoomJoined(roomId: String, displayAlias: String) {}
        override suspend fun markMessageRead(roomId: String, messageId: String, alias: String) {}
        override suspend fun triggerSelfDestruct(messageId: String) {}
        override suspend fun deleteMessage(roomId: String, messageId: String) {}
        override suspend fun syncInboxFromRemote() {}
    }

    private fun sessionWithKey(key: ByteArray): SessionManager =
        SessionManager().apply {
            openRealSession(key, NoopSqlDriver(), userId = "test-user")
            setRoomKey("room1", key)
        }

    private fun buildUseCase(sessionKey: ByteArray): SendImageUseCase =
        SendImageUseCase(fakeRepo, sessionWithKey(sessionKey), ImageProcessor())

    // ── Tests ──────────────────────────────────────────────────────────────────

    @Test
    fun `send image stores encrypted base64 content type`() = runTest {
        val sessionKey = ByteArray(32) { it.toByte() }
        val useCase    = buildUseCase(sessionKey)

        val rawJpeg = ByteArray(1000) { 0x42 }   // 1KB fake JPEG
        useCase(roomId = "room1", rawImageBytes = rawJpeg, mimeType = "image/jpeg")

        val content = capturedContent
        assertTrue(content != null, "sendMessage was not called")
        assertTrue(content is MessageContent.Image, "Content type should be Image")

        assertTrue(content.encryptedBase64.isNotEmpty(), "encryptedBase64 should not be empty")
        assertTrue(content.iv.isNotEmpty(), "IV should not be empty")
        assertEquals("image/jpeg", content.mimeType)
    }

    @Test
    fun `image over 900KB base64 throws size limit error`() = runTest {
        val sessionKey = ByteArray(32) { it.toByte() }
        val useCase    = buildUseCase(sessionKey)

        // ~700KB raw → ~933KB after encrypt+base64 → exceeds 900KB limit
        val tooBig = ByteArray(700_000) { 0x42 }

        assertFails("Should throw size limit exceeded") {
            useCase(roomId = "room1", rawImageBytes = tooBig, mimeType = "image/jpeg")
        }
    }
}
