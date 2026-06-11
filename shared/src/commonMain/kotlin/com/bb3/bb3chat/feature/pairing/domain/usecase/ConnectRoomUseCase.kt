package com.bb3.bb3chat.feature.pairing.domain.usecase

import com.bb3.bb3chat.core.crypto.RoomKeyDeriver
import com.bb3.bb3chat.core.crypto.SessionManager
import com.bb3.bb3chat.feature.messaging.domain.repository.MessageRepository

class ConnectRoomUseCase(
    private val messageRepository: MessageRepository,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(
        roomId: String,
        myCode: String,
        peerCode: String
    ): String {
        val roomKey = RoomKeyDeriver.derive(myCode, peerCode)
        sessionManager.setRoomKey(roomId, roomKey)
        messageRepository.ensureRoomJoined(roomId, displayAlias = "Phòng $peerCode")
        return roomId
    }
}
