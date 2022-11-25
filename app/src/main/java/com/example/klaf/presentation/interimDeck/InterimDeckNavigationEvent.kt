package com.example.klaf.presentation.interimDeck

sealed class InterimDeckNavigationEvent {

    object ToCardMovingDialog : InterimDeckNavigationEvent()

    object ToCardAddingFragment : InterimDeckNavigationEvent()

    object ToCardDeletionDialog: InterimDeckNavigationEvent()
}
