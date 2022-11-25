package com.example.klaf.presentation.interimDeck.common

sealed class InterimDeckNavigationEvent {

    object ToCardMovingDialog : InterimDeckNavigationEvent()

    object ToCardAddingFragment : InterimDeckNavigationEvent()

    object ToCardDeletionDialog: InterimDeckNavigationEvent()
}
