package com.example.klaf.domain.entities

import java.util.concurrent.TimeUnit

data class Deck(
    val name: String,
    val creationDate: Long,
    val repetitionIterationDates: List<Long> = emptyList(),
    val scheduledIterationDates: List<Long> = emptyList(),
    val scheduledDateInterval: Long = 0,
    val repetitionQuantity: Int = 0,
    val cardQuantity: Int = 0,
    val lastFirstRepetitionDuration: Long = 0,
    val lastSecondRepetitionDuration: Long = 0,
    val lastRepetitionIterationDuration: Long = 0,
    val isLastIterationSucceeded: Boolean = true,
    val id: Int = 0,
) {

    val lastRepetitionIterationDate: Long? get() = repetitionIterationDates.lastOrNull()
    val scheduledDate: Long? get() = scheduledIterationDates.lastOrNull()
    val existenceDayQuantity: Long get() {
        return TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - creationDate) + 1
    }
}