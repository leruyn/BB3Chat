package com.bb3.bb3chat.feature.pairing.data

import com.bb3.bb3chat.core.bridge.AuthBridgeHolder
import com.bb3.bb3chat.core.crypto.RoomIdDeriver
import com.bb3.bb3chat.feature.messaging.data.FirestoreBridgeHolder
import com.bb3.bb3chat.feature.pairing.domain.RoomCodePhraseNormalizer
import com.bb3.bb3chat.feature.pairing.domain.model.RoomCodeMatch
import com.bb3.bb3chat.feature.pairing.domain.repository.RoomCodeLobbyRepository
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class IosRoomCodeLobbyRepository : RoomCodeLobbyRepository {

    override suspend fun joinOrWait(phrase: String, myCode: String): RoomCodeMatch? {
        if (!AuthBridgeHolder.ensureSignedIn()) {
            throw IllegalStateException("Firebase Auth chưa sẵn sàng")
        }
        val uid    = AuthBridgeHolder.getCurrentUid()
            ?: throw IllegalStateException("Firebase Auth chưa sẵn sàng")
        val docId  = RoomCodePhraseNormalizer.normalize(phrase)
        val upper  = myCode.uppercase()
        val json   = awaitString { onResult, onError ->
            FirestoreBridgeHolder.getRoomCodeLobbyJson(docId, onResult, onError)
        }
        if (json == "null") {
            awaitVoid { onSuccess, onError ->
                FirestoreBridgeHolder.createRoomCodeLobbyWaiting(docId, uid, upper, onSuccess, onError)
            }
            return null
        }
        val existing = RoomCodeLobbyJson.parseConnected(json, upper)
        if (existing != null) return existing
        val obj = runCatching {
            kotlinx.serialization.json.Json.parseToJsonElement(json).jsonObject
        }.getOrNull() ?: return null
        if (obj["status"]?.jsonPrimitive?.content != "WAITING") return null
        val peer1Uid = obj["peer1Uid"]?.jsonPrimitive?.content ?: return null
        val peer1Code = obj["peer1Code"]?.jsonPrimitive?.content ?: return null
        if (peer1Uid == uid) return null
        val roomId = RoomIdDeriver.derive(peer1Code, upper)
        awaitVoid { onSuccess, onError ->
            FirestoreBridgeHolder.connectRoomCodeLobby(
                docId, uid, upper, roomId, onSuccess, onError
            )
        }
        return RoomCodeMatch(roomId = roomId, myCode = upper, peerCode = peer1Code)
    }

    override fun observeMatch(phrase: String, myCode: String): Flow<RoomCodeMatch> = callbackFlow {
        val docId = RoomCodePhraseNormalizer.normalize(phrase)
        val cancel = FirestoreBridgeHolder.observeRoomCodeLobby(docId) { json ->
            RoomCodeLobbyJson.parseConnected(json, myCode)?.let { trySend(it) }
        }
        awaitClose { cancel() }
    }

    override suspend fun clearLobby(phrase: String) {
        val docId = RoomCodePhraseNormalizer.normalize(phrase)
        runCatching {
            awaitVoid { onSuccess, onError ->
                FirestoreBridgeHolder.clearRoomCodeLobby(docId, onSuccess, onError)
            }
        }
    }

    private suspend fun awaitVoid(block: (onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit) =
        suspendCancellableCoroutine { cont ->
            block(
                { cont.resume(Unit) },
                { cont.resumeWithException(IllegalStateException(it)) }
            )
        }

    private suspend fun awaitString(block: (onResult: (String) -> Unit, onError: (String) -> Unit) -> Unit): String =
        suspendCancellableCoroutine { cont ->
            block(
                { cont.resume(it) },
                { cont.resumeWithException(IllegalStateException(it)) }
            )
        }
}
