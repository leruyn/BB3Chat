package com.bb3.bb3chat.core.bridge

import com.bb3.bb3chat.core.crypto.SessionManager
import com.bb3.bb3chat.core.platform.LocalNotificationBridgeHolder
import com.bb3.bb3chat.feature.messaging.domain.repository.MessageRepository

/** Swift PushHandler forwards FCM data payloads into Koin. */
object PushBridge {
    private var messageRepository: MessageRepository? = null
    private var sessionManager: SessionManager? = null

    fun bind(messageRepository: MessageRepository, sessionManager: SessionManager) {
        this.messageRepository = messageRepository
        this.sessionManager = sessionManager
    }

    fun hasActiveSession(): Boolean = sessionManager?.hasActiveSession == true

    suspend fun onNewMessagePush() {
        val repo = messageRepository ?: return
        val session = sessionManager
        if (session?.hasActiveSession == true) {
            repo.syncInboxFromRemote()
        } else {
            LocalNotificationBridgeHolder.show(
                title = "Thông báo",
                body = "Có tin nhắn mới — mở ứng dụng để đọc"
            )
        }
    }
}
