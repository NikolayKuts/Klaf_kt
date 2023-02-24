package com.example.presentation.cardTransferring.common

sealed class CardTransferringNavigationEvent {

    object ToCardMovingDialog : CardTransferringNavigationEvent()

    object ToCardAddingFragment : CardTransferringNavigationEvent()

    object ToCardDeletionDialog: CardTransferringNavigationEvent()

    data class  ToCardEditingFragment(val cardSelectionIndex: Int): CardTransferringNavigationEvent()
}