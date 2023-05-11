package com.example.klaf.presentation.cardTransferring.common

import androidx.lifecycle.viewModelScope
import com.example.domain.common.CoroutineStateHolder.Companion.launchWithState
import com.example.domain.common.CoroutineStateHolder.Companion.onExceptionWithCrashlyticsReport
import com.example.domain.common.catchWithCrashlyticsReport
import com.example.domain.entities.Deck
import com.example.domain.repositories.CrashlyticsRepository
import com.example.domain.useCases.*
import com.example.klaf.R
import com.example.klaf.presentation.cardTransferring.common.CardTransferringNavigationDestination.*
import com.example.klaf.presentation.cardTransferring.common.CardTransferringNavigationEvent.*
import com.example.klaf.presentation.common.EventMessage
import com.example.klaf.presentation.common.tryEmit
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CardTransferringViewModel @AssistedInject constructor(
    @Assisted private val sourceDeckId: Int,
    fetchDeckById: FetchDeckByIdUseCase,
    private val fetchCards: FetchCardsUseCase,
    private val deleteCardsFromDeckUseCase: DeleteCardsFromDeckUseCase,
    fetchDeckSource: FetchDeckSourceUseCase,
    private val moveCardsToDeck: TransferCardsToDeckUseCase,
    private val crashlytics: CrashlyticsRepository,
) : BaseCardTransferringViewModel() {

    override val eventMessage = MutableSharedFlow<EventMessage>(extraBufferCapacity = 1)

    override val sourceDeck = fetchDeckById(deckId = sourceDeckId)
        .catchWithCrashlyticsReport(crashlytics = crashlytics) {
            eventMessage.tryEmit(messageId = R.string.problem_with_fetching_deck)
        }.shareIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            replay = 1
        )

    override val cardHolders = MutableStateFlow<List<SelectableCardHolder>>(value = emptyList())

    override val navigationEvent = MutableSharedFlow<CardTransferringNavigationEvent>()

    override val decks: StateFlow<List<Deck>> = fetchDeckSource()
        .catchWithCrashlyticsReport(crashlytics = crashlytics) {
            eventMessage.tryEmit(messageId = R.string.problem_fetching_decks)
        }.filterNotCurrentAndInterimDecks()
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

    override fun navigateTo(destination: CardTransferringNavigationDestination) {
        when (destination) {
            CardAddingFragment -> sendCardAddingScreenEvent()
            CardDeletionDialog -> sendCardDeletingDialogEvent()
            CardMovingDialog -> sendCardMovingDialogEvent()
            is CardEditingFragment -> {
                sendCardEditingScreenEvent(selectedCardIndex = destination.selectedCardIndexIndex)
            }
        }
    }

    override fun deleteCards() {
        selectedCards.value
            .map { card -> card.id }
            .also { cardIds ->
                viewModelScope.launchWithState {
                    deleteCardsFromDeckUseCase(
                        cardIds = cardIds.toIntArray(),
                        deckId = sourceDeckId
                    )
                    eventMessage.tryEmit(messageId = R.string.message_deletion_completed_successfully)
                    navigationEvent.emit(value = ToPrevious)
                }.onExceptionWithCrashlyticsReport(crashlytics = crashlytics) { _, _ ->
                    eventMessage.tryEmit(messageId = R.string.problem_with_removing_cards)
                }
            }
    }

    override fun moveCards(targetDeck: Deck) {
        viewModelScope.launchWithState {
            sourceDeck.replayCache.first()?.let { sourceDeck ->
                moveCardsToDeck(
                    sourceDeck = sourceDeck,
                    targetDeck = targetDeck,
                    cards = selectedCards.value.toTypedArray()
                )

                navigationEvent.emit(value = ToPrevious)
                eventMessage.tryEmit(messageId = (R.string.message_transfer_completed_successfully))
            }
        }.onExceptionWithCrashlyticsReport(crashlytics = crashlytics) { _, _ ->
            eventMessage.tryEmit(messageId = R.string.problem_with_moving_cards)
        }
    }

    private fun observeCardSource() {
        fetchCards(deckId = sourceDeckId)
            .catchWithCrashlyticsReport(crashlytics = crashlytics) {
                eventMessage.tryEmit(messageId = R.string.problem_with_fetching_cards)
            }.onEach { cards ->
                cardHolders.value = cards.map { card -> SelectableCardHolder(card = card) }
            }.launchIn(viewModelScope)
    }

    private fun Flow<List<Deck>>.filterNotCurrentAndInterimDecks(): Flow<List<Deck>> {
        return map { fetchedDecks ->
            fetchedDecks.filterNot { deck ->
                deck.id == sourceDeckId || deck.id == Deck.INTERIM_DECK_ID
            }
        }
    }

    private fun sendCardAddingScreenEvent() {
        emitEvent(event = ToCardAddingScreen(sourceDeckId = sourceDeckId))
    }

    private fun sendCardDeletingDialogEvent() {
        val cardForDeleting = selectedCards.value

        if (cardForDeleting.isEmpty()) {
            eventMessage.tryEmit(messageId = R.string.message_no_cards_selected)
        } else {
            emitEvent(event = ToCardDeletingDialog(cardQuantity = cardForDeleting.size))
        }
    }

    private fun sendCardMovingDialogEvent() {
        if (selectedCards.value.isEmpty()) {
            eventMessage.tryEmit(messageId = R.string.message_no_cards_selected)
        } else {
            emitEvent(event = ToCardMovingDialog)
        }
    }

    private fun sendCardEditingScreenEvent(selectedCardIndex: Int) {
        val selectedCard = cardHolders.value[selectedCardIndex].card

        emitEvent(
            event = ToCardEditingScreen(cardId = selectedCard.id, deckId = selectedCard.deckId)
        )
    }

    private fun emitEvent(event: CardTransferringNavigationEvent) {
        viewModelScope.launch { navigationEvent.emit(value = event) }
    }
}