package com.example.klaf.domain.common

import android.content.Context
import com.example.klaf.R
import com.example.klaf.domain.entities.Deck
import com.example.klaf.domain.enums.DayFactor.*
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

private const val DERATION_FACTOR = 0.07
private const val DECREASE_FACTOR = 0.1

private const val DATE_FORMAT_PATTERN = "dd-MM-yy|HH:mm"

private const val UNASSIGNED_DATE_SYMBOL = "-- // --"
private const val MINUS_SYMBOL = "-"

private val yearInMillis = TimeUnit.DAYS.toMillis(365)
private val monthInMillis = TimeUnit.DAYS.toMillis(31)
private val weekInMillis = TimeUnit.DAYS.toMillis(7)
private val dayInMillis = TimeUnit.DAYS.toMillis(1)
private val hourImMillis = TimeUnit.HOURS.toMillis(1)
private val minuteINMillis = TimeUnit.MINUTES.toMillis(1)

//object DateAssistant {

fun getFormattedCurrentTime(): String {
    val date = Calendar.getInstance().time
    val dateFormat: DateFormat = SimpleDateFormat(DATE_FORMAT_PATTERN, Locale.getDefault())
    return dateFormat.format(date)
}

//fun getFormattedDate(date: Long): String {
//    val dateFormat: DateFormat = SimpleDateFormat(DATE_FORMAT_PATTERN, Locale.getDefault())
//    return dateFormat.format(date)
//}


fun Long.asFormattedDate(): String {
    val dateFormat: DateFormat = SimpleDateFormat(DATE_FORMAT_PATTERN, Locale.getDefault())
    return dateFormat.format(this)
}


fun getCurrentDateAsLong(): Long {
    return Calendar.getInstance().time.time
}

fun Deck.calculateNextScheduledRepeatDate(currentRepeatDuration: Long): Long {
    val lastRepeatInterval = getLastRepeatInterval(this)

    return if (this.repeatQuantity >= 5) {

        if (this.repeatQuantity % 2 != 0) {
            val newInterval = getNewInterval(this, currentRepeatDuration, lastRepeatInterval)
            getCurrentDateAsLong() + newInterval
        } else {
            this.scheduledDate
        }
    } else {
        getCurrentDateAsLong()
    }
}

private fun getLastRepeatInterval(deck: Deck): Long {
    return if (deck.repeatQuantity == 5) {
        TimeUnit.MINUTES.toMillis(15)
    } else {
        deck.scheduledDate - deck.lastRepeatDate
    }
}

private fun getNewInterval(
    deck: Deck,
    currentRepeatDuration: Long,
    lastRepeatInterval: Long,
): Long {
    val lastRepeatDuration = deck.lastRepeatDuration

    return if (
        currentRepeatDuration <= lastRepeatDuration + lastRepeatDuration * DERATION_FACTOR
    ) {
        lastRepeatInterval + (lastRepeatInterval * getDayFactorByNumberDay(deck.repeatDay)).toLong()
    } else {

        if (deck.isLastRepetitionSucceeded) {
            lastRepeatInterval
        } else {
            lastRepeatInterval - lastRepeatInterval * DECREASE_FACTOR.toLong()
        }
    }
}

fun Deck.isRepetitionSucceeded(currentRepetitionDuration: Long): Boolean {
    val lastRepetitionDuration: Long = this.lastRepeatDuration
    return currentRepetitionDuration <= lastRepetitionDuration + lastRepetitionDuration * DERATION_FACTOR
}

fun Deck.getUpdatedRepeatDay(): Int {
    var date: Int = this.repeatDay
    val difference: Long = getCurrentDateAsLong() - this.creationDate
    val differenceInDays = difference / TimeUnit.DAYS.toMillis(1)
//        val differenceInDays = difference / 86400000
    return if (date < differenceInDays) ++date else date
}

private fun getDayFactorByNumberDay(numberDay: Int): Double {
    return when (numberDay) {
        1 -> FIRST_DAY_FACTOR.factor
        2 -> SECOND_DAY_FACTOR.factor
        3 -> THIRD_DAY_FACTOR.factor
        else -> WHOLE_DAY_FACTOR.factor
    }
}

fun Long.calculateScheduledRange(context: Context): String {
    val currentTime = System.currentTimeMillis()

    if (this <= 0L) return UNASSIGNED_DATE_SYMBOL

    val range = this - currentTime

    val years = range.calculateYearQuantity()
    val months = range.calculateMonthQuantity()
    val weeks = range.calculateWeekQuantity()
    val days = range.calculateDayQuantity()
    val hours = range.calculateHoursQuantity()
    val minutes = range.calculateMinuteQuantity()

    val yearsMonthsWeeks =
        context.getYearsMonthsWeeks(years = years, months = months, weeks = weeks)
    val daysHoursMinutes =
        context.getDaysHoursMinutes(days = days, hours = hours, minutes = minutes)

    return yearsMonthsWeeks.ifEmpty { daysHoursMinutes }
}

