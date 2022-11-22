package com.example.klaf.presentation.interimDeck

import androidx.lifecycle.viewModelScope
import com.example.klaf.domain.entities.Deck
import com.example.klaf.domain.useCases.FetchCardsUseCase
import com.example.klaf.domain.useCases.FetchDeckByIdUseCase
import com.example.klaf.presentation.common.EventMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class InterimDeckViewModel @Inject constructor(
    fetchDeckById: FetchDeckByIdUseCase,
    fetchCards: FetchCardsUseCase,
) : BaseInterimDeckViewModel() {

    override val eventMessage = MutableSharedFlow<EventMessage>()

    override val interimDeck = fetchDeckById(deckId = Deck.INTERIM_DECK_ID).shareIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        replay = 0
    )

    override val cardHolders = MutableStateFlow<List<SelectableCardHolder>>(value = emptyList())

    override val cards = fetchCards(deckId = Deck.INTERIM_DECK_ID)
        .onEach { cardList ->
            cardHolders.value = cardList.map { card -> SelectableCardHolder(card = card) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    override fun changeSelectionState(position: Int) {
        cardHolders.update { holders ->
            val holder = holders[position]
            holders.toMutableList()
                .apply { this[position] = holder.copy(isSelected = !holder.isSelected) }
        }
    }

    override fun selectAllCards() {
        cardHolders.update { holders ->
            holders.map { holder -> holder.copy(isSelected = true) }
        }
    }
}