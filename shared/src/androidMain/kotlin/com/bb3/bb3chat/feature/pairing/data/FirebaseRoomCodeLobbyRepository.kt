package com.bb3.bb3chat.feature.pairing.data

import com.bb3.bb3chat.core.crypto.RoomIdDeriver
import com.bb3.bb3chat.feature.pairing.domain.RoomCodePhraseNormalizer
import com.bb3.bb3chat.core.firebase.FirebaseAuthInitializer
import com.bb3.bb3chat.feature.pairing.domain.model.RoomCodeMatch
import com.bb3.bb3chat.feature.pairing.domain.repository.RoomCodeLobbyRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseRoomCodeLobbyRepository(
    private val firestore: FirebaseFirestore
) : RoomCodeLobbyRepository {

    override suspend fun joinOrWait(phrase: String, myCode: String): RoomCodeMatch? {
        FirebaseAuthInitializer.ensureSignedIn()
        val uid   = FirebaseAuth.getInstance().currentUser?.uid
            ?: throw IllegalStateException("Firebase Auth chưa sẵn sàng")
        val docId = RoomCodePhraseNormalizer.normalize(phrase)
        val doc   = firestore.collection(COLLECTION).document(docId)
        val snap  = doc.get().await()

        if (!snap.exists()) {
            doc.set(
                mapOf(
                    "peer1Uid"  to uid,
                    "peer1Code" to myCode.uppercase(),
                    "status"    to STATUS_WAITING,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            return null
        }

        val data = snap.data ?: return null
        return when (data["status"]) {
            STATUS_CONNECTED -> parseMatch(data, myCode)
            STATUS_WAITING   -> {
                val peer1Uid  = data["peer1Uid"] as? String ?: return null
                val peer1Code = data["peer1Code"] as? String ?: return null
                if (peer1Uid == uid) return null
                val roomId = RoomIdDeriver.derive(peer1Code, myCode.uppercase())
                doc.update(
                    mapOf(
                        "peer2Uid"    to uid,
                        "peer2Code"   to myCode.uppercase(),
                        "roomId"      to roomId,
                        "status"      to STATUS_CONNECTED,
                        "connectedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                RoomCodeMatch(roomId = roomId, myCode = myCode.uppercase(), peerCode = peer1Code)
            }
            else -> null
        }
    }

    override fun observeMatch(phrase: String, myCode: String): Flow<RoomCodeMatch> = callbackFlow {
        val docId = RoomCodePhraseNormalizer.normalize(phrase)
        val listener = firestore.collection(COLLECTION).document(docId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                parseMatch(snapshot?.data, myCode)?.let { trySend(it) }
            }
        awaitClose { listener.remove() }
    }

    override suspend fun clearLobby(phrase: String) {
        runCatching {
            firestore.collection(COLLECTION)
                .document(RoomCodePhraseNormalizer.normalize(phrase))
                .delete()
                .await()
        }
    }

    private fun parseMatch(data: Map<String, Any>?, myCode: String): RoomCodeMatch? {
        if (data == null || data["status"] != STATUS_CONNECTED) return null
        val roomId = data["roomId"] as? String ?: return null
        val peer1  = data["peer1Code"] as? String ?: return null
        val peer2  = data["peer2Code"] as? String ?: return null
        val upper  = myCode.uppercase()
        val peer   = when (upper) {
            peer1.uppercase() -> peer2
            peer2.uppercase() -> peer1
            else              -> if (peer2.isNotEmpty()) peer1 else return null
        }
        return RoomCodeMatch(roomId = roomId, myCode = upper, peerCode = peer.uppercase())
    }

    companion object {
        private const val COLLECTION    = "room_code_lobby"
        private const val STATUS_WAITING   = "WAITING"
        private const val STATUS_CONNECTED = "CONNECTED"

    }
}
