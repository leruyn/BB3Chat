package com.bb3.bb3chat.core.disguise

enum class DisguiseType(val storageKey: String, val label: String) {
    CALCULATOR("calculator", "Máy tính"),
    WEATHER("weather", "Thời tiết"),
    MUSIC("music", "Nhạc"),
    NEWS("news", "Tin tức"),
    DICTIONARY("dictionary", "Từ điển"),
    BANKING("banking", "Ngân hàng");

    companion object {
        fun fromStorage(value: String?): DisguiseType =
            entries.firstOrNull { it.storageKey == value } ?: CALCULATOR
    }
}
