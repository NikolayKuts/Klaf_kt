package com.example.klaf.presentation.deckRepetition

sealed class RepetitionScreenState {

    object StartState : RepetitionScreenState()

    object RepetitionState : RepetitionScreenState()

    class FinishState(
        val currentDuration: String,
        val lastDuration: String,
        val scheduledDate: Long,
        val previusScheduledDate: Long,
        val lastRepetitionDate: String,
        val repetitionQuantity: String,
        val lastSuccessMark: String,
    ) : RepetitionScreenState()
}
