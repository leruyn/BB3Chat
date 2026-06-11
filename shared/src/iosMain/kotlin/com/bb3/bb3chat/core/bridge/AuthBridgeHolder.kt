package com.bb3.bb3chat.core.bridge

import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/** Swift Firebase Auth SDK is registered at app launch. */
object AuthBridgeHolder {
    private var getCurrentUidFn: (() -> String?)? = null
    private var isSignedInFn: (() -> Boolean)? = null
    private var signOutFn: (() -> Unit)? = null
    private var ensureSignedInFn: (
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) -> Unit = { onComplete, _ -> onComplete() }

    fun register(
        getCurrentUid: () -> String?,
        isSignedIn: () -> Boolean,
        signOut: () -> Unit,
        ensureSignedIn: (onComplete: () -> Unit, onError: (String) -> Unit) -> Unit
    ) {
        getCurrentUidFn = getCurrentUid
        isSignedInFn = isSignedIn
        signOutFn = signOut
        ensureSignedInFn = ensureSignedIn
    }

    fun getCurrentUid(): String? = getCurrentUidFn?.invoke()

    fun isSignedIn(): Boolean = isSignedInFn?.invoke() ?: false

    fun signOut() {
        signOutFn?.invoke()
    }

    /** Returns false on Firebase error instead of throwing (Swift cannot catch Kotlin exceptions). */
    suspend fun ensureSignedIn(): Boolean {
        if (isSignedIn()) return true
        return suspendCancellableCoroutine { cont ->
            ensureSignedInFn(
                { cont.resume(true) },
                { err ->
                    println("BB3 Firebase anonymous auth failed: $err")
                    cont.resume(false)
                }
            )
        }
    }
}
