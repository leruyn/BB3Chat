package com.bb3.bb3chat.crypto

import com.bb3.bb3chat.core.crypto.CryptoManager
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotEquals

/**
 * S18 — E2E crypto unit tests (commonTest, runs on JVM via KMP test task)
 *
 * These tests use the expect/actual CryptoManager. On JVM the actual is the
 * Android implementation via Robolectric, or a test double that mirrors the same
 * PBKDF2 + AES-GCM logic.
 */
class CryptoManagerTest {

    // ── PIN key derivation ────────────────────────────────────────────────────

    @Test
    fun `same PIN produces same key with same salt`() {
        // NOTE: In real impl, salt is stored per-device. We test determinism with fixed salt.
        val pin  = "1234"
        val key1 = CryptoManager.deriveKeyFromPin(pin)
        val key2 = CryptoManager.deriveKeyFromPin(pin)
        // Keys derived with the SAME stored salt must be equal
        assertContentEquals(key1, key2)
    }

    @Test
    fun `different PINs produce different keys`() {
        val key1 = CryptoManager.deriveKeyFromPin("1234")
        val key2 = CryptoManager.deriveKeyFromPin("5678")
        assertNotEquals(key1.toList(), key2.toList())
    }

    @Test
    fun `key length is 32 bytes (AES-256)`() {
        val key = CryptoManager.deriveKeyFromPin("9999")
        assertEquals(32, key.size)
    }

    // ── Encrypt / Decrypt round-trip ──────────────────────────────────────────

    @Test
    fun `encrypt then decrypt returns original plaintext`() {
        val key       = CryptoManager.deriveKeyFromPin("1234")
        val plaintext = "Tin nhắn bí mật 🔒".encodeToByteArray()

        val result    = CryptoManager.encrypt(plaintext, key)
        val decrypted = CryptoManager.decrypt(result.cipherBytes, result.iv, key)

        assertContentEquals(plaintext, decrypted)
    }

    @Test
    fun `encrypted data differs from plaintext`() {
        val key       = CryptoManager.deriveKeyFromPin("1234")
        val plaintext = "hello world".encodeToByteArray()
        val result    = CryptoManager.encrypt(plaintext, key)

        assertNotEquals(plaintext.toList(), result.cipherBytes.toList())
    }

    @Test
    fun `two encryptions of same plaintext produce different ciphertexts (random IV)`() {
        val key       = CryptoManager.deriveKeyFromPin("1234")
        val plaintext = "same message".encodeToByteArray()

        val r1 = CryptoManager.encrypt(plaintext, key)
        val r2 = CryptoManager.encrypt(plaintext, key)

        // IVs must differ (random IV)
        assertNotEquals(r1.iv.toList(), r2.iv.toList())
        // But both decrypt correctly
        assertContentEquals(plaintext, CryptoManager.decrypt(r1.cipherBytes, r1.iv, key))
        assertContentEquals(plaintext, CryptoManager.decrypt(r2.cipherBytes, r2.iv, key))
    }

    @Test
    fun `decrypt with wrong key fails`() {
        val key1      = CryptoManager.deriveKeyFromPin("1234")
        val key2      = CryptoManager.deriveKeyFromPin("9999")
        val plaintext = "secret".encodeToByteArray()
        val result    = CryptoManager.encrypt(plaintext, key1)

        assertFails { CryptoManager.decrypt(result.cipherBytes, result.iv, key2) }
    }

    @Test
    fun `decrypt with tampered ciphertext fails (GCM auth tag)`() {
        val key     = CryptoManager.deriveKeyFromPin("1234")
        val result  = CryptoManager.encrypt("data".encodeToByteArray(), key)
        val tampered = result.cipherBytes.copyOf().also { it[0] = (it[0] + 1).toByte() }

        assertFails { CryptoManager.decrypt(tampered, result.iv, key) }
    }

    // ── Large payload (inline image base64 simulation) ────────────────────────

    @Test
    fun `encrypt 300KB payload completes without error`() {
        val key     = CryptoManager.deriveKeyFromPin("1234")
        val payload = ByteArray(300_000) { it.toByte() }
        val result  = CryptoManager.encrypt(payload, key)

        assertContentEquals(payload, CryptoManager.decrypt(result.cipherBytes, result.iv, key))
    }

    @Test
    fun `encrypted 300KB payload stays under 900KB limit`() {
        val key     = CryptoManager.deriveKeyFromPin("1234")
        val payload = ByteArray(300_000) { it.toByte() }
        val result  = CryptoManager.encrypt(payload, key)
        // base64 overhead: ~4/3x
        val base64Size = (result.cipherBytes.size.toDouble() * 4 / 3).toInt()
        assert(base64Size < 900_000) {
            "base64 size $base64Size exceeds 900KB Firestore limit"
        }
    }
}
