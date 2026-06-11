package com.bb3.bb3chat.core.platform

expect class SensorManager {
    fun startListening(onPanicTrigger: () -> Unit)
    fun stopListening()
}
