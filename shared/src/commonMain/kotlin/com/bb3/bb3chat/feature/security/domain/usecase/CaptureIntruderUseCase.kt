package com.bb3.bb3chat.feature.security.domain.usecase

import com.bb3.bb3chat.core.crypto.CryptoManager
import com.bb3.bb3chat.core.storage.KeyValueStorage
import com.bb3.bb3chat.core.storage.StorageKeys
import kotlinx.datetime.Clock

/**
 * Stores encrypted intruder snapshots locally after failed PIN attempts.
 */
class CaptureIntruderUseCase(
    private val storage: KeyValueStorage
) {

    companion object {
        const val MAX_WRONG_ATTEMPTS = 3
        const val INTRUDER_TRIGGER_AT  = 2
        const val MAX_SLOTS            = 5
    }

    fun execute(rawJpegBytes: ByteArray, attemptCount: Int) {
        runCatching {
            val ephemeralKey  = CryptoManager.randomBytes(32)
            val encryptResult = CryptoManager.encrypt(rawJpegBytes, ephemeralKey)
            val base64Payload = encryptResult.cipherBytes.encodeBase64()
            val slot = (attemptCount - 1) % MAX_SLOTS

            storage.putString(StorageKeys.INTRUDER_PHOTO_PREFIX + slot, base64Payload)
            storage.putString(StorageKeys.INTRUDER_PHOTO_IV_PREFIX + slot,
                encryptResult.iv.encodeBase64())
            storage.putString(StorageKeys.INTRUDER_PHOTO_KEY_PREFIX + slot,
                ephemeralKey.encodeBase64())
            storage.putLong(StorageKeys.INTRUDER_LAST_TS, Clock.System.now().toEpochMilliseconds())
            storage.putInt(StorageKeys.INTRUDER_ATTEMPT_COUNT, attemptCount)
        }
    }

    fun onNewSnapshot() {
        storage.putBoolean(StorageKeys.INTRUDER_SEEN, false)
    }

    fun snapshotCount(): Int {
        val count = storage.getInt(StorageKeys.INTRUDER_ATTEMPT_COUNT)
        return minOf(count, MAX_SLOTS).coerceAtLeast(0)
    }

    fun getDecryptedSnapshots(): List<ByteArray> {
        val slots = snapshotCount()
        return (0 until slots).mapNotNull { slot ->
            val data = storage.getString(StorageKeys.INTRUDER_PHOTO_PREFIX + slot) ?: return@mapNotNull null
            val iv   = storage.getString(StorageKeys.INTRUDER_PHOTO_IV_PREFIX + slot) ?: return@mapNotNull null
            val key  = storage.getString(StorageKeys.INTRUDER_PHOTO_KEY_PREFIX + slot) ?: return@mapNotNull null
            runCatching {
                val cipher = data.decodeBase64()
                val ivBytes = iv.decodeBase64()
                val keyBytes = key.decodeBase64()
                CryptoManager.decrypt(cipher, ivBytes, keyBytes)
            }.getOrNull()
        }
    }

    fun clearSnapshots() {
        val slots = snapshotCount()
        (0 until slots).forEach { slot ->
            storage.remove(StorageKeys.INTRUDER_PHOTO_PREFIX + slot)
            storage.remove(StorageKeys.INTRUDER_PHOTO_IV_PREFIX + slot)
            storage.remove(StorageKeys.INTRUDER_PHOTO_KEY_PREFIX + slot)
        }
        storage.remove(StorageKeys.INTRUDER_ATTEMPT_COUNT)
        storage.remove(StorageKeys.INTRUDER_LAST_TS)
    }

    @OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
    private fun ByteArray.encodeBase64(): String =
        kotlin.io.encoding.Base64.Default.encode(this)

    @OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
    private fun String.decodeBase64(): ByteArray =
        kotlin.io.encoding.Base64.Default.decode(this)
}
