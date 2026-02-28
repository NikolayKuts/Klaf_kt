package com.kuts.domain.common

sealed class DateUnit {

    abstract val value: Int

    data class Year(override val value: Int = 0) : DateUnit()
    data class Month(override val value: Int = 0) : DateUnit()
    data class Week(override val value: Int = 0) : DateUnit()
    data class Day(override val value: Int = 0) : DateUnit()
    data class Hour(override val value: Int = 0) : DateUnit()
    data class Minute(override val value: Int = 0) : DateUnit()
}
