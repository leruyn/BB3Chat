package com.bb3.bb3chat.core.db

import app.cash.sqldelight.db.SqlDriver

expect class DatabaseDriverFactory {
    fun createDriver(encryptionKey: ByteArray): SqlDriver
    fun createDecoyDriver(): SqlDriver
}
