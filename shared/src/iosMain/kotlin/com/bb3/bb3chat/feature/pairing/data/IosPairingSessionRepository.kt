package com.bb3.bb3chat.feature.pairing.data

import com.bb3.bb3chat.core.bridge.AuthBridgeHolder
import com.bb3.bb3chat.feature.messaging.data.FirestoreBridgeHolder
import com.bb3.bb3chat.feature.pairing.domain.model.PairingPeerJoined
import com.bb3.bb3chat.feature.pairing.domain.repository.PairingSessionRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class IosPairingSessionRepository : PairingSessionRepository {

    override suspend fun registerHost(hostCode: String) {
        if (!AuthBridgeHolder.ensureSignedIn()) {
            throw IllegalStateException("Firebase Auth chưa sẵn sàng")
        }
        val uid = AuthBridgeHolder.getCurrentUid()
            ?: throw IllegalStateException("Firebase Auth chưa sẵn sàng")
        runCatching {
            awaitVoid { onSuccess, onError ->
                FirestoreBridgeHolder.clearPairingSession(hostCode, onSuccess, onError)
            }
        }
        awaitVoid { onSuccess, onError ->
            FirestoreBridgeHolder.registerPairingHost(hostCode, uid, onSuccess, onError)
        }
    }

    override suspend fun announceJoin(hostCode: String, peerCode: String, roomId: String) {
        if (!AuthBridgeHolder.ensureSignedIn()) {
            throw IllegalStateException("Firebase Auth chưa sẵn sàng")
        }
        val uid = AuthBridgeHolder.getCurrentUid()
            ?: throw IllegalStateException("Firebase Auth chưa sẵn sàng")
        var lastError: Exception? = null
        repeat(ANNOUNCE_RETRIES) { attempt ->
            val result = runCatching {
                awaitVoid { onSuccess, onError ->
                    FirestoreBridgeHolder.announcePairingJoin(
                        hostCode, peerCode.uppercase(), roomId, uid, onSuccess, onError
                    )
                }
            }
            if (result.isSuccess) return
            lastError = result.exceptionOrNull() as? Exception
            if (attempt < ANNOUNCE_RETRIES - 1) delay(ANNOUNCE_RETRY_MS)
        }
        throw lastError ?: IllegalStateException("Không thể thông báo cho máy tạo mã")
    }

    override fun observePeerJoin(hostCode: String): Flow<PairingPeerJoined> = callbackFlow {
        val cancel = FirestoreBridgeHolder.observePairingSession(hostCode) { json ->
            val joined = PairingSessionJson.parseConnected(json) ?: return@observePairingSession
            trySend(joined)
        }
        awaitClose { cancel() }
    }

    override suspend fun getPeerJoinIfConnected(hostCode: String): PairingPeerJoined? {
        val json = awaitString { onResult, onError ->
            FirestoreBridgeHolder.getPairingSessionJson(hostCode, onResult, onError)
        }
        return PairingSessionJson.parseConnected(json)
    }

    override suspend fun clearSession(hostCode: String) {
        runCatching {
            awaitVoid { onSuccess, onError ->
                FirestoreBridgeHolder.clearPairingSession(hostCode, onSuccess, onError)
            }
        }
    }

    private suspend fun awaitVoid(
        block: (onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit
    ) = suspendCancellableCoroutine { cont ->
        block(
            { cont.resume(Unit) },
            { err -> cont.resumeWithException(IllegalStateException(err)) }
        )
    }

    private suspend fun awaitString(
        block: (onResult: (String) -> Unit, onError: (String) -> Unit) -> Unit
    ): String = suspendCancellableCoroutine { cont ->
        block(
            { cont.resume(it) },
            { err -> cont.resumeWithException(IllegalStateException(err)) }
        )
    }

    companion object {
        private const val ANNOUNCE_RETRIES = 8
        private const val ANNOUNCE_RETRY_MS = 500L
    }
}
