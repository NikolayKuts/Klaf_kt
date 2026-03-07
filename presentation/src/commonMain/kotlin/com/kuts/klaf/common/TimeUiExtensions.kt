package com.kuts.klaf.common

private const val SECOND_QUANTITY_IN_MINUTE = 60
private const val MINIMUM_TWO_DIGITS = 2

val Long.timeAsString: String
    get() {
        val seconds = this % SECOND_QUANTITY_IN_MINUTE
        val minutes = this / SECOND_QUANTITY_IN_MINUTE

        return "${minutes.twoDigits()}:${seconds.twoDigits()}"
    }

private fun Long.twoDigits(): String = toString().padStart(MINIMUM_TWO_DIGITS, '0')
