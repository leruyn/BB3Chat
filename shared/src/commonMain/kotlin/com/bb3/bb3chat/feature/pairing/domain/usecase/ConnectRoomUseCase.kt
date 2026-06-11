package com.bb3.bb3chat.feature.pairing.domain.usecase

import com.bb3.bb3chat.core.crypto.RoomKeyDeriver
import com.bb3.bb3chat.core.crypto.SessionManager
import com.bb3.bb3chat.feature.messaging.domain.repository.MessageRepository
import com.bb3.bb3chat.feature.pairing.domain.repository.PairingSessionRepository

class ConnectRoomUseCase(
    private val messageRepository: MessageRepository,
    private val sessionManager: SessionManager,
    private val pairingSessionRepository: PairingSessionRepository
) {
    /**
     * @param hostCode When set, this device is the joiner (scanner) and will notify
     *                 the QR host via [PairingSessionRepository.announceJoin].
     */
    suspend operator fun invoke(
        roomId: String,
        myCode: String,
        peerCode: String,
        hostCode: String? = null
    ): String {
        val roomKey = RoomKeyDeriver.derive(myCode, peerCode)
        sessionManager.setRoomKey(roomId, roomKey)
        messageRepository.ensureRoomJoined(roomId, displayAlias = "Phòng $peerCode")
        if (hostCode != null) {
            pairingSessionRepository.announceJoin(
                hostCode  = hostCode,
                peerCode  = myCode,
                roomId    = roomId
            )
        }
        return roomId
    }
}
