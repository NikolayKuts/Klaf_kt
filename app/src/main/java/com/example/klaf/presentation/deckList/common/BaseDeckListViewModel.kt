package com.example.klaf.presentation.deckList.common

import androidx.lifecycle.ViewModel
import com.example.domain.entities.Deck
import com.example.klaf.data.common.DataSynchronizationState
import com.example.klaf.presentation.common.EventMessageSource
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

abstract class BaseDeckListViewModel : ViewModel(), EventMessageSource {

    abstract val dataSynchronizationState: StateFlow<DataSynchronizationState>
    abstract val deckSource: StateFlow<List<Deck>?>
    abstract val navigationDestination: StateFlow<DeckListNavigationDestination>
    abstract val navigationEvent: SharedFlow<DeckListNavigationEvent>

    abstract fun resetSynchronizationState()
    abstract fun createNewDeck(deckName: String)
    abstract fun renameDeck(deck: Deck, newName: String)
    abstract fun deleteDeck(deckId: Int)
    abstract fun getDeckById(deckId: Int): Deck?
    abstract fun synchronizeData()
    abstract fun handleNavigation(event: DeckListNavigationEvent)
    abstract fun reopenApp()
}