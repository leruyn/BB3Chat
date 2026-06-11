package com.bb3.bb3chat.core.storage

import platform.Foundation.NSUserDefaults

class KeyValueStorageImpl : KeyValueStorage {

    private val defaults = NSUserDefaults.standardUserDefaults

    override fun putString(key: String, value: String) = defaults.setObject(value, key)
    override fun getString(key: String): String?        = defaults.stringForKey(key)
    override fun putLong(key: String, value: Long)      = defaults.setInteger(value, key)
    override fun getLong(key: String, default: Long): Long =
        if (defaults.objectForKey(key) != null) defaults.integerForKey(key) else default
    override fun putInt(key: String, value: Int)        = defaults.setInteger(value.toLong(), key)
    override fun getInt(key: String, default: Int): Int =
        if (defaults.objectForKey(key) != null) defaults.integerForKey(key).toInt() else default
    override fun putBoolean(key: String, value: Boolean) = defaults.setBool(value, key)
    override fun getBoolean(key: String, default: Boolean): Boolean =
        if (defaults.objectForKey(key) != null) defaults.boolForKey(key) else default
    override fun remove(key: String)  = defaults.removeObjectForKey(key)
    override fun clearAll()           = defaults.dictionaryRepresentation().keys
        .filterIsInstance<String>().forEach { defaults.removeObjectForKey(it) }
}
