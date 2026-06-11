package com.bb3.bb3chat.feature.token.data

import com.bb3.bb3chat.feature.token.domain.repository.FcmTokenRegistrar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AndroidFcmTokenRegistrar(
    private val firestore: FirebaseFirestore
) : FcmTokenRegistrar {

    override suspend fun registerToken(rawToken: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        firestore.collection("fcm_tokens")
            .document(uid)
            .set(
                mapOf(
                    "token"     to rawToken,
                    "platform"  to "android",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            .await()
    }
}
