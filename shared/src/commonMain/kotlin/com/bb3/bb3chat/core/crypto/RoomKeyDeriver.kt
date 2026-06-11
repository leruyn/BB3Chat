package com.bb3.bb3chat.core.crypto

/**
 * Derives a shared AES-256 key from both peers' handshake codes.
 * Sorting guarantees device A and device B produce the same key.
 */
object RoomKeyDeriver {
    fun derive(codeA: String, codeB: String): ByteArray {
        val material = listOf(codeA.uppercase(), codeB.uppercase()).sorted().joinToString("|")
        return CryptoManager.deriveSharedRoomKey(material)
    }
}
