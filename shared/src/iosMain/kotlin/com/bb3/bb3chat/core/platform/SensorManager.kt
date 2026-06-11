package com.bb3.bb3chat.core.platform

actual class SensorManager {
    // iOS implementation delegates to Swift SensorBridge via callback registered at startup
    private var panicCallback: (() -> Unit)? = null
    private var lastPanicTime = 0L
    private val cooldownMs = 3_000L

    actual fun startListening(onPanicTrigger: () -> Unit) {
        panicCallback = onPanicTrigger
        SensorBridgeHolder.register { triggerIfCooledDown() }
    }

    actual fun stopListening() {
        panicCallback = null
        SensorBridgeHolder.unregister()
    }

    private fun triggerIfCooledDown() {
        val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        if (now - lastPanicTime > cooldownMs) {
            lastPanicTime = now
            panicCallback?.invoke()
        }
    }
}

// Singleton bridge — Swift SensorBridge calls this from Kotlin
object SensorBridgeHolder {
    private var callback: (() -> Unit)? = null
    fun register(cb: () -> Unit)  { callback = cb }
    fun unregister()              { callback = null }
    fun onPanicDetected()         { callback?.invoke() }
}
