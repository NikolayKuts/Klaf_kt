package com.example.klaf.presentation.interimDeck.common

import androidx.lifecycle.viewModelScope
import com.example.klaf.R
import com.example.klaf.domain.common.launchWithExceptionHandler
import com.example.klaf.domain.common.simplifiedItemFilterNot
import com.example.klaf.domain.entities.Deck
import com.example.klaf.domain.useCases.*
import com.example.klaf.presentation.common.EventMessage
import com.example.klaf.presentation.common.tryEmit
import com.example.klaf.presentation.interimDeck.cardDeleting.CardDeletingState
import com.example.klaf.presentation.interimDeck.cardDeleting.CardDeletingState.*
import com.example.klaf.presentation.interimDeck.common.InterimDeckNavigationDestination.*
import com.example.klaf.presentation.interimDeck.common.InterimDeckNavigationDestination.InterimDeckFragment
import com.example.klaf.presentation.interimDeck.common.InterimDeckNavigationEvent.*
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class InterimDeckViewModel @AssistedInject constructor(
    fetchDeckById: FetchDeckByIdUseCase,
    private val fetchCards: FetchCardsUseCase,
    private val deleteCardFromDeckUseCase: DeleteCardFromDeckUseCase,
    fetchDeckSource: FetchDeckSourceUseCase,
    private val moveCardsToDeck: MoveCarsToDeckUseCase,
) : BaseInterimDeckViewModel() {

    override val eventMessage = MutableSharedFlow<EventMessage>()

    override val interimDeck = fetchDeckById(deckId = Deck.INTERIM_DECK_ID)
        .catch { eventMessage.tryEmit(messageId = R.string.problem_with_fetching_deck) }
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            replay = 1
        )

    override val cardHolders = MutableStateFlow<List<SelectableCardHolder>>(value = emptyList())

    override val navigationDestination = MutableSharedFlow<InterimDeckNavigationDestination>()

    override val cardDeletingState: MutableStateFlow<CardDeletingState> =
        MutableStateFlow(value = NON)

    override val decks: StateFlow<List<Deck>> = fetchDeckSource()
        .catch { eventMessage.tryEmit(messageId = R.string.problem_with_fetching_decks) }
        .simplifiedItemFilterNot { deck -> deck.id == Deck.INTERIM_DECK_ID }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    private val selectedCards = cardHolders.map { holders ->
        holders.filter { it.isSelected }
            .map { holder -> holder.card }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

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
        val destination = when (event) {
            ToCardAddingFragment -> {
                CardAddingFragmentDestination(interimDeckId = Deck.INTERIM_DECK_ID)
            }
            ToCardDeletionDialog -> {
                CardDeletingDialogDestination(cardQuantity = selectedCards.value.size)
            }
            ToCardMovingDialog -> CardMovingDialogDestination
        }

        viewModelScope.launch { navigationDestination.emit(value = destination) }
    }

    override fun deleteCards() {
        cardDeletingState.value = IN_PROGRESS

        selectedCards.value
            .map { card -> card.id }
            .also { cardIds ->
                viewModelScope.launchWithExceptionHandler(
                    onException = { _, _ ->
                        eventMessage.tryEmit(messageId = R.string.problem_with_removing_cards)
                    },
                    onCompletion = { cardDeletingState.value = FINISHED }
                ) {
                    deleteCardFromDeckUseCase(
                        cardIds = cardIds.toIntArray(),
                        deckId = Deck.INTERIM_DECK_ID
                    )
                }
            }
    }

    override fun moveCards(targetDeck: Deck) {
        viewModelScope.launchWithExceptionHandler(
            onException = { _, _ ->
                eventMessage.tryEmit(messageId = R.string.problem_with_moving_cards)
            },
            onCompletion = {
                viewModelScope.launch { navigationDestination.emit(value = InterimDeckFragment) }
            }
        ) {
            interimDeck.replayCache.first()?.let { sourceDeck ->
                moveCardsToDeck(
                    sourceDeck = sourceDeck,
                    targetDeck = targetDeck,
                    cards = selectedCards.value.toTypedArray())
            }
        }
    }

    override fun resetCardDeletingState() {
        cardDeletingState.value = NON
    }

    private fun observeCardSource() {
        viewModelScope.launch {
            fetchCards(deckId = Deck.INTERIM_DECK_ID).collect { cards ->
                cardHolders.value = cards.map { card -> SelectableCardHolder(card = card) }
            }
        }
    }
}