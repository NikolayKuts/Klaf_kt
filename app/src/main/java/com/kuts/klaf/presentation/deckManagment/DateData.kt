package com.kuts.klaf.presentation.deckManagment

data class DateData(
    val year: DateUnit.Year = DateUnit.Year(0),
    val month: DateUnit.Month = DateUnit.Month(0),
    val week: DateUnit.Week = DateUnit.Week(0),
    val day: DateUnit.Day = DateUnit.Day(0),
    val hour: DateUnit.Hour = DateUnit.Hour(0),
    val minute: DateUnit.Minute = DateUnit.Minute(0),
) {

    fun copyByUnit(unit: DateUnit, value: Int): DateData = when (unit) {
        is DateUnit.Day -> copy(day = unit.makeCopy(newValue = value))
        is DateUnit.Hour -> copy(hour = unit.makeCopy(newValue = value))
        is DateUnit.Minute -> copy(minute = unit.makeCopy(newValue = value))
        is DateUnit.Month -> copy(month = unit.makeCopy(newValue = value))
        is DateUnit.Week -> copy(week = unit.makeCopy(newValue = value))
        is DateUnit.Year -> copy(year = unit.makeCopy(newValue = value))
    }

    fun copyWithEmptyUnit(unit: DateUnit): DateData  = when (unit) {
        is DateUnit.Day -> copy(day = unit.Empty())
        is DateUnit.Hour -> copy(hour = unit.Empty())
        is DateUnit.Minute -> copy(minute = unit.Empty())
        is DateUnit.Month -> copy(month = unit.Empty())
        is DateUnit.Week -> copy(week = unit.Empty())
        is DateUnit.Year -> copy(year = unit.Empty())
    }
}

//private inline fun <reified T : DateUnit> DateUnit.makeCopy(newValue: Int): T = when (this) {
//    is DateUnit.Year -> if (this.isEmpty()) this as T else DateUnit.Year(value = newValue) as T
//    is DateUnit.Month -> if (this.isEmpty()) this as T else DateUnit.Month(value = newValue) as T
//    is DateUnit.Week -> if (this.isEmpty()) this as T else DateUnit.Week(value = newValue) as T
//    is DateUnit.Day -> if (this.isEmpty()) this as T else DateUnit.Day(value = newValue) as T
//    is DateUnit.Hour -> if (this.isEmpty()) this as T else DateUnit.Hour(value = newValue) as T
//    is DateUnit.Minute -> if (this.isEmpty()) this as T else DateUnit.Minute(value = newValue) as T
//}

private inline fun <reified T : DateUnit> DateUnit.makeCopy(newValue: Int): T = when (this) {
    is DateUnit.Year -> DateUnit.Year(value = newValue) as T
    is DateUnit.Month -> DateUnit.Month(value = newValue) as T
    is DateUnit.Week -> DateUnit.Week(value = newValue) as T
    is DateUnit.Day -> DateUnit.Day(value = newValue) as T
    is DateUnit.Hour -> DateUnit.Hour(value = newValue) as T
    is DateUnit.Minute -> DateUnit.Minute(value = newValue) as T
}