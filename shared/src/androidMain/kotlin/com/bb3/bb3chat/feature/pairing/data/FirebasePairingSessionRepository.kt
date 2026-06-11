package com.bb3.bb3chat.feature.pairing.data

import com.bb3.bb3chat.core.firebase.FirebaseAuthInitializer
import com.bb3.bb3chat.feature.pairing.domain.model.PairingPeerJoined
import com.bb3.bb3chat.feature.pairing.domain.repository.PairingSessionRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebasePairingSessionRepository(
    private val firestore: FirebaseFirestore
) : PairingSessionRepository {

    override suspend fun registerHost(hostCode: String) {
        FirebaseAuthInitializer.ensureSignedIn()
        val uid = FirebaseAuth.getInstance().currentUser?.uid
            ?: throw IllegalStateException("Firebase Auth chưa sẵn sàng")
        val doc = firestore.collection(COLLECTION).document(hostCode)
        // Stale CONNECTED/WAITING docs block set() — rules treat it as update, not create.
        runCatching { doc.delete().await() }
        doc.set(
            mapOf(
                "hostUid"   to uid,
                "status"    to STATUS_WAITING,
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    override suspend fun announceJoin(hostCode: String, peerCode: String, roomId: String) {
        FirebaseAuthInitializer.ensureSignedIn()
        val uid = FirebaseAuth.getInstance().currentUser?.uid
            ?: throw IllegalStateException("Firebase Auth chưa sẵn sàng")
        val payload = mapOf(
            "peerCode"    to peerCode.uppercase(),
            "roomId"      to roomId,
            "joinerUid"   to uid,
            "status"      to STATUS_CONNECTED,
            "connectedAt" to FieldValue.serverTimestamp()
        )
        val doc = firestore.collection(COLLECTION).document(hostCode)
        var lastError: Exception? = null
        repeat(ANNOUNCE_RETRIES) { attempt ->
            try {
                doc.update(payload).await()
                return
            } catch (e: Exception) {
                lastError = e
                if (attempt < ANNOUNCE_RETRIES - 1) delay(ANNOUNCE_RETRY_MS)
            }
        }
        throw lastError ?: IllegalStateException("Không thể thông báo cho máy tạo mã")
    }

    override fun observePeerJoin(hostCode: String): Flow<PairingPeerJoined> = callbackFlow {
        val listener = firestore.collection(COLLECTION).document(hostCode)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                parseConnected(snapshot?.data)?.let { trySend(it) }
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getPeerJoinIfConnected(hostCode: String): PairingPeerJoined? {
        val snapshot = firestore.collection(COLLECTION).document(hostCode).get().await()
        return parseConnected(snapshot.data)
    }

    override suspend fun clearSession(hostCode: String) {
        runCatching {
            firestore.collection(COLLECTION).document(hostCode).delete().await()
        }
    }

    private fun parseConnected(data: Map<String, Any>?): PairingPeerJoined? {
        if (data == null || data["status"] != STATUS_CONNECTED) return null
        val peerCode = data["peerCode"] as? String ?: return null
        val roomId   = data["roomId"] as? String ?: return null
        return PairingPeerJoined(peerCode = peerCode, roomId = roomId)
    }

    companion object {
        private const val COLLECTION = "pairing_sessions"
        private const val STATUS_WAITING   = "WAITING"
        private const val STATUS_CONNECTED = "CONNECTED"
        private const val ANNOUNCE_RETRIES = 8
        private const val ANNOUNCE_RETRY_MS = 500L
    }
}
