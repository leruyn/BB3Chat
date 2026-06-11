package com.bb3.bb3chat.core.crypto

/**
 * Deterministic room id from both peers' handshake codes (order-independent).
 */
object RoomIdDeriver {
    fun derive(codeA: String, codeB: String): String {
        val sorted = listOf(codeA.uppercase(), codeB.uppercase()).sorted().joinToString("|")
        return sorted.map { it.code.toString(16) }.joinToString("").take(28)
    }
}
