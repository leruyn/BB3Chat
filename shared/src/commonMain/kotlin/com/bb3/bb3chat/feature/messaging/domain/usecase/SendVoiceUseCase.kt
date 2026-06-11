package com.bb3.bb3chat.feature.messaging.domain.usecase

import com.bb3.bb3chat.core.crypto.CryptoManager
import com.bb3.bb3chat.core.crypto.SessionManager
import com.bb3.bb3chat.feature.messaging.domain.model.MessageContent
import com.bb3.bb3chat.feature.messaging.domain.repository.MessageRepository
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private const val MAX_VOICE_BYTES = 900_000

class SendVoiceUseCase(
    private val repository: MessageRepository,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(
        roomId: String,
        audioBytes: ByteArray,
        durationMs: Long
    ): String {
        val key       = sessionManager.requireRoomKey(roomId)
        val encrypted = CryptoManager.encrypt(audioBytes, key)
        val encBase64 = encrypted.cipherBytes.toBase64()
        require(encBase64.length < MAX_VOICE_BYTES) { "Ghi âm quá dài." }

        val content = MessageContent.Voice(
            encryptedBase64 = encBase64,
            iv              = encrypted.iv.toBase64(),
            durationMs      = durationMs
        )
        return repository.sendMessage(roomId, content)
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun ByteArray.toBase64(): String = Base64.Default.encode(this)
}
