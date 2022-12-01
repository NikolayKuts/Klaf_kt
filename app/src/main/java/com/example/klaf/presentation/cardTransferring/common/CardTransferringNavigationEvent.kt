package com.example.klaf.presentation.cardTransferring.common

sealed class CardTransferringNavigationEvent {

    object ToCardMovingDialog : CardTransferringNavigationEvent()

    object ToCardAddingFragment : CardTransferringNavigationEvent()

    object ToCardDeletionDialog: CardTransferringNavigationEvent()
}