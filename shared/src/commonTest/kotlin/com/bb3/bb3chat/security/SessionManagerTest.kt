package com.bb3.bb3chat.security

import com.bb3.bb3chat.core.crypto.SessionManager
import com.bb3.bb3chat.testutil.NoopSqlDriver
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SessionManagerTest {

    private fun makeSession(): SessionManager = SessionManager()

    private fun SessionManager.openWith(key: ByteArray) =
        openRealSession(key, NoopSqlDriver(), userId = "test-user")

    @Test
    fun `no active session before authentication`() {
        val sm = makeSession()
        assertFalse(sm.hasActiveSession)
        assertNull(sm.currentUserId)
    }

    @Test
    fun `requireSessionKey throws when not authenticated`() {
        val sm = makeSession()
        assertFails { sm.requireSessionKey() }
    }

    @Test
    fun `after openRealSession, requireSessionKey returns the key`() {
        val sm  = makeSession()
        val key = ByteArray(32) { it.toByte() }
        sm.openWith(key)
        assertTrue(sm.hasActiveSession)
        assertContentEquals(key, sm.requireSessionKey())
    }

    @Test
    fun `destroySessionKey zeros out the internal key copy`() {
        val sm  = makeSession()
        val key = ByteArray(32) { 0xFF.toByte() }
        sm.openWith(key)

        // openRealSession stores a copy — grab a reference to the internal array
        val internalKey = sm.requireSessionKey()
        sm.destroySessionKey()

        assertFalse(sm.hasActiveSession)
        assertNull(sm.currentUserId)

        // Internal copy must be wiped with zeros (in-place fill)
        assertTrue(
            internalKey.all { it == 0.toByte() },
            "Session key was not zeroed out in memory after destroySessionKey()"
        )
    }

    @Test
    fun `requireSessionKey throws after destroy`() {
        val sm = makeSession()
        sm.openWith(ByteArray(32))
        sm.destroySessionKey()
        assertFails { sm.requireSessionKey() }
    }
}
