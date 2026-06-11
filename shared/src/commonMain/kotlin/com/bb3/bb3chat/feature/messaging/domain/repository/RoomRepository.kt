package com.bb3.bb3chat.feature.messaging.domain.repository

import com.bb3.bb3chat.feature.messaging.domain.model.InboxRoom
import kotlinx.coroutines.flow.Flow

interface RoomRepository {
    fun observeActiveRooms(): Flow<List<InboxRoom>>
    suspend fun syncInboxFromRemote()
}
