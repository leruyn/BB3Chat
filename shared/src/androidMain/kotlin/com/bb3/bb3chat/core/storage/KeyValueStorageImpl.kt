package com.bb3.bb3chat.core.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class KeyValueStorageImpl(context: Context) : KeyValueStorage {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "bb3_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override fun putString(key: String, value: String) =
        prefs.edit().putString(key, value).apply()

    override fun getString(key: String): String? = prefs.getString(key, null)

    override fun putLong(key: String, value: Long) =
        prefs.edit().putLong(key, value).apply()

    override fun getLong(key: String, default: Long): Long = prefs.getLong(key, default)

    override fun putInt(key: String, value: Int) =
        prefs.edit().putInt(key, value).apply()

    override fun getInt(key: String, default: Int): Int =
        prefs.getInt(key, default)

    override fun putBoolean(key: String, value: Boolean) =
        prefs.edit().putBoolean(key, value).apply()

    override fun getBoolean(key: String, default: Boolean): Boolean =
        prefs.getBoolean(key, default)

    override fun remove(key: String) = prefs.edit().remove(key).apply()

    override fun clearAll() = prefs.edit().clear().apply()
}
