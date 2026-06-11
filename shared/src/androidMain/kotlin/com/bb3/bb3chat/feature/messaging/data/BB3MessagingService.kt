package com.bb3.bb3chat.feature.messaging.data

import com.bb3.bb3chat.core.crypto.SessionManager
import com.bb3.bb3chat.feature.messaging.domain.repository.MessageRepository
import com.bb3.bb3chat.feature.token.domain.repository.TokenRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class BB3MessagingService : FirebaseMessagingService() {

    private val sessionManager: SessionManager by inject()
    private val messageRepository: MessageRepository by inject()
    private val tokenRepository: TokenRepository by inject()

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        if (remoteMessage.notification != null) return

        val action = remoteMessage.data["action"] ?: return
        val roomId = remoteMessage.data["roomId"]

        when (action) {
            "SELF_DESTRUCT" -> {
                if (roomId != null) handleSelfDestruct(roomId)
            }
            "ROOM_DESTRUCT" -> {
                if (roomId != null) handleRoomDestruct(roomId)
            }
            "NEW_MESSAGE" -> {
                if (sessionManager.hasActiveSession) {
                    serviceScope.launch {
                        runCatching { messageRepository.syncInboxFromRemote() }
                    }
                }
            }
            "TOKEN_PING" -> {
                serviceScope.launch { tokenRepository.sendHeartbeat() }
            }
        }
    }

    override fun onNewToken(token: String) {
        serviceScope.launch { tokenRepository.onTokenRefreshed(token) }
    }

    private fun handleSelfDestruct(roomId: String) {
        if (!sessionManager.hasActiveSession) return
        runCatching {
            val db = sessionManager.requireDatabase()
            db.messageQueries.deleteByRoomId(roomId)
        }
    }

    private fun handleRoomDestruct(roomId: String) {
        if (!sessionManager.hasActiveSession) return
        runCatching {
            val db = sessionManager.requireDatabase()
            db.messageQueries.deleteByRoomId(roomId)
            db.chatRoomQueries.deactivateRoom(roomId)
        }
    }
}
