package com.bb3.bb3chat.feature.security.domain.usecase

import com.bb3.bb3chat.core.crypto.SessionManager
import com.bb3.bb3chat.core.security.SecurityPreferences
import com.bb3.bb3chat.feature.messaging.data.ScheduledMessageStore

class ExecutePanicUseCase(
    private val securityPrefs: SecurityPreferences,
    private val sessionManager: SessionManager,
    private val triggerPanic: TriggerLocalPanicUseCase,
    private val captureIntruder: CaptureIntruderUseCase,
    private val scheduledStore: ScheduledMessageStore
) {
    operator fun invoke() {
        if (securityPrefs.isPanicWipeEnabled()) {
            runCatching {
                sessionManager.requireDatabase().messageQueries.deleteAllMessages()
            }
            captureIntruder.clearSnapshots()
            scheduledStore.clearAll()
        }
        triggerPanic()
    }
}
