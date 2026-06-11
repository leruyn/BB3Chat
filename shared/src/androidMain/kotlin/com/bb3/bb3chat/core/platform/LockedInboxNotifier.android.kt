package com.bb3.bb3chat.core.platform

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

private const val LOCKED_CHANNEL_ID = "bb3_locked_inbox"

object LockedInboxNotifier {

    fun show(context: Context) {
        val appContext = context.applicationContext
        val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(
                    LOCKED_CHANNEL_ID,
                    "Tin nhắn mới",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }

        val smallIcon = appContext.resources
            .getIdentifier("ic_stat_notify", "drawable", appContext.packageName)
            .takeIf { it != 0 }
            ?: android.R.drawable.ic_dialog_info

        val notification = NotificationCompat.Builder(appContext, LOCKED_CHANNEL_ID)
            .setSmallIcon(smallIcon)
            .setContentTitle("Thông báo")
            .setContentText("Có tin nhắn mới — mở ứng dụng để đọc")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        nm.notify(LOCKED_INBOX_NOTIFICATION_ID, notification)
    }

    private const val LOCKED_INBOX_NOTIFICATION_ID = 42_001
}
