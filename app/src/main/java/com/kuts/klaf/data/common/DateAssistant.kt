package com.kuts.klaf.data.common

import android.content.Context
import com.kuts.domain.common.ScheduledDateState
import com.kuts.domain.common.getCurrentDateAsLong
import com.kuts.domain.entities.Deck
import com.kuts.domain.entities.Deck.Companion.MIN_SCHEDULED_REPETITION_INTERVAL_MINUTES
import com.kuts.domain.entities.DeckRepetitionInfo
import com.kuts.domain.enums.DayIncreaseFactor.FIRST_DAY_INCREASE_FACTOR
import com.kuts.domain.enums.DayIncreaseFactor.SECOND_DAY_INCREASE_FACTOR
import com.kuts.domain.enums.DayIncreaseFactor.THIRD_DAY_INCREASE_FACTOR
import com.kuts.domain.enums.DayIncreaseFactor.WHOLE_DAY_INCREASE_FACTOR
import com.kuts.klaf.R
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val ADDITIONAL_ALLOWABLE_DURATION_FACTOR = 0.07F
private const val DECREASE_FACTOR = 0.2F

private const val DATE_FORMAT_PATTERN = "dd.MM.yy|HH:mm"

private const val UNASSIGNED_DATE_SYMBOL = "---"
private const val MINUS_SYMBOL = "-"

private val yearInMillis = TimeUnit.DAYS.toMillis(365)
private val monthInMillis = TimeUnit.DAYS.toMillis(31)
private val weekInMillis = TimeUnit.DAYS.toMillis(7)
private val dayInMillis = TimeUnit.DAYS.toMillis(1)
private val hourImMillis = TimeUnit.HOURS.toMillis(1)
private val minuteINMillis = TimeUnit.MINUTES.toMillis(1)

fun getFormattedCurrentTime(): String {
    val date = Calendar.getInstance().time
    val dateFormat: DateFormat = SimpleDateFormat(DATE_FORMAT_PATTERN, Locale.getDefault())
    return dateFormat.format(date)
}

fun Long.asFormattedDate(): String {
    val dateFormat: DateFormat = SimpleDateFormat(DATE_FORMAT_PATTERN, Locale.getDefault())
    return dateFormat.format(this)
}

fun Deck.calculateNextScheduledRepeatDate(currentRepetitionIterationDuration: Long): Long {
    return if (repetitionQuantity >= 5) {
        getCurrentDateAsLong() + getNewInterval(currentRepetitionIterationDuration)
    } else {
        getCurrentDateAsLong()
    }
}

fun Deck.getNewInterval(currentIterationDuration: Long): Long {
    val minScheduledRepetitionInterval = MIN_SCHEDULED_REPETITION_INTERVAL_MINUTES.toMillis()

    if (repetitionQuantity < 5) return 0
    if (scheduledDateInterval <= 0L) return minScheduledRepetitionInterval

    val additionalDuration =
        (lastRepetitionIterationDuration * ADDITIONAL_ALLOWABLE_DURATION_FACTOR).toLong()
    val allowableIterationDuration = lastRepetitionIterationDuration + additionalDuration
    val shouldIntervalBeIncreased = currentIterationDuration <= allowableIterationDuration

    return if (shouldIntervalBeIncreased) {
        calculateIncreasedInterval()
    } else {
        calculateDecreasedInterval(currentIterationDuration = currentIterationDuration)
    }
}

private fun Deck.calculateIncreasedInterval(): Long {
    val dayIncreaseFactor = getDayIncreaseFactorByDayQuantity(quantity = existenceDayQuantity)
    val increaseInterval = (scheduledDateInterval * dayIncreaseFactor).toLong()
    return scheduledDateInterval + increaseInterval
}

private fun Deck.calculateDecreasedInterval(currentIterationDuration: Long): Long {
    val minScheduledRepetitionInterval = MIN_SCHEDULED_REPETITION_INTERVAL_MINUTES.toMillis()
    val multiplicationFactor =
        (currentIterationDuration.toFloat() / lastRepetitionIterationDuration)
    val actualDecreaseFactor = DECREASE_FACTOR * multiplicationFactor
    val decreaseInterval = (scheduledDateInterval * actualDecreaseFactor).toLong()
    val decreasedInterval = scheduledDateInterval - decreaseInterval

    return if (decreaseInterval >= scheduledDateInterval) {
        minScheduledRepetitionInterval
    } else {
        decreasedInterval
    }
}

private fun Long.toMillis(): Long = TimeUnit.MINUTES.toMillis(this)

fun Deck.isRepetitionSucceeded(currentRepetitionDuration: Long): Boolean {
    val lastRepetitionDuration: Long = this.lastRepetitionIterationDuration
    return currentRepetitionDuration <=
            lastRepetitionDuration + lastRepetitionDuration * ADDITIONAL_ALLOWABLE_DURATION_FACTOR
}

private fun getDayIncreaseFactorByDayQuantity(quantity: Long): Float {
    return when (quantity) {
        1L -> FIRST_DAY_INCREASE_FACTOR.value
        2L -> SECOND_DAY_INCREASE_FACTOR.value
        3L -> THIRD_DAY_INCREASE_FACTOR.value
        else -> WHOLE_DAY_INCREASE_FACTOR.value
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
    return this.scheduledDate.calculateDetailedScheduledRange(context = context)
}

fun DeckRepetitionInfo.calculateDetailedScheduledRange(context: Context): String {
    return this.scheduledDate.calculateDetailedScheduledRange(context = context)
}

fun DeckRepetitionInfo.calculateDetailedPreviousScheduledRange(context: Context): String {
    return this.previousScheduledDate.calculateDetailedScheduledRange(context = context)
}

fun DeckRepetitionInfo.calculateDetailedLastIterationRange(context: Context): String {
    return this.previousScheduledDate.calculateDetailedScheduledRange(context = context)
}

fun Long?.calculateDetailedScheduledRange(context: Context): String {
    if (this == null || this <= 0) return UNASSIGNED_DATE_SYMBOL
    val currentTime = System.currentTimeMillis()
    val range = this - currentTime

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

    return yearsMonthsWeeks.ifEmpty {
        daysHoursMinutes.ifEmpty { context.getString(R.string.time_pointer_now) }
    }
}

fun Deck.getScheduledDateStateByByCalculatedRange(context: Context): ScheduledDateState {
    val range = this.calculateDetailedScheduledRange(context = context)

    return ScheduledDateState(
        range = range,
        isOverdue = range.firstOrNull()?.toString() == MINUS_SYMBOL
    )
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