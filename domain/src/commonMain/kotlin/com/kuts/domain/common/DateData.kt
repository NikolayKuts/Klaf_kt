package com.kuts.domain.common

data class DateData(
    val year: DateUnit.Year = DateUnit.Year(),
    val month: DateUnit.Month = DateUnit.Month(),
    val week: DateUnit.Week = DateUnit.Week(),
    val day: DateUnit.Day = DateUnit.Day(),
    val hour: DateUnit.Hour = DateUnit.Hour(),
    val minute: DateUnit.Minute = DateUnit.Minute(),
) {

    fun toList(): List<DateUnit> = listOf(year, month, week, day, hour, minute)

    fun copyByUnit(unit: DateUnit, value: Int): DateData = when (unit) {
        is DateUnit.Year -> copy(year = DateUnit.Year(value = value))
        is DateUnit.Month -> copy(month = DateUnit.Month(value = value))
        is DateUnit.Week -> copy(week = DateUnit.Week(value = value))
        is DateUnit.Day -> copy(day = DateUnit.Day(value = value))
        is DateUnit.Hour -> copy(hour = DateUnit.Hour(value = value))
        is DateUnit.Minute -> copy(minute = DateUnit.Minute(value = value))
    }
}
