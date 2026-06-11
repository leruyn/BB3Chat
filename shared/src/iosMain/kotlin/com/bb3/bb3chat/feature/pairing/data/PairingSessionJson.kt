package com.bb3.bb3chat.feature.pairing.data

import com.bb3.bb3chat.feature.pairing.domain.model.PairingPeerJoined
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal object PairingSessionJson {
    private val json = Json { ignoreUnknownKeys = true }

    fun parseConnected(raw: String): PairingPeerJoined? {
        if (raw == "null") return null
        val obj = runCatching { json.parseToJsonElement(raw).jsonObject }.getOrNull() ?: return null
        if (obj["status"]?.jsonPrimitive?.content != "CONNECTED") return null
        val peerCode = obj["peerCode"]?.jsonPrimitive?.content ?: return null
        val roomId   = obj["roomId"]?.jsonPrimitive?.content ?: return null
        return PairingPeerJoined(peerCode = peerCode, roomId = roomId)
    }
}
