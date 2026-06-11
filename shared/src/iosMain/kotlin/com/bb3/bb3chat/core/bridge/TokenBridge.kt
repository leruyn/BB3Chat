package com.bb3.bb3chat.core.bridge

import com.bb3.bb3chat.feature.token.domain.repository.TokenRepository

/** Swift AppDelegate forwards FCM token refresh events into Koin. */
object TokenBridge {
    private var repository: TokenRepository? = null

    fun bind(repository: TokenRepository) {
        this.repository = repository
    }

    suspend fun onTokenRefreshed(token: String) {
        repository?.onTokenRefreshed(token)
    }
}
