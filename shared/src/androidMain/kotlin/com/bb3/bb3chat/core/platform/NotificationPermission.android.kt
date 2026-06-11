package com.bb3.bb3chat.core.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object NotificationPermissionHolder {
    private var requestFn: (suspend () -> Boolean)? = null

    fun register(request: suspend () -> Boolean) {
        requestFn = request
    }

    suspend fun requestFromActivity(): Boolean =
        requestFn?.invoke() ?: false
}

actual class NotificationPermission(private val context: Context) {

    actual fun areGranted(): Boolean {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    actual suspend fun ensureGranted(): Boolean {
        if (areGranted()) return true
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
        return NotificationPermissionHolder.requestFromActivity()
    }
}
