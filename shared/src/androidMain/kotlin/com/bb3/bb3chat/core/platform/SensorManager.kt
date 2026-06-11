package com.bb3.bb3chat.core.platform

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager as AndroidSensorManager
import kotlin.math.sqrt

private const val FACE_DOWN_THRESHOLD = -8.0f   // z < -8 m/s² = úp màn hình xuống
private const val SHAKE_THRESHOLD     = 25.0f   // magnitude > 25 m/s² = lắc mạnh

actual class SensorManager(private val context: Context) {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as AndroidSensorManager

    private var eventListener: SensorEventListener? = null
    private var lastPanicTime = 0L
    private val panicCooldownMs = 3_000L

    actual fun startListening(onPanicTrigger: () -> Unit) {
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return

        eventListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                val magnitude = sqrt(x * x + y * y + z * z)
                val isFaceDown = z < FACE_DOWN_THRESHOLD
                val isShake    = magnitude > SHAKE_THRESHOLD

                if (isFaceDown || isShake) {
                    val now = System.currentTimeMillis()
                    if (now - lastPanicTime > panicCooldownMs) {
                        lastPanicTime = now
                        onPanicTrigger()
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(
            eventListener,
            accelerometer,
            AndroidSensorManager.SENSOR_DELAY_NORMAL
        )
    }

    actual fun stopListening() {
        eventListener?.let { sensorManager.unregisterListener(it) }
        eventListener = null
    }
}
