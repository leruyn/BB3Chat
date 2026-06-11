package com.bb3.bb3chat.core.platform

actual class NotificationPermission {

    actual fun areGranted(): Boolean =
        LocalNotificationBridgeHolder.areNotificationsGranted()

    actual suspend fun ensureGranted(): Boolean =
        LocalNotificationBridgeHolder.ensureNotificationsGranted()
}
