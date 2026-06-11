@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.bb3.bb3chat.core.crypto

import kotlinx.cinterop.*
import platform.CoreCrypto.*
import platform.CoreFoundation.*
import platform.Foundation.*
import platform.Security.*
import platform.posix.memcpy

private const val PBKDF2_ITERATIONS = 100_000
private const val KEY_BYTES         = 32
private const val IV_LENGTH         = 12
private const val KEYCHAIN_SERVICE  = "com.bb3.bb3chat"
private const val SALT_ACCOUNT      = "pbkdf2_salt"

actual object CryptoManager {

    actual fun deriveKeyFromPin(pin: String): ByteArray {
        val salt = getOrCreateSalt()
        return pbkdf2HmacSha256(pin, salt, PBKDF2_ITERATIONS, KEY_BYTES)
    }

    actual fun encrypt(plainBytes: ByteArray, key: ByteArray): EncryptResult {
        val iv = randomBytes(IV_LENGTH)
        val cipher = CryptoBridgeHolder.aesGcmEncrypt(plainBytes, key, iv)
        return EncryptResult(cipher, iv)
    }

    actual fun decrypt(cipherBytes: ByteArray, iv: ByteArray, key: ByteArray): ByteArray =
        CryptoBridgeHolder.aesGcmDecrypt(cipherBytes, key, iv)

    actual fun encryptString(plain: String, key: ByteArray): EncryptResult =
        encrypt(plain.encodeToByteArray(), key)

    actual fun decryptString(cipherB64: String, ivB64: String, key: ByteArray): String {
        val cipher = NSData.create(base64EncodedString = cipherB64, options = 0u)!!.toByteArray()
        val iv     = NSData.create(base64EncodedString = ivB64, options = 0u)!!.toByteArray()
        return decrypt(cipher, iv, key).decodeToString()
    }

    actual fun randomBytes(size: Int): ByteArray {
        val bytes = ByteArray(size)
        bytes.usePinned {
            SecRandomCopyBytes(kSecRandomDefault, size.toULong(), it.addressOf(0))
        }
        return bytes
    }

    actual fun deriveSharedRoomKey(material: String): ByteArray {
        val salt = "bb3-chat-room-v1".encodeToByteArray()
        return pbkdf2HmacSha256(material, salt, PBKDF2_ITERATIONS, KEY_BYTES)
    }

    private fun getOrCreateSalt(): ByteArray {
        keychainLoad(KEYCHAIN_SERVICE, SALT_ACCOUNT)?.let { return it }
        val salt = randomBytes(16)
        keychainSave(KEYCHAIN_SERVICE, SALT_ACCOUNT, salt)
        return salt
    }

    private fun pbkdf2HmacSha256(
        password: String,
        salt: ByteArray,
        iterations: Int,
        keyLen: Int
    ): ByteArray {
        val derived = ByteArray(keyLen)
        salt.usePinned { s ->
            derived.usePinned { d ->
                CCKeyDerivationPBKDF(
                    kCCPBKDF2,
                    password,
                    password.length.toULong(),
                    s.addressOf(0).reinterpret(),
                    salt.size.toULong(),
                    kCCPRFHmacAlgSHA256,
                    iterations.toUInt(),
                    d.addressOf(0).reinterpret(),
                    keyLen.toULong()
                )
            }
        }
        return derived
    }
}

private fun keychainSave(service: String, account: String, data: ByteArray) {
    val query = mapOf<Any?, Any?>(
        kSecClass to kSecClassGenericPassword,
        kSecAttrService to service,
        kSecAttrAccount to account,
        kSecValueData to data.toNSData()
    )
    SecItemDelete(query as CFDictionaryRef)
    SecItemAdd(query as CFDictionaryRef, null)
}

private fun keychainLoad(service: String, account: String): ByteArray? {
    val query = mapOf<Any?, Any?>(
        kSecClass to kSecClassGenericPassword,
        kSecAttrService to service,
        kSecAttrAccount to account,
        kSecReturnData to kCFBooleanTrue,
        kSecMatchLimit to kSecMatchLimitOne
    )
    memScoped {
        val result = alloc<CFTypeRefVar>()
        val status = SecItemCopyMatching(query as CFDictionaryRef, result.ptr)
        if (status != errSecSuccess) return null
        val data = result.value as NSData
        return data.toByteArray()
    }
}

private fun ByteArray.toNSData(): NSData = usePinned {
    NSData.create(bytes = it.addressOf(0), length = size.toULong())
}

private fun NSData.toByteArray(): ByteArray {
    val length = this.length.toInt()
    if (length == 0) return ByteArray(0)
    return ByteArray(length).also { arr ->
        arr.usePinned { memcpy(it.addressOf(0), bytes, length.toULong()) }
    }
}
