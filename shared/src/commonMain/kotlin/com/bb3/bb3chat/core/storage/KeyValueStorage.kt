package com.bb3.bb3chat.core.storage

interface KeyValueStorage {
    fun putString(key: String, value: String)
    fun getString(key: String): String?
    fun putLong(key: String, value: Long)
    fun getLong(key: String, default: Long = 0L): Long
    fun putInt(key: String, value: Int)
    fun getInt(key: String, default: Int = 0): Int
    fun putBoolean(key: String, value: Boolean)
    fun getBoolean(key: String, default: Boolean = false): Boolean
    fun remove(key: String)
    fun clearAll()
}

object StorageKeys {
    const val SETUP_PENDING_REAL_HASH = "setup_pending_real_hash"
    const val REAL_PIN_HASH       = "real_pin_hash"
    const val REAL_PIN_IV         = "real_pin_iv"
    const val DECOY_PIN_HASH      = "decoy_pin_hash"
    const val DECOY_PIN_IV        = "decoy_pin_iv"
    const val SECRET_DISGUISE_CODE = "secret_disguise_code"
    const val DISGUISE_APP_TYPE   = "disguise_app_type"
    const val USER_ALIAS          = "user_alias"
    const val USER_AVATAR_INDEX   = "user_avatar_index"
    const val FCM_TOKEN_ENCRYPTED = "fcm_token_enc"
    const val FCM_TOKEN_IV        = "fcm_token_iv"
    const val LAST_HEARTBEAT_MS   = "last_heartbeat_ms"
    const val PANIC_FLIP_ENABLED  = "panic_flip_enabled"
    const val PANIC_WIPE_ENABLED  = "panic_wipe_enabled"
    const val DEADMAN_ENABLED     = "deadman_enabled"
    const val DEADMAN_TRIGGER_MS  = "deadman_trigger_ms"
    const val ONBOARDING_DONE     = "onboarding_done"

    // Safe Hours
    const val SAFE_HOURS_ENABLED  = "safe_hours_enabled"
    const val SAFE_HOURS_START    = "safe_hours_start"   // Int 0..23
    const val SAFE_HOURS_END      = "safe_hours_end"     // Int 0..23

    // VIP
    const val VIP_TIER            = "vip_tier"
    const val VIP_OWNED_PLANS     = "vip_owned_plans"

    // Intruder capture
    const val INTRUDER_PHOTO_PREFIX     = "intruder_photo_"
    const val INTRUDER_PHOTO_IV_PREFIX  = "intruder_photo_iv_"
    const val INTRUDER_PHOTO_KEY_PREFIX = "intruder_photo_key_"
    const val INTRUDER_LAST_TS         = "intruder_last_ts"
    const val INTRUDER_ATTEMPT_COUNT   = "intruder_attempt_count"
    const val INTRUDER_SEEN            = "intruder_seen"
}
