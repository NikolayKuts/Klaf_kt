package com.example.klaf.presentation.deckList.common

import com.example.klaf.domain.entities.Deck

sealed class DeckListNavigationDestination {

    object DeckCreationDialogDestination : DeckListNavigationDestination()

    data class DeckRepetitionFragmentDestination(val deck: Deck) : DeckListNavigationDestination()

    data class DeckNavigationDialogDestination(val deck: Deck) : DeckListNavigationDestination()

    object DataSynchronizationDialogDestination : DeckListNavigationDestination()

    data class CardTransferringDestination(val deckId: Int) : DeckListNavigationDestination()
}
