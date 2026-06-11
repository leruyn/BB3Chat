package com.bb3.bb3chat.feature.pairing.domain

object RoomCodePhraseNormalizer {
    fun normalize(phrase: String): String =
        phrase.uppercase().replace(Regex("[^A-Z0-9]"), "").take(32)
            .ifEmpty { throw IllegalArgumentException("Mã phòng không hợp lệ") }
}
