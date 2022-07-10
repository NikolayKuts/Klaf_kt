package com.example.klaf.presentation.repeatDeck

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.klaf.R
import com.example.klaf.domain.auxiliary.DateAssistant
import com.example.klaf.domain.common.CardRepetitionOrder.FOREIGN_TO_NATIVE
import com.example.klaf.domain.common.CardRepetitionOrder.NATIVE_TO_FOREIGN
import com.example.klaf.domain.common.CardRepetitionState
import com.example.klaf.domain.common.CardSide.BACK
import com.example.klaf.domain.common.CardSide.FRONT
import com.example.klaf.domain.common.update
import com.example.klaf.domain.entities.Card
import com.example.klaf.domain.entities.Deck
import com.example.klaf.domain.enums.DifficultyRecallingLevel
import com.example.klaf.domain.enums.DifficultyRecallingLevel.*
import com.example.klaf.domain.useCases.FetchCardsUseCase
import com.example.klaf.domain.useCases.FetchDeckByIdUseCase
import com.example.klaf.domain.useCases.UpdateDeckUseCase
import com.example.klaf.presentation.auxiliary.RepetitionTimer
import com.example.klaf.presentation.common.EventMessage
import com.example.klaf.presentation.common.tryEmit
import com.example.klaf.presentation.repeatDeck.RepetitionScreenState.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*


