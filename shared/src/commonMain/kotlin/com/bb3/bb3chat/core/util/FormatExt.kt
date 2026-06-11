package com.bb3.bb3chat.core.util

fun Byte.toHexLower(): String = (toInt() and 0xFF).toString(16).padStart(2, '0')

fun Byte.toHexUpper(): String = toHexLower().uppercase()

fun ByteArray.toHexLower(): String = joinToString("") { it.toHexLower() }

fun ByteArray.toHexUpper(): String = joinToString("") { it.toHexUpper() }

fun formatTwoDigits(value: Int): String = value.toString().padStart(2, '0')

fun formatHourMinute(hour: Int, minute: Int): String =
    "${formatTwoDigits(hour)}:${formatTwoDigits(minute)}"

/** Calculator display — multiplatform replacement for JVM `%.6g`. */
fun formatCalculatorValue(v: Double): String {
    if (v.isNaN() || v.isInfinite()) return v.toString()
    val text = if (kotlin.math.abs(v) >= 1e-4 && kotlin.math.abs(v) < 1e6) {
        val scaled = (v * 1_000_000.0).toLong() / 1_000_000.0
        scaled.toString()
    } else {
        v.toString()
    }
    return text.trimEnd('0').trimEnd('.')
}
