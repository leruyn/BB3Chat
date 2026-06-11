package com.bb3.bb3chat.core.bridge

import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

object FcmTokenBridgeHolder {
    private var registerFn: ((
        token: String,
        platform: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) -> Unit)? = null

    fun register(
        register: (
            token: String,
            platform: String,
            onSuccess: () -> Unit,
            onError: (String) -> Unit
        ) -> Unit
    ) {
        registerFn = register
    }

    suspend fun registerToken(token: String, platform: String = "ios"): Boolean =
        suspendCancellableCoroutine { cont ->
            val fn = registerFn
            if (fn == null) {
                cont.resume(false)
                return@suspendCancellableCoroutine
            }
            fn(
                token,
                platform,
                { cont.resume(true) },
                { cont.resume(false) }
            )
        }
}
