package com.kuts.klaf.presentation.cardTransferring.common

import androidx.lifecycle.ViewModel
import com.kuts.domain.entities.Deck
import com.kuts.klaf.data.networking.CardAudioPlayer
import com.kuts.klaf.presentation.common.EventMessageSource
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

abstract class BaseCardTransferringViewModel : ViewModel(), EventMessageSource {

    abstract val sourceDeck: SharedFlow<Deck?>
    abstract val cardHolders: StateFlow<List<SelectableCardHolder>>
    abstract val navigationEvent: SharedFlow<CardTransferringNavigationEvent>
    abstract val decks: StateFlow<List<Deck>>
    abstract val audioPlayer: CardAudioPlayer
    abstract val listHeaderState: StateFlow<ListHeaderState>

    abstract fun sendAction(action: CardTransferringAction)
}