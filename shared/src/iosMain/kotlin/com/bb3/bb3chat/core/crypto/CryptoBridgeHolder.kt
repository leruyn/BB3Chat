package com.bb3.bb3chat.core.crypto

/**
 * Swift [CryptoBridge] registers AES-GCM handlers at app launch (CryptoKit).
 * PBKDF2 stays in [CryptoManager] via platform.CoreCrypto.
 */
object CryptoBridgeHolder {
    private var encryptFn: ((ByteArray, ByteArray, ByteArray) -> ByteArray)? = null
    private var decryptFn: ((ByteArray, ByteArray, ByteArray) -> ByteArray)? = null

    fun register(
        encrypt: (ByteArray, ByteArray, ByteArray) -> ByteArray,
        decrypt: (ByteArray, ByteArray, ByteArray) -> ByteArray
    ) {
        encryptFn = encrypt
        decryptFn = decrypt
    }

    fun aesGcmEncrypt(plain: ByteArray, key: ByteArray, iv: ByteArray): ByteArray =
        encryptFn?.invoke(plain, key, iv)
            ?: error("CryptoBridge chưa đăng ký — gọi từ AppDelegate sau initIosKoin")

    fun aesGcmDecrypt(cipherWithTag: ByteArray, key: ByteArray, iv: ByteArray): ByteArray =
        decryptFn?.invoke(cipherWithTag, key, iv)
            ?: error("CryptoBridge chưa đăng ký — gọi từ AppDelegate sau initIosKoin")
}
