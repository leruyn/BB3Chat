package com.bb3.bb3chat.core.db

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.bb3.bb3chat.db.BB3Database
import net.sqlcipher.database.SupportFactory

actual class DatabaseDriverFactory(private val context: Context) {

    actual fun createDriver(encryptionKey: ByteArray): SqlDriver {
        val factory = SupportFactory(encryptionKey)
        return AndroidSqliteDriver(
            schema   = BB3Database.Schema,
            context  = context,
            name     = "bb3_real.db",
            factory  = factory
        )
    }

    actual fun createDecoyDriver(): SqlDriver {
        // Decoy DB — vỏ rỗng, không mã hóa, không chứa dữ liệu thật
        return AndroidSqliteDriver(
            schema  = BB3Database.Schema,
            context = context,
            name    = "bb3_decoy.db"
        )
    }
}
