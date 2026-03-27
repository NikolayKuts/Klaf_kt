package com.kuts.klaf.deckList.common

import androidx.lifecycle.ViewModel
import com.kuts.domain.common.IDataSynchronizationState
import com.kuts.domain.entities.Deck
import com.kuts.domain.entities.WordInsightsProvider
import com.kuts.klaf.common.IEventMessageSource
import com.kuts.klaf.deckList.drawer.DrawerViewState
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

abstract class BaseDeckListViewModel : ViewModel(), IEventMessageSource {

    abstract val dataSynchronizationState: StateFlow<IDataSynchronizationState>
    abstract val deckSource: StateFlow<List<Deck>?>
    abstract val navigationDestination: StateFlow<IDeckListNavigationDestination>
    abstract val navigationEvent: SharedFlow<IDeckListNavigationEvent?>
    abstract val shouldSynchronizationIndicatorBeShown: StateFlow<Boolean>
    abstract val drawerState: SharedFlow<DrawerViewState>
    abstract val drawerActionLoadingState: StateFlow<Boolean>

    abstract fun resetSynchronizationState()
    abstract fun createNewDeck(deckName: String)
    abstract fun renameDeck(deck: Deck, newName: String)
    abstract fun deleteDeck(deckId: Int)
    abstract fun getDeckById(deckId: Int): Deck?
    abstract fun synchronizeData()
    abstract fun handleNavigation(event: IDeckListNavigationEvent)
    abstract fun reopenApp()
    abstract fun logOut()
    abstract fun deleteAccount()
    abstract fun generateGptPromptWithDeckContent(deckId: Int)
    abstract fun setWordInsightsProvider(provider: WordInsightsProvider)
}
