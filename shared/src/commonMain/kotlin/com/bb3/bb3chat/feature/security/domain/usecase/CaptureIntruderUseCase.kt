package com.bb3.bb3chat.feature.security.domain.usecase

import com.bb3.bb3chat.core.crypto.CryptoManager
import com.bb3.bb3chat.core.crypto.SessionManager
import com.bb3.bb3chat.core.storage.KeyValueStorage
import com.bb3.bb3chat.core.storage.StorageKeys

/**
 * S16 — Intruder Capture
 *
 * Được gọi sau [MAX_WRONG_ATTEMPTS] lần nhập PIN sai liên tiếp.
 *
 * Flow:
 *  1. Platform layer (Android/iOS) chụp ảnh camera trước lặng lẽ.
 *  2. Ảnh raw bytes truyền vào [execute].
 *  3. CommonMain nén → AES-256-GCM encrypt → base64 → lưu vào EncryptedSharedPrefs.
 *  4. Metadata (timestamp, attempt count) ghi vào KeyValueStorage.
 *  5. Không upload ngay; chỉ upload khi PIN thật được mở thành công (tránh network trace).
 *
 * Platform sẽ implement [IntruderCameraCapture] expect/actual để chụp ảnh im lặng.
 */
class CaptureIntruderUseCase(
    private val storage        : KeyValueStorage,
    private val sessionManager : SessionManager
) {

    companion object {
        const val MAX_WRONG_ATTEMPTS = 3
    }

    /**
     * @param rawJpegBytes  JPEG bytes từ front camera (platform layer truyền vào)
     * @param attemptCount  số lần sai PIN tích lũy
     */
    fun execute(rawJpegBytes: ByteArray, attemptCount: Int) {
        runCatching {
            // Encrypt với ephemeral key (không cần session key — session chưa mở)
            val ephemeralKey  = CryptoManager.randomBytes(32)
            val encryptResult = CryptoManager.encrypt(rawJpegBytes, ephemeralKey)
            val base64Payload = encryptResult.cipherBytes.encodeBase64()

            // Lưu vào storage (EncryptedSharedPrefs)
            val slot = (attemptCount % 5)   // giữ tối đa 5 snapshot
            storage.putString(StorageKeys.INTRUDER_PHOTO_PREFIX + slot, base64Payload)
            storage.putString(StorageKeys.INTRUDER_PHOTO_IV_PREFIX + slot,
                encryptResult.iv.encodeBase64())
            storage.putString(StorageKeys.INTRUDER_PHOTO_KEY_PREFIX + slot,
                ephemeralKey.encodeBase64())
            storage.putLong(StorageKeys.INTRUDER_LAST_TS, currentEpochMillis())
            storage.putInt(StorageKeys.INTRUDER_ATTEMPT_COUNT, attemptCount)
        }
        // Silent fail — never crash the app or hint to intruder
    }

    /**
     * Lấy danh sách intruder snapshots đã lưu (gọi sau khi PIN thật đã mở).
     * Trả về list base64-encrypted blobs để ViewModel upload lên Firestore log.
     */
    fun getPendingSnapshots(): List<Pair<String, String>> {
        val count = storage.getInt(StorageKeys.INTRUDER_ATTEMPT_COUNT)
        val slots = minOf(count, 5)
        return (0 until slots).mapNotNull { slot ->
            val data = storage.getString(StorageKeys.INTRUDER_PHOTO_PREFIX + slot)
            val iv   = storage.getString(StorageKeys.INTRUDER_PHOTO_IV_PREFIX + slot)
            if (data != null && iv != null) data to iv else null
        }
    }

    fun clearSnapshots() {
        val count = storage.getInt(StorageKeys.INTRUDER_ATTEMPT_COUNT)
        (0 until minOf(count, 5)).forEach { slot ->
            storage.remove(StorageKeys.INTRUDER_PHOTO_PREFIX + slot)
            storage.remove(StorageKeys.INTRUDER_PHOTO_IV_PREFIX + slot)
            storage.remove(StorageKeys.INTRUDER_PHOTO_KEY_PREFIX + slot)
        }
        storage.remove(StorageKeys.INTRUDER_ATTEMPT_COUNT)
    }

    private fun currentEpochMillis(): Long = 0L  // platform override via expect/actual
}

// Extension helpers (inline to avoid Android/iOS import dependency in commonMain)
@OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
private fun ByteArray.encodeBase64(): String =
    kotlin.io.encoding.Base64.Default.encode(this)
