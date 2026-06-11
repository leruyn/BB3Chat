package com.bb3.bb3chat.feature.pairing.domain.repository

import com.bb3.bb3chat.feature.pairing.domain.model.PairingPeerJoined
import kotlinx.coroutines.flow.Flow

interface PairingSessionRepository {
    /** Host (QR displayer) registers a wait session keyed by [hostCode]. */
    suspend fun registerHost(hostCode: String)

    /** Joiner (scanner) notifies the host that pairing is complete. */
    suspend fun announceJoin(hostCode: String, peerCode: String, roomId: String)

    /** Host listens for a peer joining via [hostCode]. */
    fun observePeerJoin(hostCode: String): Flow<PairingPeerJoined>

    /** One-shot read — polling fallback when the snapshot listener misses an update. */
    suspend fun getPeerJoinIfConnected(hostCode: String): PairingPeerJoined?

    suspend fun clearSession(hostCode: String)
}