fun Deck.calculateDetailedScheduledRange(context: Context): String {
    val currentTime = System.currentTimeMillis()

    if (this.scheduledDate <= 0L) return UNASSIGNED_DATE_SYMBOL

    val range = this.scheduledDate - currentTime

    val years = range.calculateYearQuantity()
    val months = range.calculateMonthQuantity()
    val weeks = range.calculateWeekQuantity()
    val days = range.calculateDayQuantity()
    val hours = range.calculateHoursQuantity()
    val minutes = range.calculateMinuteQuantity()

    val yearsMonthsWeeks =
        context.getDetailedYearsMonthsWeeks(years = years, months = months, weeks = weeks)
    val daysHoursMinutes =
        context.getDetailedDaysHoursMinutes(days = days, hours = hours, minutes = minutes)

    return yearsMonthsWeeks.ifEmpty { daysHoursMinutes }
}

private fun Long.calculateYearQuantity(): Long = this / yearInMillis

private fun Long.calculateMonthQuantity(): Long = this % yearInMillis / monthInMillis

private fun Long.calculateWeekQuantity(): Long = this % yearInMillis % monthInMillis / weekInMillis

private fun Long.calculateDayQuantity(): Long {
    return this % yearInMillis % monthInMillis % weekInMillis / dayInMillis
}

private fun Long.calculateHoursQuantity(): Long {
    return this % yearInMillis % monthInMillis % weekInMillis % dayInMillis / hourImMillis
}

private fun Long.calculateMinuteQuantity(): Long {
    return this % yearInMillis %
            monthInMillis % weekInMillis % dayInMillis % hourImMillis / minuteINMillis
}

private fun Context.getDetailedYearsMonthsWeeks(years: Long, months: Long, weeks: Long): String {
    val range =
        "${getYearsOrEmpty(years)} ${getMonthsOrEmpty(months)} ${getWeeksOrEmpty(weeks)}".trim()

    if (range.isEmpty()) return ""
    return range.getFormatted()
}

private fun Context.getYearsMonthsWeeks(years: Long, months: Long, weeks: Long): String {
    val yearsAsString = getYearsOrEmpty(years)
    val monthsAsString = getMonthsOrEmpty(months)
    val weeksAsString = getWeeksOrEmpty(weeks)

    return when {
        yearsAsString.isNotEmpty() -> yearsAsString
        yearsAsString.isEmpty() && monthsAsString.isNotEmpty() -> monthsAsString
        yearsAsString.isEmpty() && monthsAsString.isEmpty() && weeksAsString.isNotEmpty() -> {
            weeksAsString
        }
        else -> ""
    }
}

private fun Context.getDetailedDaysHoursMinutes(days: Long, hours: Long, minutes: Long): String {
    val range = "${getDaysOrEmpty(days)} ${getHoursOrEmpty(hours)} ${getMinutesOrEmpty(minutes)}"
        .trim()

    if (range.isEmpty()) return ""
    return range.getFormatted()
}

private fun Context.getDaysHoursMinutes(days: Long, hours: Long, minutes: Long): String {
    val daysAsString = getDaysOrEmpty(days)
    val hoursAsString = getHoursOrEmpty(hours)
    val minutesAsString = getMinutesOrEmpty(minutes)

    return when {
        daysAsString.isNotEmpty() -> daysAsString
        daysAsString.isEmpty() && hoursAsString.isNotEmpty() -> hoursAsString
        daysAsString.isEmpty() && hoursAsString.isEmpty() && minutesAsString.isNotEmpty() -> {
            minutesAsString
        }
        else -> getString(R.string.time_pointer_now)
    }
}

private fun String.getFormatted(): String {
    return this.substring(range = 0..0) + this.substring(startIndex = 1, endIndex = this.length)
        .replace(oldValue = MINUS_SYMBOL, newValue = "")
}

private fun Context.getYearsOrEmpty(years: Long): String {
    return if (years == 0L) "" else getString(R.string.year_pointer, years)
}

private fun Context.getMonthsOrEmpty(months: Long): String {
    return if (months == 0L) "" else getString(R.string.month_pointer, months)
}

private fun Context.getWeeksOrEmpty(weeks: Long): String {
    return if (weeks == 0L) "" else getString(R.string.week_pointer, weeks)
}

private fun Context.getDaysOrEmpty(days: Long): String {
    return if (days == 0L) "" else getString(R.string.day_pointer, days)
}

private fun Context.getHoursOrEmpty(hours: Long): String {
    return if (hours == 0L) "" else getString(R.string.hour_pointer, hours)
}

private fun Context.getMinutesOrEmpty(minutes: Long): String {
    return if (minutes == 0L) "" else getString(R.string.minute_pointer, minutes)
}