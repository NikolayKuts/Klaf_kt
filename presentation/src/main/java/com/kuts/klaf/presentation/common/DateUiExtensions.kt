package com.kuts.klaf.presentation.common

import android.content.Context
import androidx.annotation.StringRes
import com.kuts.domain.common.DateData
import com.kuts.domain.common.DateUnit
import com.kuts.domain.common.DeckReviewPassSuccessMark
import com.kuts.domain.common.DeckReviewPassSuccessMark.FAILURE
import com.kuts.domain.common.DeckReviewPassSuccessMark.SUCCESS
import com.kuts.domain.common.ScheduledDateState
import com.kuts.domain.common.UNASSIGNED_LONG_VALUE
import com.kuts.domain.common.UNASSIGNED_STRING_VALUE
import com.kuts.domain.entities.Deck
import com.kuts.domain.entities.DeckRepetitionInfo
import com.kuts.klaf.presentation.R
import java.util.concurrent.TimeUnit

private const val UNASSIGNED_DATE_SYMBOL = "---"
private const val MINUS_SYMBOL = "-"

private val yearInMillis = TimeUnit.DAYS.toMillis(365)
private val monthInMillis = TimeUnit.DAYS.toMillis(31)
private val weekInMillis = TimeUnit.DAYS.toMillis(7)
private val dayInMillis = TimeUnit.DAYS.toMillis(1)
private val hourImMillis = TimeUnit.HOURS.toMillis(1)
private val minuteInMillis = TimeUnit.MINUTES.toMillis(1)

@StringRes
fun DateUnit.toLabelRes(): Int = when (this) {
    is DateUnit.Year -> R.string.year
    is DateUnit.Month -> R.string.month
    is DateUnit.Week -> R.string.week
    is DateUnit.Day -> R.string.day
    is DateUnit.Hour -> R.string.hour
    is DateUnit.Minute -> R.string.minute
}

fun DateData.asString(context: Context): String {
    val years = context.getYearsOrEmpty(year.value)
    val months = context.getMonthsOrEmpty(month.value)
    val week = context.getWeeksOrEmpty(week.value)
    val days = context.getDaysOrEmpty(day.value)
    val hours = context.getHoursOrEmpty(hour.value)
    val minutes = context.getMinutesOrEmpty(minute.value)

    return buildString {
        setOf(years, months, week, days, hours, minutes).forEach {
            if (it.isNotEmpty()) append("$it ")
        }
    }
}

fun Deck.calculateDetailedScheduledRange(context: Context): String {
    return scheduledDate.calculateDetailedScheduledRange(context = context)
}

fun DeckRepetitionInfo.calculateDetailedScheduledRange(context: Context): String {
    return scheduledDate.calculateDetailedScheduledRange(context = context)
}

fun DeckRepetitionInfo.calculateDetailedPreviousScheduledRange(context: Context): String {
    return previousScheduledDate.calculateDetailedScheduledRange(context = context)
}

fun Long?.calculateDetailedScheduledRange(context: Context): String {
    if (this == null || this <= 0) return UNASSIGNED_DATE_SYMBOL
    val currentTime = System.currentTimeMillis()
    val range = this - currentTime

    val years = range.calculateYearQuantity().toInt()
    val months = range.calculateMonthQuantity().toInt()
    val weeks = range.calculateWeekQuantity().toInt()
    val days = range.calculateDayQuantity().toInt()
    val hours = range.calculateHoursQuantity().toInt()
    val minutes = range.calculateMinuteQuantity().toInt()

    val yearsMonthsWeeks =
        context.getDetailedYearsMonthsWeeks(years = years, months = months, weeks = weeks)
    val daysHoursMinutes =
        context.getDetailedDaysHoursMinutes(days = days, hours = hours, minutes = minutes)

    return yearsMonthsWeeks.ifEmpty {
        daysHoursMinutes.ifEmpty { context.getString(R.string.time_pointer_now) }
    }
}

fun Deck.getScheduledDateStateByByCalculatedRange(context: Context): ScheduledDateState {
    val range = calculateDetailedScheduledRange(context = context)

    return ScheduledDateState(
        range = range,
        isOverdue = range.firstOrNull()?.toString() == MINUS_SYMBOL
    )
}

val DeckRepetitionInfo.currentDurationAsTimeOrUnassigned: String
    get() = if (currentDuration == UNASSIGNED_LONG_VALUE) {
        UNASSIGNED_STRING_VALUE
    } else {
        currentDuration.timeAsString
    }

val DeckReviewPassSuccessMark.markResId: Int
    @StringRes get() = when (this) {
        DeckReviewPassSuccessMark.UNASSIGNED -> R.string.unassigned_string_value
        SUCCESS -> R.string.deck_repetition_mark_successful
        FAILURE -> R.string.deck_repetition_mark_failed
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
        monthInMillis % weekInMillis % dayInMillis % hourImMillis / minuteInMillis
}

private fun Context.getDetailedYearsMonthsWeeks(years: Int, months: Int, weeks: Int): String {
    val range =
        "${getYearsOrEmpty(years)} ${getMonthsOrEmpty(months)} ${getWeeksOrEmpty(weeks)}".trim()

    if (range.isEmpty()) return ""
    return range.getFormatted()
}

private fun Context.getDetailedDaysHoursMinutes(days: Int, hours: Int, minutes: Int): String {
    val range = "${getDaysOrEmpty(days)} ${getHoursOrEmpty(hours)} ${getMinutesOrEmpty(minutes)}"
        .trim()

    if (range.isEmpty()) return ""
    return range.getFormatted()
}

private fun String.getFormatted(): String {
    return this.substring(range = 0..0) + this.substring(startIndex = 1, endIndex = this.length)
        .replace(oldValue = MINUS_SYMBOL, newValue = "")
}

private fun Context.getYearsOrEmpty(years: Int): String {
    return if (years == 0) "" else getString(R.string.year_pointer, years)
}

private fun Context.getMonthsOrEmpty(months: Int): String {
    return if (months == 0) "" else getString(R.string.month_pointer, months)
}

private fun Context.getWeeksOrEmpty(weeks: Int): String {
    return if (weeks == 0) "" else getString(R.string.week_pointer, weeks)
}

private fun Context.getDaysOrEmpty(days: Int): String {
    return if (days == 0) "" else getString(R.string.day_pointer, days)
}

private fun Context.getHoursOrEmpty(hours: Int): String {
    return if (hours == 0) "" else getString(R.string.hour_pointer, hours)
}

private fun Context.getMinutesOrEmpty(minutes: Int): String {
    return if (minutes == 0) "" else getString(R.string.minute_pointer, minutes)
}
