package com.kuts.klaf.common

import androidx.annotation.StringRes
import com.kuts.klaf.data.R

sealed class DateUnit {

    @get:StringRes
    abstract val nameRes: Int
    abstract val value: Int

    data class Year(
        override val nameRes: Int = R.string.year,
        override val value: Int = 0,
    ) : DateUnit()

    data class Month(
        override val nameRes: Int = R.string.month,
        override val value: Int = 0,
    ) : DateUnit()

    data class Week(
        override val nameRes: Int = R.string.week,
        override val value: Int = 0,
    ) : DateUnit()

    data class Day(
        override val nameRes: Int = R.string.day,
        override val value: Int = 0,
    ) : DateUnit()

    data class Hour(
        override val nameRes: Int = R.string.hour,
        override val value: Int = 0,
    ) : DateUnit()

    data class Minute(
        override val nameRes: Int = R.string.minute,
        override val value: Int = 0,
    ) : DateUnit()
}
