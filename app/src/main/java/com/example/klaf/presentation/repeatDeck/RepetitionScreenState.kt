package com.example.klaf.presentation.repeatDeck

sealed class RepetitionScreenState {

    object StartState : RepetitionScreenState()

    object RepetitionState : RepetitionScreenState()

    object FinishState : RepetitionScreenState()
}
