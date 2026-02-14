package com.kuts.klaf.presentation.cardTransferring.common

import androidx.lifecycle.ViewModel
import com.kuts.domain.entities.Deck
import com.kuts.domain.managers.IAudioPlayerManager
import com.kuts.klaf.presentation.common.IEventMessageSource
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

abstract class BaseCardTransferringViewModel : ViewModel(), IEventMessageSource {

    abstract val sourceDeck: SharedFlow<Deck?>
    abstract val cardHolders: StateFlow<List<SelectableCardHolder>>
    abstract val navigationEvent: SharedFlow<ICardTransferringNavigationEvent>
    abstract val decks: StateFlow<List<Deck>>
    abstract val audioPlayer: IAudioPlayerManager
    abstract val listHeaderState: StateFlow<ListHeaderState>

    abstract fun sendAction(action: ICardTransferringAction)
}
