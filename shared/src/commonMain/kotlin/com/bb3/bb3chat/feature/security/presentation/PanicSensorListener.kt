package com.bb3.bb3chat.feature.security.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.bb3.bb3chat.core.platform.SensorManager
import org.koin.compose.koinInject

@Composable
fun PanicSensorListener(
    enabled: Boolean,
    onPanic: () -> Unit
) {
    val sensorManager: SensorManager = koinInject()
    DisposableEffect(enabled) {
        if (enabled) {
            sensorManager.startListening(onPanic)
        }
        onDispose {
            sensorManager.stopListening()
        }
    }
}
