package com.example.klaf.data.firestore.entities

data class FirestoreDeck(
    val name: String = DEFAULT_STRING_VALUE,
    val creationDate: Long = DEFAULT_LONG_VALUE,
    val repetitionIterationDates: List<Long> = emptyList(),
    val scheduledIterationDates: List<Long> = emptyList(),
    val scheduledDateInterval: Long = DEFAULT_LONG_VALUE,
    val repetitionQuantity: Int = DEFAULT_INT_VALUE,
    val cardQuantity: Int = DEFAULT_INT_VALUE,
    val lastFirstRepetitionDuration: Long = DEFAULT_LONG_VALUE,
    val lastSecondRepetitionDuration: Long = DEFAULT_LONG_VALUE,
    val lastRepetitionIterationDuration: Long = DEFAULT_LONG_VALUE,
    @field: JvmField
    val isLastIterationSucceeded: Boolean = DEFAULT_BOOLEAN_VALUE,
    val id: Int = DEFAULT_INT_VALUE,
)