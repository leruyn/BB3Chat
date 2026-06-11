package com.bb3.bb3chat.core.security

import com.bb3.bb3chat.core.storage.KeyValueStorage
import com.bb3.bb3chat.core.storage.StorageKeys

class SecurityPreferences(private val storage: KeyValueStorage) {

    fun isSafeHoursEnabled(): Boolean =
        storage.getBoolean(StorageKeys.SAFE_HOURS_ENABLED)

    fun setSafeHoursEnabled(enabled: Boolean) {
        storage.putBoolean(StorageKeys.SAFE_HOURS_ENABLED, enabled)
    }

    fun getSafeHoursStart(): Int =
        storage.getInt(StorageKeys.SAFE_HOURS_START, DEFAULT_START)

    fun getSafeHoursEnd(): Int =
        storage.getInt(StorageKeys.SAFE_HOURS_END, DEFAULT_END)

    fun setSafeHoursRange(startHour: Int, endHour: Int) {
        storage.putInt(StorageKeys.SAFE_HOURS_START, startHour.coerceIn(0, 23))
        storage.putInt(StorageKeys.SAFE_HOURS_END, endHour.coerceIn(0, 23))
    }

    fun isPanicFlipEnabled(): Boolean =
        storage.getBoolean(StorageKeys.PANIC_FLIP_ENABLED)

    fun setPanicFlipEnabled(enabled: Boolean) {
        storage.putBoolean(StorageKeys.PANIC_FLIP_ENABLED, enabled)
    }

    fun isPanicWipeEnabled(): Boolean =
        storage.getBoolean(StorageKeys.PANIC_WIPE_ENABLED)

    fun setPanicWipeEnabled(enabled: Boolean) {
        storage.putBoolean(StorageKeys.PANIC_WIPE_ENABLED, enabled)
    }

    fun isIntruderBannerSeen(): Boolean =
        storage.getBoolean(StorageKeys.INTRUDER_SEEN)

    fun markIntruderBannerSeen() {
        storage.putBoolean(StorageKeys.INTRUDER_SEEN, true)
    }

    fun clearIntruderBannerSeen() {
        storage.putBoolean(StorageKeys.INTRUDER_SEEN, false)
    }

    fun isFakePushEnabled(): Boolean =
        storage.getBoolean(StorageKeys.FAKE_PUSH_ENABLED)

    fun setFakePushEnabled(enabled: Boolean) {
        storage.putBoolean(StorageKeys.FAKE_PUSH_ENABLED, enabled)
    }

    fun isDeadmanEnabled(): Boolean =
        storage.getBoolean(StorageKeys.DEADMAN_ENABLED)

    fun setDeadmanEnabled(enabled: Boolean) {
        storage.putBoolean(StorageKeys.DEADMAN_ENABLED, enabled)
    }

    fun getDeadmanTriggerMs(): Long =
        storage.getLong(StorageKeys.DEADMAN_TRIGGER_MS, DEFAULT_DEADMAN_MS)

    fun setDeadmanTriggerHours(hours: Int) {
        val h = hours.coerceIn(12, 72)
        storage.putLong(StorageKeys.DEADMAN_TRIGGER_MS, h * 3_600_000L)
    }

    fun getDeadmanTriggerHours(): Int =
        (getDeadmanTriggerMs() / 3_600_000L).toInt().coerceIn(12, 72)

    fun getLastActivityMs(): Long =
        storage.getLong(StorageKeys.LAST_ACTIVITY_MS)

    fun recordActivity(atMs: Long = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()) {
        storage.putLong(StorageKeys.LAST_ACTIVITY_MS, atMs)
    }

    companion object {
        const val DEFAULT_START = 8
        const val DEFAULT_END   = 22
        const val DEFAULT_DEADMAN_MS = 36L * 3_600_000L
    }
}

/** In-memory bypass for the current app session (cleared on panic / disguise). */
object SafeHoursSession {
    var emergencyBypass: Boolean = false

    fun reset() {
        emergencyBypass = false
    }
}
