package com.bb3.bb3chat.feature.messaging.domain.usecase

import com.bb3.bb3chat.feature.messaging.domain.model.DestructConfig
import com.bb3.bb3chat.feature.messaging.domain.model.MessageContent
import com.bb3.bb3chat.feature.messaging.domain.repository.MessageRepository

/**
 * Sends a plaintext message. Encryption happens once in [MessageRepository.sendMessage].
 */
class SendMessageUseCase(
    private val repository: MessageRepository
) {
    suspend operator fun invoke(
        roomId: String,
        plainText: String,
        destructConfig: DestructConfig? = null
    ): String {
        val content = MessageContent.Text(
            encryptedBody = plainText,
            iv            = ""
        )
        return repository.sendMessage(roomId, content, destructConfig)
    }
}
