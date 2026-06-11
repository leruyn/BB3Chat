package com.bb3.bb3chat.core.firebase

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

/**
 * Ensures an anonymous Firebase Auth session exists before any Firestore access.
 * Firestore security rules require [request.auth != null].
 */
object FirebaseAuthInitializer {

    suspend fun ensureSignedIn() {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            auth.signInAnonymously().await()
        }
    }
}
