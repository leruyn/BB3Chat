package com.bb3.bb3chat.core.crypto

import android.content.Context
import com.bb3.bb3chat.core.platform.AndroidContextHolder
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec

private const val SALT_PREFS_NAME = "bb3_crypto_prefs"
private const val SALT_PREF_KEY     = "pbkdf2_salt"
private const val AES_TRANSFORM     = "AES/GCM/NoPadding"
private const val GCM_TAG_LENGTH    = 128
private const val IV_LENGTH         = 12
private const val PBKDF2_ITERATIONS = 100_000
private const val KEY_BITS          = 256

actual object CryptoManager {

    actual fun deriveKeyFromPin(pin: String): ByteArray {
        val salt = getOrCreateSalt()
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(pin.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_BITS)
        return factory.generateSecret(spec).encoded
    }

    actual fun encrypt(plainBytes: ByteArray, key: ByteArray): EncryptResult {
        val iv = randomBytes(IV_LENGTH)
        val cipher = Cipher.getInstance(AES_TRANSFORM)
        cipher.init(Cipher.ENCRYPT_MODE, key.toSecretKey(), GCMParameterSpec(GCM_TAG_LENGTH, iv))
        return EncryptResult(cipher.doFinal(plainBytes), iv)
    }

    actual fun decrypt(cipherBytes: ByteArray, iv: ByteArray, key: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(AES_TRANSFORM)
        cipher.init(Cipher.DECRYPT_MODE, key.toSecretKey(), GCMParameterSpec(GCM_TAG_LENGTH, iv))
        return cipher.doFinal(cipherBytes)
    }

    actual fun encryptString(plain: String, key: ByteArray): EncryptResult =
        encrypt(plain.toByteArray(Charsets.UTF_8), key)

    actual fun decryptString(cipherB64: String, ivB64: String, key: ByteArray): String {
        val cipher = Base64.getDecoder().decode(cipherB64)
        val iv     = Base64.getDecoder().decode(ivB64)
        return decrypt(cipher, iv, key).toString(Charsets.UTF_8)
    }

    actual fun randomBytes(size: Int): ByteArray =
        ByteArray(size).also { SecureRandom().nextBytes(it) }

    actual fun deriveSharedRoomKey(material: String): ByteArray {
        val salt = "bb3-chat-room-v1".toByteArray(Charsets.UTF_8)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(material.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_BITS)
        return factory.generateSecret(spec).encoded
    }

    private fun getOrCreateSalt(): ByteArray {
        // JVM unit tests: no Android context — use a fixed salt.
        val context = AndroidContextHolder.getOrNull()
            ?: return ByteArray(16) { it.toByte() }

        val prefs = context.getSharedPreferences(SALT_PREFS_NAME, Context.MODE_PRIVATE)

        prefs.getString(SALT_PREF_KEY, null)?.let { stored ->
            return Base64.getDecoder().decode(stored)
        }

        val salt = randomBytes(16)
        prefs.edit()
            .putString(SALT_PREF_KEY, Base64.getEncoder().encodeToString(salt))
            .apply()
        return salt
    }

    private fun ByteArray.toSecretKey(): SecretKey =
        javax.crypto.spec.SecretKeySpec(this, "AES")
}

fun ByteArray.toBase64(): String = Base64.getEncoder().encodeToString(this)
fun String.fromBase64(): ByteArray = Base64.getDecoder().decode(this)
