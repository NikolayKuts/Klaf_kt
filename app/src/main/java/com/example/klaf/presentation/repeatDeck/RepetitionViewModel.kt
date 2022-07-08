package com.example.klaf.presentation.repeatDeck

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.klaf.R
import com.example.klaf.domain.common.CardRepetitionOrder
import com.example.klaf.domain.common.CardRepetitionOrder.*
import com.example.klaf.domain.common.CardRepetitionState
import com.example.klaf.domain.common.CardSide
import com.example.klaf.domain.common.CardSide.*
import com.example.klaf.domain.common.update
import com.example.klaf.domain.entities.Card
import com.example.klaf.domain.entities.Deck
import com.example.klaf.domain.useCases.FetchCardsUseCase
import com.example.klaf.domain.useCases.FetchDeckByIdUseCase
import com.example.klaf.domain.useCases.RemoveCardUseCase
import com.example.klaf.presentation.common.EventMessage
import com.example.klaf.presentation.common.log
import com.example.klaf.presentation.common.tryEmit
import com.example.klaf.presentation.repeatDeck.RepetitionScreenState.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class RepetitionViewModel @AssistedInject constructor(
    @Assisted deckId: Int,
    private val removeCard: RemoveCardUseCase,
    fetchCards: FetchCardsUseCase,
    fetchDeckById: FetchDeckByIdUseCase,
) : ViewModel() {

    private val _eventMessage = MutableSharedFlow<EventMessage>(extraBufferCapacity = 1)
    val eventMessage = _eventMessage.asSharedFlow()

    private val _savedProgressCards: MutableList<Card> = LinkedList()
    val savedProgressCards: List<Card> get() = _savedProgressCards

    val deck: SharedFlow<Deck?> = fetchDeckById(deckId = deckId).shareIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        replay = 1
    )

    val cardsSource: SharedFlow<List<Card>> = fetchCards(deckId).shareIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        replay = 1
    )

    private val repetitionCards: MutableList<Card> = LinkedList()

    private val _screenState = MutableStateFlow<RepetitionScreenState>(StartState)
    val screenState = _screenState.asStateFlow()

    private val _currentCard = MutableSharedFlow<Card>(extraBufferCapacity = 1)
    val currentCard = _currentCard.asSharedFlow()

    private val cardSide = MutableStateFlow(FRONT)
    private val repetitionOrder = MutableStateFlow(value = NATIVE_TO_FOREIGN)

    val cardState =
        combine(_currentCard, cardSide, repetitionOrder) { card, side, repetitionOrder ->
            CardRepetitionState(card = card, side = side, repetitionOrder = repetitionOrder)
        }.shareIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
        )

    init {
        log(message = "init replay cash -> ${cardsSource.replayCache}")
        viewModelScope.launch {
            cardsSource.collect { cards ->
                repetitionCards.addAll(cards)
                _currentCard.emit(repetitionCards[0])
                this.cancel()
            }
        }
    }

    fun startRepeating() {
        if (repetitionCards.isEmpty()) {
            _eventMessage.tryEmit(messageId = R.string.problem_with_fetching_cards)
        } else {
            when (_screenState.value) {
                StartState -> {
                    _screenState.value = RepetitionState
                }
                RepetitionState -> {
                    val cardForeTransferring = repetitionCards.first()
                    repetitionCards.removeFirst()
                    repetitionCards.add(cardForeTransferring)
                }
                FinishState -> {


                }
            }
        }
    }

    fun turnCard() {
        val side = if (cardSide.value == FRONT) BACK else FRONT
        cardSide.value = side
    }

    fun changeRepetitionOrder() {
        val order = if (repetitionOrder.value == NATIVE_TO_FOREIGN) {
            FOREIGN_TO_NATIVE
        } else {
            NATIVE_TO_FOREIGN
        }

        repetitionOrder.value = order
    }

    fun removeCard(cardId: Int) {
//        viewModelScope.launch { repository.deleteCard(cardId = cardId) }
    }
//
//    fun onGetDeck(deckId: Int, onDeckReceived: (Deck?) -> Unit) {
//        viewModelScope.launch { onDeckReceived(repository.getDeckById(deckId = deckId)) }
//    }

    fun saveRepetitionProgress(cards: List<Card>) {
        _savedProgressCards.update(cards)
    }

    fun clearProgress() {
        _savedProgressCards.clear()
    }

    fun getCardsByProgress(receivedCards: List<Card>): List<Card> {
        return LinkedList<Card>().apply result@{

            val newAddedCards = LinkedList<Card>().apply addedCards@{
                if (_savedProgressCards.size < receivedCards.size) {

                    receivedCards.forEach { receivedCard ->
                        if (!_savedProgressCards.contains(receivedCard)) {
                            this@addedCards.add(receivedCard)
                        }
                    }
                }
            }

            val temporary: MutableList<Card> = LinkedList(receivedCards).apply temporary@{
                this@temporary.removeAll(newAddedCards)
            }

            _savedProgressCards.forEach { savedCard ->
                temporary.forEach { relevantCard ->
                    if (relevantCard.id == savedCard.id) {
                        this@result.add(relevantCard)
                    }
                }
            }
            this@result.addAll(newAddedCards)
        }
    }
}