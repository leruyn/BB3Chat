package com.bb3.bb3chat.feature.token.data

import com.bb3.bb3chat.core.crypto.CryptoManager
import com.bb3.bb3chat.core.storage.KeyValueStorage
import com.bb3.bb3chat.core.storage.StorageKeys
import com.bb3.bb3chat.feature.token.domain.repository.TokenRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.datetime.Clock

class TokenRepositoryImpl(
    private val httpClient: HttpClient,
    private val storage: KeyValueStorage,
    private val vpsRelayUrl: String
) : TokenRepository {

    override suspend fun onTokenRefreshed(rawToken: String) {
        val key      = getStorageKey()
        val result   = CryptoManager.encryptString(rawToken, key)
        val encToken = result.cipherBytes.toBase64()
        val iv       = result.iv.toBase64()
        storage.putString(StorageKeys.FCM_TOKEN_ENCRYPTED, encToken)
        storage.putString(StorageKeys.FCM_TOKEN_IV, iv)
        pushTokenToVps(encToken)
    }

    override suspend fun sendHeartbeat() {
        val encToken = storage.getString(StorageKeys.FCM_TOKEN_ENCRYPTED) ?: return
        pushTokenToVps(encToken)
        storage.putLong(StorageKeys.LAST_HEARTBEAT_MS, Clock.System.now().toEpochMilliseconds())
    }

    override fun getLastHeartbeatMs(): Long =
        storage.getLong(StorageKeys.LAST_HEARTBEAT_MS, 0L)

    private suspend fun pushTokenToVps(encryptedToken: String) {
        runCatching {
            httpClient.post("$vpsRelayUrl/relay/token") {
                header("User-Agent", "Mozilla/5.0 (compatible)")
                header("X-Forwarded-For", "0.0.0.0")
                contentType(ContentType.Application.Json)
                setBody(mapOf("t" to encryptedToken))
            }
        }
    }

    private fun getStorageKey(): ByteArray {
        val storedKey = storage.getString("relay_key")
        if (storedKey != null) return storedKey.fromBase64()
        val newKey = CryptoManager.randomBytes(32)
        storage.putString("relay_key", newKey.toBase64())
        return newKey
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun ByteArray.toBase64(): String = Base64.Default.encode(this)

    @OptIn(ExperimentalEncodingApi::class)
    private fun String.fromBase64(): ByteArray = Base64.Default.decode(this)
}
