package com.bb3.bb3chat.core.platform

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object AppBackgroundCallbacks {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var onBackground: (suspend () -> Unit)? = null

    fun register(callback: suspend () -> Unit) {
        onBackground = callback
    }

    fun notifyAppBackground() {
        val callback = onBackground ?: return
        scope.launch { callback() }
    }
}
