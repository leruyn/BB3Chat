package com.bb3.bb3chat.core.disguise

import com.bb3.bb3chat.core.storage.KeyValueStorage
import com.bb3.bb3chat.core.storage.StorageKeys

class DisguiseConfig(private val storage: KeyValueStorage) {

    fun getSecretCode(): String =
        storage.getString(StorageKeys.SECRET_DISGUISE_CODE) ?: DEFAULT_SECRET_CODE

    fun setSecretCode(code: String) {
        storage.putString(StorageKeys.SECRET_DISGUISE_CODE, code.trim())
    }

    /** Persists the default disguise unlock code on first PIN setup. */
    fun ensureDefaultSecretCode() {
        if (storage.getString(StorageKeys.SECRET_DISGUISE_CODE).isNullOrBlank()) {
            storage.putString(StorageKeys.SECRET_DISGUISE_CODE, DEFAULT_SECRET_CODE)
        }
    }

    fun getDisguiseType(): DisguiseType =
        DisguiseType.fromStorage(storage.getString(StorageKeys.DISGUISE_APP_TYPE))

    fun setDisguiseType(type: DisguiseType) {
        storage.putString(StorageKeys.DISGUISE_APP_TYPE, type.storageKey)
    }

    companion object {
        const val DEFAULT_SECRET_CODE = "14022026"
    }
}
