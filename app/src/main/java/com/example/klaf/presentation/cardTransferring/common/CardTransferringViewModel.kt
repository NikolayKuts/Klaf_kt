package com.example.klaf.presentation.cardTransferring.common

import androidx.annotation.StringRes
import androidx.lifecycle.viewModelScope
import com.example.klaf.R
import com.example.klaf.domain.common.launchWithExceptionHandler
import com.example.klaf.domain.common.simplifiedItemFilterNot
import com.example.klaf.domain.entities.Deck
import com.example.klaf.domain.useCases.*
import com.example.klaf.presentation.common.EventMessage
import com.example.klaf.presentation.common.emit
import com.example.klaf.presentation.cardTransferring.cardDeleting.CardDeletingState
import com.example.klaf.presentation.cardTransferring.cardDeleting.CardDeletingState.*
import com.example.klaf.presentation.cardTransferring.common.CardTransferringNavigationDestination.*
import com.example.klaf.presentation.cardTransferring.common.CardTransferringNavigationDestination.CardTransferringFragment
import com.example.klaf.presentation.cardTransferring.common.CardTransferringNavigationEvent.*
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CardTransferringViewModel @AssistedInject constructor(
    fetchDeckById: FetchDeckByIdUseCase,
    private val fetchCards: FetchCardsUseCase,
    private val deleteCardFromDeckUseCase: DeleteCardFromDeckUseCase,
    fetchDeckSource: FetchDeckSourceUseCase,
    private val moveCardsToDeck: TransferCardsToDeckUseCase,
) : BaseCardTransferringViewModel() {

    override val eventMessage = MutableSharedFlow<EventMessage>()

    override val interimDeck = fetchDeckById(deckId = Deck.INTERIM_DECK_ID)
        .catch { emitEventMessage(messageId = R.string.problem_with_fetching_deck) }
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            replay = 1
        )

    override val cardHolders = MutableStateFlow<List<SelectableCardHolder>>(value = emptyList())

    override val navigationDestination = MutableSharedFlow<CardTransferringNavigationDestination>()

    override val cardDeletingState: MutableStateFlow<CardDeletingState> =
        MutableStateFlow(value = NON)

    override val decks: StateFlow<List<Deck>> = fetchDeckSource()
        .catch { emitEventMessage(messageId = R.string.problem_with_fetching_decks) }
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

    override fun navigate(event: CardTransferringNavigationEvent) {
        when (event) {
            ToCardAddingFragment -> navigateToCardAddingFragmentDestination()
            ToCardDeletionDialog -> navigateToCardDeletingDialogDestination()
            ToCardMovingDialog -> navigateToCardMovingDialogDestination()
        }
    }

    override fun deleteCards() {
        cardDeletingState.value = IN_PROGRESS

        selectedCards.value
            .map { card -> card.id }
            .also { cardIds ->
                viewModelScope.launchWithExceptionHandler(
                    onException = { _, _ ->
                        emitEventMessage(messageId = R.string.problem_with_removing_cards)
                    },
                    onCompletion = {
                        cardDeletingState.value = FINISHED
                        emitEventMessage(
                            messageId = R.string.message_deletion_completed_successfully
                        )
                    }
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
                emitEventMessage(messageId = R.string.problem_with_moving_cards)
            },
            onCompletion = {
                emitDestination(destination = CardTransferringFragment)
                emitEventMessage(messageId = (R.string.message_transfer_completed_successfully))
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

    private fun navigateToCardAddingFragmentDestination() {
        emitDestination(
            destination = CardAddingFragmentDestination(interimDeckId = Deck.INTERIM_DECK_ID)
        )
    }

    private fun navigateToCardDeletingDialogDestination() {
        val cardForDeleting = selectedCards.value

        if (cardForDeleting.isEmpty()) {
            emitEventMessage(messageId = R.string.message_no_cards_selected)
        } else {
            emitDestination(
                destination = CardDeletingDialogDestination(cardQuantity = cardForDeleting.size)
            )
        }
    }

    private fun navigateToCardMovingDialogDestination() {
        if (selectedCards.value.isEmpty()) {
            emitEventMessage(messageId = R.string.message_no_cards_selected)
        } else {
            emitDestination(destination = CardMovingDialogDestination)
        }
    }

    private fun emitDestination(destination: CardTransferringNavigationDestination) {
        viewModelScope.launch { navigationDestination.emit(value = destination) }
    }

    private fun emitEventMessage(@StringRes messageId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            eventMessage.emit(messageId = messageId)
        }
    }
}