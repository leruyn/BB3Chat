package com.bb3.bb3chat.core.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import app.cash.sqldelight.driver.native.wrapConnection
import co.touchlab.sqliter.DatabaseConfiguration
import com.bb3.bb3chat.core.util.toHexLower
import com.bb3.bb3chat.db.BB3Database

actual class DatabaseDriverFactory {

    actual fun createDriver(encryptionKey: ByteArray): SqlDriver {
        val keyHex = encryptionKey.toHexLower()
        val config = DatabaseConfiguration(
            name = "bb3_real.db",
            version = BB3Database.Schema.version.toInt(),
            create = { connection ->
                wrapConnection(connection) { BB3Database.Schema.create(it) }
            },
            upgrade = { connection, oldVersion, newVersion ->
                wrapConnection(connection) {
                    BB3Database.Schema.migrate(it, oldVersion.toLong(), newVersion.toLong())
                }
            },
            extendedConfig = DatabaseConfiguration.Extended(
                foreignKeyConstraints = true
            ),
            encryptionConfig = DatabaseConfiguration.Encryption(
                key = "x'$keyHex'"
            )
        )
        return NativeSqliteDriver(config)
    }

    actual fun createDecoyDriver(): SqlDriver {
        return NativeSqliteDriver(BB3Database.Schema, "bb3_decoy.db")
    }
}