class RepetitionViewModel @AssistedInject constructor(
    @Assisted deckId: Int,
    fetchCards: FetchCardsUseCase,
    fetchDeckById: FetchDeckByIdUseCase,
    val timer: RepetitionTimer,
    private val updateDeck: UpdateDeckUseCase,
) : ViewModel() {

    private val _eventMessage = MutableSharedFlow<EventMessage>(extraBufferCapacity = 1)
    val eventMessage = _eventMessage.asSharedFlow()

    private val _savedProgressCards: MutableList<Card> = LinkedList()

    val deck: SharedFlow<Deck?> = fetchDeckById(deckId = deckId).shareIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        replay = 1
    )

    private val cardsSource: SharedFlow<List<Card>> = fetchCards(deckId).shareIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        replay = 1
    )

    private val repetitionCards = MutableStateFlow<List<Card>>(LinkedList())

    private val _screenState = MutableStateFlow<RepetitionScreenState>(StartState)
    val screenState = _screenState.asStateFlow()

    private val currentCard = repetitionCards.map { cards -> cards.firstOrNull() }
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            replay = 1
        )

    private val cardSide = MutableStateFlow(FRONT)
    private val repetitionOrder = MutableStateFlow(value = NATIVE_TO_FOREIGN)

    private var startRepetitionCard: Card? = null

    private val goodeCards = mutableSetOf<Card>()
    private val hardCards = mutableSetOf<Card>()
    private var isWaitingForFinish = false

    val cardState = combine(
        currentCard,
        cardSide,
        repetitionOrder,
    ) { card, side, repetitionOrder ->
        CardRepetitionState(card = card, side = side, repetitionOrder = repetitionOrder)
    }.shareIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        replay = 1
    )

    init {
        viewModelScope.launch {
            cardsSource.collect { cards ->
                repetitionCards.value = getCardsByProgress(receivedCards = cards)

                if (cards.isNotEmpty()) {
                    startRepetitionCard = cards.first()
                }
            }
        }
    }

    fun startRepeating() {
        if (repetitionCards.value.isEmpty()) {
            _eventMessage.tryEmit(messageId = R.string.problem_with_fetching_cards)
        } else if (_screenState.value is StartState || _screenState.value is FinishState) {
            startRepetitionCard = currentCard.replayCache.first()
            _screenState.value = RepetitionState
            timer.runCounting()
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

    fun moveCardByDifficultyRecallingLevel(level: DifficultyRecallingLevel) {
        val cardForMoving = currentCard.replayCache.first()

        if (repetitionCards.value.isEmpty()) {
            _eventMessage.tryEmit(messageId = R.string.problem_with_fetching_cards)
        } else if (cardForMoving != null) {
            val updatedCardList = getUpdatedCardList(cardForMoving = cardForMoving, level = level)

            repetitionCards.value = updatedCardList
            checkRepetitionStartPosition()

            when (level) {
                EASY -> {
                    if (goodeCards.contains(cardForMoving)) {
                        goodeCards.remove(cardForMoving)
                    } else if (hardCards.contains(cardForMoving)) {
                        hardCards.remove(cardForMoving)
                        goodeCards.add(cardForMoving)
                    }
                    if (mustRepetitionBeFinished()) {
                        finishRepetition()
                    }
                }
                GOOD -> goodeCards.add(cardForMoving)
                HARD -> hardCards.add(cardForMoving)
            }

            saveRepetitionProgress(cards = repetitionCards.value)
        }
    }

    private fun clearRepetitionProgress() {
        _savedProgressCards.clear()
    }

    private fun mustRepetitionBeFinished(): Boolean {
        return goodeCards.isEmpty()
                && hardCards.isEmpty()
                && (isWaitingForFinish)
    }

    private fun getUpdatedCardList(
        cardForMoving: Card,
        level: DifficultyRecallingLevel,
    ): List<Card> {
        return repetitionCards.value.toMutableList().apply {
            removeFirst()
            add(
                index = calculateNewPositionForMovingCard(level = level, updatedCards = this),
                element = cardForMoving
            )
        }
    }

    private fun calculateNewPositionForMovingCard(
        level: DifficultyRecallingLevel,
        updatedCards: List<Card>,
    ): Int {
        return when (level) {
            EASY -> updatedCards.size
            GOOD -> updatedCards.size * 3 / 4
            HARD -> updatedCards.size / 4
        }
    }

    private fun checkRepetitionStartPosition() {
        if (repetitionCards.value.first() == startRepetitionCard) {
            isWaitingForFinish = true
        }
    }

    private fun saveRepetitionProgress(cards: List<Card>) {
        _savedProgressCards.update(cards)
    }

    private fun getCardsByProgress(receivedCards: List<Card>): List<Card> {
        val result = LinkedList<Card>()

        val newAddedCards = mutableListOf<Card>().apply {
            if (_savedProgressCards.size < receivedCards.size) {

                receivedCards.forEach { receivedCard ->
                    if (!_savedProgressCards.contains(receivedCard)) {
                        add(receivedCard)
                    }
                }
            }
        }

        val temporaryCardList = mutableListOf(*receivedCards.toTypedArray())
            .apply { removeAll(newAddedCards) }

        _savedProgressCards.forEach { savedCard ->
            temporaryCardList.forEach { relevantCard ->
                if (relevantCard.id == savedCard.id) {
                    result.add(relevantCard)
                }
            }
        }
        result.addAll(newAddedCards)

        return result
    }

    private fun finishRepetition() {
        val repeatedDeck = deck.replayCache.firstOrNull()
            ?: throw Exception("The deck for updating is null")
        val updatedDeck = getUpdatedDesk(deckForUpdating = repeatedDeck)

        _screenState.value = FinishState
        isWaitingForFinish = false
        clearRepetitionProgress()
        timer.stopCounting()
        viewModelScope.launch { updateDeck(updatedDeck = updatedDeck) }
        scheduleDeckRepetition(repeatedDeck = repeatedDeck, updatedDeck = updatedDeck)
    }

    private fun getUpdatedDesk(deckForUpdating: Deck): Deck {
        val updatedLastRepetitionDate: Long
        val currentRepetitionDuration: Long
        val updatedLastSucceededRepetition: Boolean

        if (deckForUpdating.repeatQuantity % 2 != 0) {
            updatedLastRepetitionDate = DateAssistant.getCurrentDateAsLong()
            currentRepetitionDuration = timer.savedTotalTime
            updatedLastSucceededRepetition = DateAssistant.isRepetitionSucceeded(
                desk = deckForUpdating,
                currentRepetitionDuration = currentRepetitionDuration
            )
        } else {
            updatedLastRepetitionDate = deckForUpdating.lastRepeatDate
            currentRepetitionDuration = deckForUpdating.lastRepeatDuration
            updatedLastSucceededRepetition = deckForUpdating.isLastRepetitionSucceeded
        }

        return deckForUpdating.copy(
            repeatDay = DateAssistant.getUpdatedRepeatDay(deckForUpdating),
            scheduledDate = DateAssistant.getNextScheduledRepeatDate(deckForUpdating,
                currentRepetitionDuration),
            lastRepeatDate = updatedLastRepetitionDate,
            repeatQuantity = deckForUpdating.repeatQuantity + 1,
            lastRepeatDuration = currentRepetitionDuration,
            isLastRepetitionSucceeded = updatedLastSucceededRepetition
        )


//        return repeatDeck?.let { repeatDeck ->
//
//            Deck(
//                name = repeatDeck.name,
//                creationDate = repeatDeck.creationDate,
//                id = repeatDeck.id,
//                cardQuantity = repeatDeck.cardQuantity,
//                repeatDay = DateAssistant.getUpdatedRepeatDay(repeatDeck),
//                scheduledDate = newScheduledDate,
//                lastRepeatDate = updatedLastRepetitionDate,
//                repeatQuantity = repeatDeck.repeatQuantity + 1,
//                lastRepeatDuration = currentRepetitionDuration,
//                isLastRepetitionSucceeded = updatedLastSucceededRepetition
//            )
//        }
    }

    private fun scheduleDeckRepetition(repeatedDeck: Deck, updatedDeck: Deck) {
        val currentTime = System.currentTimeMillis()
        if (
            updatedDeck.scheduledDate > currentTime
            && repeatedDeck.repeatQuantity > 5
            && repeatedDeck.repeatQuantity % 2 == 0
        ) {
            //TODO implement scheduling deck repetition
        }
    }
}