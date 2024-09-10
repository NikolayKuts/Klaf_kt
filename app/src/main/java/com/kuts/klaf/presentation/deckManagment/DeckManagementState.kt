package com.kuts.klaf.presentation.deckManagment

data class DeckManagementState(
    val name: String = "",
    val creationDate: String = "",
    val repetitionIterationDates: List<String> = emptyList(),
    val scheduledIterationDates: List<String> = emptyList(),
    val scheduledDateInterval: Long = 0,
    val repetitionQuantity: Int = 0,
    val cardQuantity: Int = 0,
    val lastFirstRepetitionDuration: Long = 0,
    val lastSecondRepetitionDuration: Long = 0,
    val lastRepetitionIterationDuration: Long = 0,
    val isLastIterationSucceeded: Boolean = true,
    val id: Int = 0
)