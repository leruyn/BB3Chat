package com.bb3.bb3chat.feature.security.domain.usecase

import com.bb3.bb3chat.core.security.SecurityPreferences
import kotlinx.datetime.Clock

class CheckDeadmanUseCase(
    private val securityPrefs: SecurityPreferences,
    private val triggerPanic: TriggerLocalPanicUseCase
) {
    /** Returns true if dead-man threshold was exceeded and panic was triggered. */
    operator fun invoke(): Boolean {
        if (!securityPrefs.isDeadmanEnabled()) return false

        val last = securityPrefs.getLastActivityMs()
        val now  = Clock.System.now().toEpochMilliseconds()
        if (last == 0L) {
            securityPrefs.recordActivity(now)
            return false
        }

        val elapsed = now - last
        if (elapsed >= securityPrefs.getDeadmanTriggerMs()) {
            triggerPanic()
            return true
        }
        return false
    }
}
