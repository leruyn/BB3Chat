package com.bb3.bb3chat.core.platform

import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

actual class FakePushNotifier {

    actual fun show(message: FakePushMessage) {
        LocalNotificationBridgeHolder.show(title = message.title, body = message.body)
    }
}

object LocalNotificationBridgeHolder {
    private var showFn: ((String, String) -> Unit)? = null
    private var areGrantedFn: (() -> Boolean)? = null
    private var ensureGrantedFn: (((Boolean) -> Unit) -> Unit)? = null

    fun register(
        show: (String, String) -> Unit,
        areGranted: () -> Boolean,
        ensureGranted: (((Boolean) -> Unit) -> Unit)
    ) {
        showFn = show
        areGrantedFn = areGranted
        ensureGrantedFn = ensureGranted
    }

    fun show(title: String, body: String) {
        showFn?.invoke(title, body)
    }

    fun areNotificationsGranted(): Boolean = areGrantedFn?.invoke() ?: false

    suspend fun ensureNotificationsGranted(): Boolean = suspendCoroutine { cont ->
        val fn = ensureGrantedFn
        if (fn == null) {
            cont.resume(false)
        } else {
            fn { granted -> cont.resume(granted) }
        }
    }
}
