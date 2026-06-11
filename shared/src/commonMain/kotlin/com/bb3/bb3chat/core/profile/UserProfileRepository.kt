package com.bb3.bb3chat.core.profile

import com.bb3.bb3chat.core.crypto.SessionManager
import com.bb3.bb3chat.core.storage.KeyValueStorage
import com.bb3.bb3chat.core.storage.StorageKeys

class UserProfileRepository(
    private val storage: KeyValueStorage,
    private val sessionManager: SessionManager
) {
    fun getAlias(): String =
        storage.getString(StorageKeys.USER_ALIAS)
            ?: runCatching {
                sessionManager.requireDatabase()
                    .appConfigQueries.selectByKey("user_alias")
                    .executeAsOneOrNull()
            }.getOrNull()
            ?: ""

    fun getAvatarIndex(): Int =
        storage.getInt(StorageKeys.USER_AVATAR_INDEX, 0)

    fun isProfileConfigured(): Boolean =
        storage.getBoolean(StorageKeys.PROFILE_CONFIGURED)

    fun isOnboardingDone(): Boolean =
        storage.getBoolean(StorageKeys.ONBOARDING_DONE)

    fun setOnboardingDone() {
        storage.putBoolean(StorageKeys.ONBOARDING_DONE, true)
    }

    suspend fun saveProfile(alias: String, avatarIndex: Int) {
        val trimmed = alias.trim()
        require(trimmed.length >= 2) { "Biệt danh quá ngắn" }
        storage.putString(StorageKeys.USER_ALIAS, trimmed)
        storage.putInt(StorageKeys.USER_AVATAR_INDEX, avatarIndex.coerceIn(0, 7))
        storage.putBoolean(StorageKeys.PROFILE_CONFIGURED, true)
        runCatching {
            sessionManager.requireDatabase()
                .appConfigQueries.upsert(key = "user_alias", value = trimmed)
        }
        runCatching {
            sessionManager.setCurrentUserId(trimmed)
        }
    }
}
