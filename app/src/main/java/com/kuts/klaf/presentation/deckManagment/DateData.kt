package com.kuts.klaf.presentation.deckManagment

data class DateData(
    val year: DateUnit.Year = DateUnit.Year(),
    val month: DateUnit.Month = DateUnit.Month(),
    val week: DateUnit.Week = DateUnit.Week(),
    val day: DateUnit.Day = DateUnit.Day(),
    val hour: DateUnit.Hour = DateUnit.Hour(),
    val minute: DateUnit.Minute = DateUnit.Minute(),
) {

    fun toList(): List<DateUnit> = listOf(
        year,
        month,
        week,
        day,
        hour,
        minute,
    )

    fun copyByUnit(unit: DateUnit, value: Int): DateData = when (unit) {
        is DateUnit.Year -> copy(year = unit.makeCopy(newValue = value))
        is DateUnit.Month -> copy(month = unit.makeCopy(newValue = value))
        is DateUnit.Week -> copy(week = unit.makeCopy(newValue = value))
        is DateUnit.Day -> copy(day = unit.makeCopy(newValue = value))
        is DateUnit.Hour -> copy(hour = unit.makeCopy(newValue = value))
        is DateUnit.Minute -> copy(minute = unit.makeCopy(newValue = value))
    }

    private inline fun <reified T : DateUnit> DateUnit.makeCopy(newValue: Int): T = when (this) {
        is DateUnit.Year -> DateUnit.Year(value = newValue) as T
        is DateUnit.Month -> DateUnit.Month(value = newValue) as T
        is DateUnit.Week -> DateUnit.Week(value = newValue) as T
        is DateUnit.Day -> DateUnit.Day(value = newValue) as T
        is DateUnit.Hour -> DateUnit.Hour(value = newValue) as T
        is DateUnit.Minute -> DateUnit.Minute(value = newValue) as T
    }
}