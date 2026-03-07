package com.kuts.klaf.common

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

object DateFormatPattern {
    const val FULL_WITH_DIVIDER = "dd.MM.yy|HH:mm"
    const val FULL = "dd.MM.yy HH:mm"
}

fun Long.asFormattedDate(
    pattern: String = DateFormatPattern.FULL_WITH_DIVIDER,
): String {
    val date = Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.currentSystemDefault())
    val formattedDate = "${date.dayOfMonth.twoDigits()}.${date.monthNumber.twoDigits()}.${(date.year % 100).twoDigits()}"
    val formattedTime = "${date.hour.twoDigits()}:${date.minute.twoDigits()}"

    return when (pattern) {
        DateFormatPattern.FULL_WITH_DIVIDER -> "$formattedDate|$formattedTime"
        DateFormatPattern.FULL -> "$formattedDate $formattedTime"
        else -> "$formattedDate $formattedTime"
    }
}

private fun Int.twoDigits(): String = toString().padStart(length = 2, padChar = '0')
