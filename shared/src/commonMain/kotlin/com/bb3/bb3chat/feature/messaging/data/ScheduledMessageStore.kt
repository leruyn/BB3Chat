package com.bb3.bb3chat.feature.messaging.data

import com.bb3.bb3chat.core.storage.KeyValueStorage
import com.bb3.bb3chat.core.storage.StorageKeys
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlin.random.Random

@Serializable
data class ScheduledMessage(
    val id: String,
    val roomId: String,
    val plainText: String,
    val fireAtMs: Long
)

class ScheduledMessageStore(private val storage: KeyValueStorage) {

    private val json = Json { ignoreUnknownKeys = true }

    fun listForRoom(roomId: String): List<ScheduledMessage> =
        loadAll().filter { it.roomId == roomId }

    fun schedule(roomId: String, plainText: String, fireAtMs: Long): ScheduledMessage {
        val item = ScheduledMessage(
            id        = "${Clock.System.now().toEpochMilliseconds()}_${Random.nextInt()}",
            roomId    = roomId,
            plainText = plainText.trim(),
            fireAtMs  = fireAtMs
        )
        save(loadAll() + item)
        return item
    }

    fun remove(id: String) {
        save(loadAll().filterNot { it.id == id })
    }

    fun due(nowMs: Long): List<ScheduledMessage> =
        loadAll().filter { it.fireAtMs <= nowMs }

    fun clearAll() {
        storage.remove(StorageKeys.SCHEDULED_MESSAGES_JSON)
    }

    private fun loadAll(): List<ScheduledMessage> {
        val raw = storage.getString(StorageKeys.SCHEDULED_MESSAGES_JSON) ?: return emptyList()
        return runCatching { json.decodeFromString<List<ScheduledMessage>>(raw) }
            .getOrDefault(emptyList())
    }

    private fun save(items: List<ScheduledMessage>) {
        storage.putString(StorageKeys.SCHEDULED_MESSAGES_JSON, json.encodeToString(items))
    }
}
