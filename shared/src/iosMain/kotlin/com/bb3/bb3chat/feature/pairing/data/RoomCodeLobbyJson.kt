package com.bb3.bb3chat.feature.pairing.data

import com.bb3.bb3chat.feature.pairing.domain.model.RoomCodeMatch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal object RoomCodeLobbyJson {
    private val json = Json { ignoreUnknownKeys = true }

    fun parseConnected(raw: String, myCode: String): RoomCodeMatch? {
        if (raw == "null") return null
        val obj = runCatching { json.parseToJsonElement(raw).jsonObject }.getOrNull() ?: return null
        if (obj["status"]?.jsonPrimitive?.content != "CONNECTED") return null
        val roomId = obj["roomId"]?.jsonPrimitive?.content ?: return null
        val peer1  = obj["peer1Code"]?.jsonPrimitive?.content ?: return null
        val peer2  = obj["peer2Code"]?.jsonPrimitive?.content ?: return null
        val upper  = myCode.uppercase()
        val peer   = when (upper) {
            peer1.uppercase() -> peer2
            peer2.uppercase() -> peer1
            else              -> if (peer2.isNotEmpty()) peer1 else return null
        }
        return RoomCodeMatch(roomId = roomId, myCode = upper, peerCode = peer.uppercase())
    }
}
