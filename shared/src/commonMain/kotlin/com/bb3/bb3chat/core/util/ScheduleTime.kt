package com.bb3.bb3chat.core.util

import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

fun fireAtMsForToday(hour: Int, minute: Int): Long {
    val zone = TimeZone.currentSystemDefault()
    val now  = Clock.System.now().toLocalDateTime(zone)
    val time = LocalTime(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
    var date = now.date
    var target = LocalDateTime(date, time)
    if (target <= now) {
        date = date.plus(1, DateTimeUnit.DAY)
        target = LocalDateTime(date, time)
    }
    return target.toInstant(zone).toEpochMilliseconds()
}
