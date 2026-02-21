package com.kuts.klaf.common

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale

object DateFormatPattern {
    const val FULL_WITH_DIVIDER = "dd.MM.yy|HH:mm"
    const val FULL = "dd.MM.yy HH:mm"
}

fun Long.asFormattedDate(
    pattern: String = DateFormatPattern.FULL_WITH_DIVIDER,
): String {
    val dateFormat: DateFormat = SimpleDateFormat(pattern, Locale.getDefault())
    return dateFormat.format(this)
}
