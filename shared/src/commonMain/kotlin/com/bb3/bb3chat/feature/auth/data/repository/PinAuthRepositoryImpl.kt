package com.bb3.bb3chat.feature.auth.data.repository

import com.bb3.bb3chat.core.crypto.CryptoManager
import com.bb3.bb3chat.core.crypto.SessionManager
import com.bb3.bb3chat.core.db.DatabaseDriverFactory
import com.bb3.bb3chat.core.storage.KeyValueStorage
import com.bb3.bb3chat.core.storage.StorageKeys
import com.bb3.bb3chat.core.util.toHexLower
import com.bb3.bb3chat.core.util.toHexUpper
import com.bb3.bb3chat.feature.auth.domain.model.PinValidationResult
import com.bb3.bb3chat.feature.auth.domain.repository.PinAuthRepository

class PinAuthRepositoryImpl(
    private val storage: KeyValueStorage,
    private val driverFactory: DatabaseDriverFactory,
    private val sessionManager: SessionManager
) : PinAuthRepository {

    override suspend fun validatePin(inputPin: String): PinValidationResult {
        val derivedKey = CryptoManager.deriveKeyFromPin(inputPin)
        val keyHex     = derivedKey.toHex()

        val realHash  = storage.getString(StorageKeys.REAL_PIN_HASH)
        val decoyHash = storage.getString(StorageKeys.DECOY_PIN_HASH)

        return when (keyHex) {
            realHash -> {
                val driver = driverFactory.createDriver(derivedKey)
                sessionManager.openRealSession(derivedKey, driver, userId = "")
                val alias = ensureUserAlias()
                sessionManager.setCurrentUserId(alias)
                PinValidationResult.RealAccess
            }
            decoyHash -> {
                val decoyDriver = driverFactory.createDecoyDriver()
                sessionManager.openRealSession(derivedKey, decoyDriver, userId = "")
                val alias = ensureUserAlias()
                sessionManager.setCurrentUserId(alias)
                PinValidationResult.DecoyAccess
            }
            else -> PinValidationResult.InvalidPin
        }
    }

    override suspend fun setRealPin(newPin: String) {
        val key = CryptoManager.deriveKeyFromPin(newPin)
        storage.putString(StorageKeys.REAL_PIN_HASH, key.toHex())
    }

    override suspend fun setDecoyPin(newPin: String) {
        val key = CryptoManager.deriveKeyFromPin(newPin)
        storage.putString(StorageKeys.DECOY_PIN_HASH, key.toHex())
    }

    override suspend fun savePendingRealPin(pin: String) {
        val key = CryptoManager.deriveKeyFromPin(pin)
        storage.putString(StorageKeys.SETUP_PENDING_REAL_HASH, key.toHex())
    }

    override suspend fun confirmPendingRealPin(pin: String): Boolean {
        val pendingHash = storage.getString(StorageKeys.SETUP_PENDING_REAL_HASH) ?: return false
        val inputHash   = CryptoManager.deriveKeyFromPin(pin).toHex()
        if (inputHash != pendingHash) return false
        setRealPin(pin)
        storage.remove(StorageKeys.SETUP_PENDING_REAL_HASH)
        return true
    }

    override fun hasPendingRealPin(): Boolean =
        storage.getString(StorageKeys.SETUP_PENDING_REAL_HASH) != null

    override fun isSameAsRealPin(pin: String): Boolean {
        val realHash = storage.getString(StorageKeys.REAL_PIN_HASH) ?: return false
        return CryptoManager.deriveKeyFromPin(pin).toHex() == realHash
    }

    override fun isPinConfigured(): Boolean =
        storage.getString(StorageKeys.REAL_PIN_HASH) != null

    override fun isDecoyPinConfigured(): Boolean =
        storage.getString(StorageKeys.DECOY_PIN_HASH) != null

    /** Creates a stable anonymous alias stored in encrypted AppConfig. */
    private fun ensureUserAlias(): String {
        val db = sessionManager.requireDatabase()
        db.appConfigQueries.selectByKey("user_alias").executeAsOneOrNull()?.let { existing ->
            storage.putString(StorageKeys.USER_ALIAS, existing)
            return existing
        }
        val alias = CryptoManager.randomBytes(4).toHexUpper().take(8)
        db.appConfigQueries.upsert(key = "user_alias", value = alias)
        storage.putString(StorageKeys.USER_ALIAS, alias)
        return alias
    }

    private fun ByteArray.toHex(): String = toHexLower()
}
