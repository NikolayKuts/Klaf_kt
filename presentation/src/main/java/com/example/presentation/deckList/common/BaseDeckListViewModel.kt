package com.example.presentation.deckList.common

import androidx.lifecycle.ViewModel
import com.example.domain.common.DataSynchronizationState
import com.example.domain.entities.Deck
import com.example.presentation.common.EventMessageSource
import com.example.presentation.deckList.deckCreation.DeckCreationState
import com.example.presentation.deckList.deckRenaming.DeckRenamingState
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

abstract class BaseDeckListViewModel : ViewModel(), EventMessageSource {

    abstract val renamingState: StateFlow<DeckRenamingState>
    abstract val deckCreationState: StateFlow<DeckCreationState>
    abstract val dataSynchronizationState: StateFlow<DataSynchronizationState>
    abstract val deckSource: StateFlow<List<Deck>?>
    abstract val navigationDestination: SharedFlow<DeckListNavigationDestination>

    abstract fun resetSynchronizationState()
    abstract fun createNewDeck(deckName: String)
    abstract fun resetDeckCreationState()
    abstract fun renameDeck(deck: Deck, newName: String)
    abstract fun resetDeckRenamingState()
    abstract fun deleteDeck(deckId: Int)
    abstract fun getDeckById(deckId: Int): Deck?
    abstract fun synchronizeData()
    abstract fun navigate(event: DeckListNavigationEvent)
    abstract fun reopenApp()
}