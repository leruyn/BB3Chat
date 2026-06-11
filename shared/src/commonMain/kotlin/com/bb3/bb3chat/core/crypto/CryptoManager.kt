package com.bb3.bb3chat.core.crypto

data class EncryptResult(val cipherBytes: ByteArray, val iv: ByteArray)

expect object CryptoManager {
    fun deriveKeyFromPin(pin: String): ByteArray
    fun encrypt(plainBytes: ByteArray, key: ByteArray): EncryptResult
    fun decrypt(cipherBytes: ByteArray, iv: ByteArray, key: ByteArray): ByteArray
    fun encryptString(plain: String, key: ByteArray): EncryptResult
    fun decryptString(cipherB64: String, ivB64: String, key: ByteArray): String
    fun randomBytes(size: Int): ByteArray
    /** PBKDF2 with app-fixed salt — identical output on every device for the same material. */
    fun deriveSharedRoomKey(material: String): ByteArray
}
