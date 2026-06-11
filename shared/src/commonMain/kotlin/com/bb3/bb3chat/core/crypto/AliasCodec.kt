package com.bb3.bb3chat.core.crypto

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Encrypts/decrypts display strings (room alias, snippets) stored in SQLDelight.
 * Wire format: "{cipherB64}:{ivB64}"
 */
@OptIn(ExperimentalEncodingApi::class)
object AliasCodec {

    fun encrypt(plain: String, sessionKey: ByteArray): String {
        val result = CryptoManager.encryptString(plain, sessionKey)
        return "${result.cipherBytes.encodeBase64()}:${result.iv.encodeBase64()}"
    }

    fun decrypt(stored: String, sessionKey: ByteArray): String {
        if (stored.isBlank() || !stored.contains(':')) return stored
        val parts = stored.split(':', limit = 2)
        if (parts.size != 2) return stored
        return runCatching {
            CryptoManager.decryptString(parts[0], parts[1], sessionKey)
        }.getOrDefault(stored)
    }

    private fun ByteArray.encodeBase64(): String = Base64.Default.encode(this)
}
