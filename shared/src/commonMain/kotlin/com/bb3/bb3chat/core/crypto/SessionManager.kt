package com.bb3.bb3chat.core.crypto

import app.cash.sqldelight.db.SqlDriver
import com.bb3.bb3chat.db.BB3Database
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SessionManager {

    private var _sessionKey: ByteArray? = null
    private var _database: BB3Database? = null
    private val roomKeys = mutableMapOf<String, ByteArray>()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated

    var currentUserId: String? = null
        private set

    fun openRealSession(key: ByteArray, driver: SqlDriver, userId: String) {
        _sessionKey   = key.copyOf()
        _database     = BB3Database(driver)
        currentUserId = userId
        _isAuthenticated.value = true
        loadAllPersistedRoomKeys()
    }

    fun setCurrentUserId(userId: String) {
        currentUserId = userId
    }

    fun requireSessionKey(): ByteArray =
        _sessionKey ?: throw IllegalStateException("No active session — PIN not entered")

    fun requireDatabase(): BB3Database =
        _database ?: throw IllegalStateException("No active database session")

    fun setRoomKey(roomId: String, key: ByteArray) {
        roomKeys[roomId]?.fill(0)
        roomKeys[roomId] = key.copyOf()
        persistRoomKey(roomId, key)
    }

    fun loadRoomKey(roomId: String): ByteArray? {
        roomKeys[roomId]?.let { return it.copyOf() }
        val loaded = loadPersistedRoomKey(roomId) ?: return null
        roomKeys[roomId] = loaded.copyOf()
        return loaded
    }

    fun requireRoomKey(roomId: String): ByteArray =
        loadRoomKey(roomId)
            ?: throw IllegalStateException("Chưa có room key cho $roomId — ghép phòng lại")

    fun destroySessionKey() {
        _sessionKey?.fill(0)
        _sessionKey   = null
        _database     = null
        currentUserId = null
        roomKeys.values.forEach { it.fill(0) }
        roomKeys.clear()
        _isAuthenticated.value = false
    }

    val hasActiveSession: Boolean get() = _sessionKey != null

    @OptIn(ExperimentalEncodingApi::class)
    private fun persistRoomKey(roomId: String, key: ByteArray) {
        runCatching {
            val sessionKey = requireSessionKey()
            val db         = requireDatabase()
            val result     = CryptoManager.encrypt(key, sessionKey)
            val stored     = "${result.cipherBytes.encodeBase64()}:${result.iv.encodeBase64()}"
            db.appConfigQueries.upsert(key = "room_key_$roomId", value = stored)
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun loadPersistedRoomKey(roomId: String): ByteArray? {
        return runCatching {
            val stored = requireDatabase()
                .appConfigQueries.selectByKey("room_key_$roomId")
                .executeAsOneOrNull() ?: return null
            val parts = stored.split(':', limit = 2)
            if (parts.size != 2) return null
            val cipher = Base64.Default.decode(parts[0])
            val iv     = Base64.Default.decode(parts[1])
            CryptoManager.decrypt(cipher, iv, requireSessionKey())
        }.getOrNull()
    }

    private fun loadAllPersistedRoomKeys() {
        runCatching {
            val db = requireDatabase()
            // Room keys are loaded lazily per roomId via loadRoomKey()
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun ByteArray.encodeBase64(): String = Base64.Default.encode(this)
}
