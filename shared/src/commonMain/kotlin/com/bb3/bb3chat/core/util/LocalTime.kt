package com.bb3.bb3chat.core.util

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class LocalTimeOfDay(val hour: Int, val minute: Int)

fun currentLocalTime(): LocalTimeOfDay {
    val dt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    return LocalTimeOfDay(dt.hour, dt.minute)
}

fun isWithinSafeHours(hour: Int, startHour: Int, endHour: Int): Boolean =
    hour in startHour until endHour
