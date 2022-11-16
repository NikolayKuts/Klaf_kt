package com.example.klaf.presentation.deckRepetition

sealed class RepetitionScreenState {

    object StartState : RepetitionScreenState()

    object RepetitionState : RepetitionScreenState()

    class FinishState(
        val currentDuration: String,
        val previousDuration: String,
        val scheduledDate: Long,
        val previousScheduledDate: Long,
        val lastRepetitionIterationDate: String?,
        val repetitionQuantity: String,
        val lastSuccessMark: String,
    ) : RepetitionScreenState()
}
