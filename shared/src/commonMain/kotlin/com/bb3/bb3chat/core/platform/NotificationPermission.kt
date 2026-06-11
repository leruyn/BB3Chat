package com.bb3.bb3chat.core.platform

expect class NotificationPermission {
    fun areGranted(): Boolean
    suspend fun ensureGranted(): Boolean
}
