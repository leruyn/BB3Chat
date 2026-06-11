package com.bb3.bb3chat.core.bridge

import com.bb3.bb3chat.core.crypto.SessionManager
import com.bb3.bb3chat.db.BB3Database

/** Swift PushHandler reads the active SQLDelight database for silent-push cleanup. */
object SessionManagerHolder {
    private var sessionManager: SessionManager? = null

    fun bind(sessionManager: SessionManager) {
        this.sessionManager = sessionManager
    }

    val database: BB3Database?
        get() {
            val sm = sessionManager ?: return null
            if (!sm.hasActiveSession) return null
            return runCatching { sm.requireDatabase() }.getOrNull()
        }
}
