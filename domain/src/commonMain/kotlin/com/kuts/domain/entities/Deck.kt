package com.kuts.domain.entities

import com.kuts.domain.common.UNASSIGNED_LONG_VALUE
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

data class Deck(
    val name: String,
    val creationDate: Long,
    val reviewPassDates: List<Long> = emptyList(),
    val scheduledReviewDates: List<Long> = emptyList(),
    val scheduledDateInterval: Long = 0,
    val reviewCount: Int = 0,
    val cardQuantity: Int = 0,
    val lastFirstReviewDuration: Long = 0,
    val lastSecondReviewDuration: Long = 0,
    val lastReviewPassDuration: Long = 0,
    val isLastPassSucceeded: Boolean = true,
    val id: Int = 0,
) {

    companion object {

        const val INTERIM_DECK_NAME = "interim deck"
        const val INTERIM_DECK_ID = -1
        const val MAX_NAME_LENGTH = 30
        const val MIN_SCHEDULED_REPETITION_INTERVAL_MINUTES = 15L
    }

    val lastRepetitionIterationDate: Long? get() = reviewPassDates.lastOrNull()
    val scheduledDate: Long? get() = scheduledReviewDates.lastOrNull()
    val scheduledDateOrUnassignedValue: Long get() = scheduledDate ?: UNASSIGNED_LONG_VALUE
    @OptIn(ExperimentalTime::class)
    val existenceDayQuantity: Long get() {
        val dayInMillis = 24L * 60L * 60L * 1000L
        val currentTime = Clock.System.now().toEpochMilliseconds()
        return ((currentTime - creationDate) / dayInMillis) + 1
    }
}
