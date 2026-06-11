package com.bb3.bb3chat.core.platform

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

private const val TAG = "FakePushNotifier"
private const val CHANNEL_ID = "bb3_benign_v2"

actual class FakePushNotifier(private val context: Context) {

    actual fun show(message: FakePushMessage) {
        val appContext = context.applicationContext
        if (!canPostNotifications(appContext)) {
            Log.w(TAG, "Notifications blocked: permission or app-level setting off")
            return
        }

        val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.deleteNotificationChannel("bb3_benign")
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Thông báo",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Thông báo ngụy trang BB3Chat"
                enableVibration(true)
            }
            nm.createNotificationChannel(channel)
        }

        val smallIcon = resolveSmallIcon(appContext)
        val notificationId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(smallIcon)
            .setContentTitle(message.title)
            .setContentText(message.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message.body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(appContext).notify(notificationId, notification)
        } catch (e: Exception) {
            Log.e(TAG, "notify failed", e)
        }
    }

    private fun resolveSmallIcon(ctx: Context): Int {
        val dedicated = ctx.resources.getIdentifier("ic_stat_notify", "drawable", ctx.packageName)
        if (dedicated != 0) return dedicated
        val foreground = ctx.resources.getIdentifier("ic_launcher_foreground", "drawable", ctx.packageName)
        if (foreground != 0) return foreground
        return android.R.drawable.ic_dialog_info
    }

    private fun canPostNotifications(ctx: Context): Boolean {
        if (!NotificationManagerCompat.from(ctx).areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            ctx,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }
}
