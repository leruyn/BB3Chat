@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlin.io.encoding.ExperimentalEncodingApi::class)

package com.bb3.bb3chat.core.crypto

import kotlin.io.encoding.Base64
import kotlinx.cinterop.*
import platform.CoreCrypto.*
import platform.Foundation.NSUserDefaults
import platform.Security.*
import platform.posix.memcpy

private const val PBKDF2_ITERATIONS = 100_000
private const val KEY_BYTES         = 32
private const val IV_LENGTH         = 12
private const val SALT_PREFS_KEY    = "pbkdf2_salt_b64"

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
        val cipher = Base64.Default.decode(cipherB64)
        val iv     = Base64.Default.decode(ivB64)
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
        val defaults = NSUserDefaults.standardUserDefaults
        defaults.stringForKey(SALT_PREFS_KEY)?.let { stored ->
            return Base64.Default.decode(stored)
        }
        val salt = randomBytes(16)
        defaults.setObject(Base64.Default.encode(salt), SALT_PREFS_KEY)
        return salt
    }

    private fun pbkdf2HmacSha256(
        password: String,
        salt: ByteArray,
        iterations: Int,
        keyLen: Int
    ): ByteArray {
        val passwordLen = password.encodeToByteArray().size.toULong()
        val derived = ByteArray(keyLen)
        salt.usePinned { s ->
            derived.usePinned { d ->
                val status = CCKeyDerivationPBKDF(
                    kCCPBKDF2,
                    password,
                    passwordLen,
                    s.addressOf(0).reinterpret(),
                    salt.size.toULong(),
                    kCCPRFHmacAlgSHA256,
                    iterations.toUInt(),
                    d.addressOf(0).reinterpret(),
                    keyLen.toULong()
                )
                if (status != kCCSuccess) {
                    error("PBKDF2 failed with status $status")
                }
            }
        }
        return derived
    }
}
