package com.example.klaf.presentation.interimDeck

import androidx.lifecycle.viewModelScope
import com.example.klaf.domain.entities.Deck
import com.example.klaf.domain.useCases.FetchCardsUseCase
import com.example.klaf.domain.useCases.FetchDeckByIdUseCase
import com.example.klaf.presentation.common.EventMessage
import com.example.klaf.presentation.interimDeck.InterimDeckNavigationDestination.*
import com.example.klaf.presentation.interimDeck.InterimDeckNavigationEvent.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InterimDeckViewModel @Inject constructor(
    fetchDeckById: FetchDeckByIdUseCase,
    private val fetchCards: FetchCardsUseCase,
) : BaseInterimDeckViewModel() {

    override val eventMessage = MutableSharedFlow<EventMessage>()

    override val interimDeck = fetchDeckById(deckId = Deck.INTERIM_DECK_ID).shareIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        replay = 0
    )

    override val cardHolders = MutableStateFlow<List<SelectableCardHolder>>(value = emptyList())

    override val navigationDestination = MutableSharedFlow<InterimDeckNavigationDestination>()

    init {
        observeCardSource()
    }

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

    override fun navigate(event: InterimDeckNavigationEvent) {
        val destination = when(event) {
            ToCardAddingFragment -> CardAddingFragmentDestination
            ToCardDeletionDialog -> CardDeletingDialogDestination
            ToCardMovingDialog -> CardMovingDialogDestination
        }

        viewModelScope.launch { navigationDestination.emit(value = destination) }
    }

    private fun observeCardSource() {
        viewModelScope.launch {
            fetchCards(deckId = Deck.INTERIM_DECK_ID).collect { cards ->
                cardHolders.value = cards.map { card -> SelectableCardHolder(card = card) }
            }
        }
    }
}