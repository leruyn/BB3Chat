package com.bb3.bb3chat.feature.pairing.domain.repository

import com.bb3.bb3chat.feature.pairing.domain.model.RoomCodeMatch
import kotlinx.coroutines.flow.Flow

interface RoomCodeLobbyRepository {
    /** Join lobby; returns match immediately if second peer already connected. */
    suspend fun joinOrWait(phrase: String, myCode: String): RoomCodeMatch?

    fun observeMatch(phrase: String, myCode: String): Flow<RoomCodeMatch>

    suspend fun clearLobby(phrase: String)
}
