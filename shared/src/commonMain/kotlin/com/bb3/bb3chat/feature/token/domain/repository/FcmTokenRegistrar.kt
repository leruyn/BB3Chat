package com.bb3.bb3chat.feature.token.domain.repository

/** Registers the device FCM token for server-side push delivery. */
interface FcmTokenRegistrar {
    suspend fun registerToken(rawToken: String)
}
