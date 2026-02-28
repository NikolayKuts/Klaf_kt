package com.kuts.klaf.cardTransferring.common

import androidx.lifecycle.viewModelScope
import com.kuts.domain.common.CoroutineStateHolder.Companion.launchWithState
import com.kuts.domain.common.CoroutineStateHolder.Companion.onExceptionWithCrashlyticsReport
import com.kuts.domain.common.catchWithCrashlyticsReport
import com.kuts.domain.entities.Deck
import com.kuts.domain.managers.IAudioPlayerManager
import com.kuts.domain.repositories.ICrashlyticsRepository
import com.kuts.domain.useCases.DeleteCardsFromDeckUseCase
import com.kuts.domain.useCases.FetchCardsUseCase
import com.kuts.domain.useCases.FetchDeckByIdUseCase
import com.kuts.domain.useCases.FetchDeckSourceUseCase
import com.kuts.domain.useCases.TransferCardsToDeckUseCase
import com.kuts.klaf.presentation.R
import com.kuts.klaf.common.EventMessage
import com.kuts.klaf.common.tryEmitAsNegative
import com.kuts.klaf.common.tryEmitAsPositive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CardTransferringViewModel(
    private val sourceDeckId: Int,
    fetchDeckById: FetchDeckByIdUseCase,
    private val fetchCards: FetchCardsUseCase,
    private val deleteCardsFromDeckUseCase: DeleteCardsFromDeckUseCase,
    fetchDeckSource: FetchDeckSourceUseCase,
    override val audioPlayer: IAudioPlayerManager,
    private val moveCardsToDeck: TransferCardsToDeckUseCase,
    private val crashlytics: ICrashlyticsRepository,
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

    override val navigationEvent = MutableSharedFlow<ICardTransferringNavigationEvent>()

    override val decks: StateFlow<List<Deck>> = fetchDeckSource()
        .catchWithCrashlyticsReport(crashlytics = crashlytics) {
            eventMessage.tryEmitAsNegative(resId = R.string.problem_fetching_decks)
        }.filterNotCurrentDecks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    override val listHeaderState = MutableStateFlow(value = ListHeaderState())

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
        subscribeToCardHoldersUpdated()
    }

    override fun sendAction(action: ICardTransferringAction) {
        when (action) {
            is ICardTransferringAction.ChangeSelectionState -> {
                changeCardSelectionState(position = action.position)
            }

            ICardTransferringAction.ChangeAllCardSelection -> {
                changeAllCardSelection()
            }

            ICardTransferringAction.DeleteCards -> {
                deleteCards()
            }

            is ICardTransferringAction.MoveCards -> {
                moveCards(targetDeck = action.targetDeck)
            }

            is ICardTransferringAction.NavigateTo -> {
                navigateTo(destination = action.destination)
            }

            is ICardTransferringAction.PronounceWord -> {
                pronounceWord(wordIndex = action.wordIndex)
            }

            ICardTransferringAction.ForeignWordVisibilityIconClick -> {
                handleForeignWordVisibilityIconClick()
            }

            ICardTransferringAction.NativeWordVisibilityIconClick -> {
                handleNativeWordVisibilityIconClick()
            }
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

    private fun subscribeToCardHoldersUpdated() {
        cardHolders.onEach { holders ->
            listHeaderState.update { state ->
                state.copy(isChecked = holders.all { it.isSelected })
            }
        }.flowOn(Dispatchers.IO)
            .launchIn(viewModelScope)
    }

    private fun changeCardSelectionState(position: Int) {
        cardHolders.update { holders ->
            val holder = holders[position]

            holders.toMutableList()
                .apply { this[position] = holder.copy(isSelected = !holder.isSelected) }
        }
    }

    private fun changeAllCardSelection() {
        val isAllSelected = cardHolders.value.all { it.isSelected }

        cardHolders.update { holders ->
            holders.map { holder -> holder.copy(isSelected = !isAllSelected) }
        }
    }

    private fun navigateTo(destination: ICardTransferringNavigationDestination) {
        when (destination) {
            ICardTransferringNavigationDestination.CardAddingScreen -> sendCardAddingScreenEvent()
            ICardTransferringNavigationDestination.CardDeletionDialog -> sendCardDeletingDialogEvent()
            ICardTransferringNavigationDestination.CardMovingDialog -> sendCardMovingDialogEvent()
            is ICardTransferringNavigationDestination.CardEditingScreen -> {
                sendCardEditingScreenEvent(selectedCardIndex = destination.selectedCardIndexIndex)
            }

            ICardTransferringNavigationDestination.CardTransferringScreen -> {
                emitEvent(event = ICardTransferringNavigationEvent.ToPrevious)
            }
        }
    }

    private fun deleteCards() {
        selectedCards.value
            .map { card -> card.id }
            .also { cardIds ->
                viewModelScope.launchWithState {
                    deleteCardsFromDeckUseCase(
                        cardIds = cardIds.toIntArray(),
                        deckId = sourceDeckId
                    )
                    eventMessage.tryEmitAsPositive(resId = R.string.message_deletion_completed_successfully)
                    navigationEvent.emit(value = ICardTransferringNavigationEvent.ToPrevious)
                }.onExceptionWithCrashlyticsReport(crashlytics = crashlytics) { _, _ ->
                    eventMessage.tryEmitAsNegative(resId = R.string.problem_with_removing_cards)
                }
            }
    }

    private fun moveCards(targetDeck: Deck) {
        viewModelScope.launchWithState {
            sourceDeck.replayCache.first()?.let { sourceDeck ->
                moveCardsToDeck(
                    sourceDeck = sourceDeck,
                    targetDeck = targetDeck,
                    cardsToMove = selectedCards.value.toTypedArray()
                )

                navigationEvent.emit(value = ICardTransferringNavigationEvent.ToPrevious)
                eventMessage.tryEmitAsPositive(resId = (R.string.message_transfer_completed_successfully))
            }
        }.onExceptionWithCrashlyticsReport(crashlytics = crashlytics) { _, _ ->
            eventMessage.tryEmitAsNegative(resId = R.string.problem_with_moving_cards)
        }
    }

    private fun pronounceWord(wordIndex: Int) {
        audioPlayer.preparePronunciationAndPlay(word = cardHolders.value[wordIndex].card.foreignWord)
    }

    private fun handleForeignWordVisibilityIconClick() {
        listHeaderState.update { state ->
            state.copy(foreignWordsVisible = !state.foreignWordsVisible)
        }
    }

    private fun handleNativeWordVisibilityIconClick() {
        listHeaderState.update { state ->
            state.copy(nativeWordsVisible = !state.nativeWordsVisible)
        }
    }

    private fun Flow<List<Deck>>.filterNotCurrentDecks(): Flow<List<Deck>> {
        return map { fetchedDecks ->
            fetchedDecks.filterNot { deck -> deck.id == sourceDeckId }
        }
    }

    private fun sendCardAddingScreenEvent() {
        emitEvent(
            event = ICardTransferringNavigationEvent.ToCardAddingScreen(sourceDeckId = sourceDeckId)
        )
    }

    private fun sendCardDeletingDialogEvent() {
        val cardForDeleting = selectedCards.value

        if (cardForDeleting.isEmpty()) {
            eventMessage.tryEmitAsNegative(resId = R.string.message_no_cards_selected)
        } else {
            emitEvent(
                event = ICardTransferringNavigationEvent.ToCardDeletingDialog(cardQuantity = cardForDeleting.size)
            )
        }
    }

    private fun sendCardMovingDialogEvent() {
        if (selectedCards.value.isEmpty()) {
            eventMessage.tryEmitAsNegative(resId = R.string.message_no_cards_selected)
        } else {
            emitEvent(event = ICardTransferringNavigationEvent.ToCardMovingDialog)
        }
    }

    private fun sendCardEditingScreenEvent(selectedCardIndex: Int) {
        val selectedCard = cardHolders.value[selectedCardIndex].card

        emitEvent(
            event = ICardTransferringNavigationEvent.ToCardEditingScreen(
                cardId = selectedCard.id,
                deckId = selectedCard.deckId
            )
        )
    }

    private fun emitEvent(event: ICardTransferringNavigationEvent) {
        viewModelScope.launch { navigationEvent.emit(value = event) }
    }
}
