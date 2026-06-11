package com.bb3.bb3chat.feature.messaging.domain.usecase

import com.bb3.bb3chat.feature.messaging.domain.repository.MessageRepository
import com.bb3.bb3chat.feature.messaging.domain.repository.RoomStatus

sealed class OpenChatroomResult {
    object Ready     : OpenChatroomResult()
    object Destroyed : OpenChatroomResult()
}

class OpenChatroomUseCase(private val repository: MessageRepository) {
    suspend operator fun invoke(roomId: String): OpenChatroomResult {
        val status = repository.checkRoomStatus(roomId)
        if (status == RoomStatus.PENDING_DESTRUCT || status == RoomStatus.DESTROYED) {
            repository.destroyLocalRoom(roomId)
            return OpenChatroomResult.Destroyed
        }
        return OpenChatroomResult.Ready
    }
}
