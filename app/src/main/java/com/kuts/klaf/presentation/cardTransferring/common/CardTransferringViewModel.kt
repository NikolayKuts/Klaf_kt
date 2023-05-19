package com.kuts.klaf.presentation.cardTransferring.common

import androidx.lifecycle.viewModelScope
import com.kuts.domain.common.CoroutineStateHolder.Companion.launchWithState
import com.kuts.domain.common.CoroutineStateHolder.Companion.onExceptionWithCrashlyticsReport
import com.kuts.domain.common.catchWithCrashlyticsReport
import com.kuts.domain.entities.Deck
import com.kuts.domain.repositories.CrashlyticsRepository
import com.kuts.domain.useCases.*
import com.kuts.klaf.R
import com.kuts.klaf.presentation.cardTransferring.common.CardTransferringNavigationDestination.*
import com.kuts.klaf.presentation.cardTransferring.common.CardTransferringNavigationEvent.*
import com.kuts.klaf.presentation.common.EventMessage
import com.kuts.klaf.presentation.common.tryEmitAsNegative
import com.kuts.klaf.presentation.common.tryEmitAsPositive
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
            eventMessage.tryEmitAsNegative(resId = R.string.problem_with_fetching_deck)
        }.shareIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            replay = 1
        )

    override val cardHolders = MutableStateFlow<List<SelectableCardHolder>>(value = emptyList())

    override val navigationEvent = MutableSharedFlow<CardTransferringNavigationEvent>()

    override val decks: StateFlow<List<Deck>> = fetchDeckSource()
        .catchWithCrashlyticsReport(crashlytics = crashlytics) {
            eventMessage.tryEmitAsNegative(resId = R.string.problem_fetching_decks)
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
            CardTransferringScreen -> emitEvent(event = ToPrevious)
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
                    eventMessage.tryEmitAsPositive(resId = R.string.message_deletion_completed_successfully)
                    navigationEvent.emit(value = ToPrevious)
                }.onExceptionWithCrashlyticsReport(crashlytics = crashlytics) { _, _ ->
                    eventMessage.tryEmitAsNegative(resId = R.string.problem_with_removing_cards)
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
                eventMessage.tryEmitAsPositive(resId = (R.string.message_transfer_completed_successfully))
            }
        }.onExceptionWithCrashlyticsReport(crashlytics = crashlytics) { _, _ ->
            eventMessage.tryEmitAsNegative(resId = R.string.problem_with_moving_cards)
        }
    }

    private fun observeCardSource() {
        fetchCards(deckId = sourceDeckId)
            .catchWithCrashlyticsReport(crashlytics = crashlytics) {
                eventMessage.tryEmitAsNegative(resId = R.string.problem_with_fetching_cards)
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
            eventMessage.tryEmitAsNegative(resId = R.string.message_no_cards_selected)
        } else {
            emitEvent(event = ToCardDeletingDialog(cardQuantity = cardForDeleting.size))
        }
    }

    private fun sendCardMovingDialogEvent() {
        if (selectedCards.value.isEmpty()) {
            eventMessage.tryEmitAsNegative(resId = R.string.message_no_cards_selected)
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