package com.bb3.bb3chat.feature.token.domain.repository

interface TokenRepository {
    suspend fun onTokenRefreshed(rawToken: String)
    suspend fun sendHeartbeat()
    fun getLastHeartbeatMs(): Long
}
