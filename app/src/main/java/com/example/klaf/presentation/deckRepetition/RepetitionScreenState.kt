package com.example.klaf.presentation.deckRepetition

sealed class RepetitionScreenState {

    object StartState : RepetitionScreenState()

    object RepetitionState : RepetitionScreenState()

    object FinishState : RepetitionScreenState()
}
