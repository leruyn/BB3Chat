package com.bb3.bb3chat.feature.messaging.domain.usecase

import com.bb3.bb3chat.core.crypto.CryptoManager
import com.bb3.bb3chat.core.crypto.SessionManager
import com.bb3.bb3chat.core.platform.ImageProcessor
import com.bb3.bb3chat.feature.messaging.domain.model.DestructConfig
import com.bb3.bb3chat.feature.messaging.domain.model.MessageContent
import com.bb3.bb3chat.feature.messaging.domain.repository.MessageRepository

private const val MAX_FIRESTORE_BYTES = 900_000

class SendImageUseCase(
    private val repository: MessageRepository,
    private val sessionManager: SessionManager,
    private val imageProcessor: ImageProcessor
) {
    suspend operator fun invoke(
        roomId: String,
        rawImageBytes: ByteArray,
        mimeType: String,
        destructConfig: DestructConfig? = null
    ): String {
        val key        = sessionManager.requireRoomKey(roomId)
        val compressed = imageProcessor.compressAndResize(rawImageBytes, maxDimension = 1080, quality = 65)
        val blurHash   = imageProcessor.generateBlurHash(compressed)
        val encrypted  = CryptoManager.encrypt(compressed, key)

        val encBase64 = encrypted.cipherBytes.toBase64()
        require(encBase64.length < MAX_FIRESTORE_BYTES) {
            "Ảnh quá lớn sau khi nén (${encBase64.length} bytes). Vui lòng chọn ảnh nhỏ hơn."
        }

        val content = MessageContent.Image(
            encryptedBase64 = encBase64,
            iv              = encrypted.iv.toBase64(),
            mimeType        = mimeType,
            compressedBytes = compressed.size,
            blurHash        = blurHash
        )
        return repository.sendMessage(roomId, content, destructConfig)
    }

    @OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
    private fun ByteArray.toBase64(): String =
        kotlin.io.encoding.Base64.Default.encode(this)
}
