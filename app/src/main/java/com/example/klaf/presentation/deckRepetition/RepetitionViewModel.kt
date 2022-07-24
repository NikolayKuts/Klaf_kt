package com.example.klaf.presentation.deckRepetition

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.example.klaf.R
import com.example.klaf.data.common.DeckRepetitionReminder.Companion.scheduleDeckRepetition
import com.example.klaf.data.networking.CardAudioPlayer
import com.example.klaf.domain.common.*
import com.example.klaf.domain.common.CardRepetitionOrder.FOREIGN_TO_NATIVE
import com.example.klaf.domain.common.CardRepetitionOrder.NATIVE_TO_FOREIGN
import com.example.klaf.domain.common.CardSide.BACK
import com.example.klaf.domain.common.CardSide.FRONT
import com.example.klaf.domain.entities.Card
import com.example.klaf.domain.entities.Deck
import com.example.klaf.domain.enums.DifficultyRecallingLevel
import com.example.klaf.domain.enums.DifficultyRecallingLevel.*
import com.example.klaf.domain.useCases.FetchCardsUseCase
import com.example.klaf.domain.useCases.FetchDeckByIdUseCase
import com.example.klaf.domain.useCases.UpdateDeckUseCase
import com.example.klaf.presentation.common.EventMessage
import com.example.klaf.presentation.common.RepetitionTimer
import com.example.klaf.presentation.common.timeAsString
import com.example.klaf.presentation.common.tryEmit
import com.example.klaf.presentation.deckRepetition.RepetitionScreenState.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import java.io.IOException
import java.util.*


class RepetitionViewModel @AssistedInject constructor(
    @ApplicationContext private val context: Context,
    @Assisted deckId: Int,
    fetchCards: FetchCardsUseCase,
    fetchDeckById: FetchDeckByIdUseCase,
    val timer: RepetitionTimer,
    private val updateDeck: UpdateDeckUseCase,
    private val audioPlayer: CardAudioPlayer,
) : ViewModel() {

    companion object {

        private const val ONE_QUARTER: Double = 1.0 / 4.0
        private const val THREE_QUADS: Double = 3.0 / 4.0
    }

    private val _eventMessage = MutableSharedFlow<EventMessage>(extraBufferCapacity = 1)
    val eventMessage = _eventMessage.asSharedFlow()

    private val _savedProgressCards: MutableList<Card> = LinkedList()

    val deck: SharedFlow<Deck?> = fetchDeckById(deckId = deckId)
        .catch { _eventMessage.tryEmit(messageId = R.string.problem_with_fetching_deck) }
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            replay = 1
        )

    private val cardsSource: SharedFlow<List<Card>> = fetchCards(deckId)
        .catch { _eventMessage.tryEmit(messageId = R.string.problem_with_fetching_cards) }
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            replay = 1
        )

    private val repetitionCards = MutableStateFlow<List<Card>>(LinkedList())

    private val currentCard = repetitionCards.map { cards -> cards.firstOrNull() }
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            replay = 1
        )

    private val _screenState = MutableStateFlow<RepetitionScreenState>(StartState)
    val screenState = _screenState.asStateFlow()


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
        observeCardSource()
        observeCurrentCard()
    }

    fun pronounce() {
        cardState.replayCache.firstOrNull()?.let { cardRepetitionState ->
            val repetitionOrder = cardRepetitionState.repetitionOrder
            val cardSide = cardRepetitionState.side

            when {
                (repetitionOrder == NATIVE_TO_FOREIGN && cardSide == BACK)
                        || (repetitionOrder == FOREIGN_TO_NATIVE && cardSide == FRONT) -> {
                    audioPlayer.play()
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

            manageCardSide()
            saveRepetitionProgress(cards = repetitionCards.value)
        }
    }

    private fun manageCardSide() {
        cardState.replayCache.firstOrNull()?.let { cardState ->
            if (cardState.side == BACK) {
                turnCard()
            }
        }
    }

    private fun observeCardSource() {
        viewModelScope.launchWithExceptionHandler(
            onException = { _, throwable ->
                when (throwable) {
                    is IOException, is IllegalArgumentException -> {
                        _eventMessage.tryEmit(messageId = R.string.problem_with_playing_audio)
                    }
                }
                _eventMessage.tryEmit(messageId = R.string.problem_with_fetching_cards)
            }
        ) {
            cardsSource.collect { receivedCards ->
                repetitionCards.value = getCardsByProgress(receivedCards = receivedCards)

                if (receivedCards.isNotEmpty()) {
                    startRepetitionCard = receivedCards.first()
                }
            }
        }
    }

    private fun observeCurrentCard() {
        viewModelScope.launchWithExceptionHandler(
            context = Dispatchers.IO,
            onException = { _, _ ->
                _eventMessage.tryEmit(messageId = R.string.problem_with_fetching_card)
            }
        ) {
            currentCard.collect { card: Card? ->
                card?.let { audioPlayer.prepare(card = card) }
            }
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
            GOOD -> (updatedCards.size * ONE_QUARTER).toInt()
            HARD -> (updatedCards.size * THREE_QUADS).toInt()
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

        isWaitingForFinish = false
        clearRepetitionProgress()
        timer.stopCounting()

        viewModelScope.launchWithExceptionHandler(
            onException = { _, _ ->
                _eventMessage.tryEmit(messageId = R.string.problem_with_updating_deck)
            },
            onCompletion = {
                scheduleDeckRepetition(repeatedDeck = repeatedDeck, updatedDeck = updatedDeck)

                _screenState.value = FinishState(
                    currentDuration = timer.savedTotalTimeAsString,
                    lastDuration = repeatedDeck.lastRepeatDuration.timeAsString,
                    scheduledDate = updatedDeck.scheduledDate,
//                    scheduledDate = updatedDeck.scheduledDate.calculateDetailedScheduledRange(),
//                    scheduledDate = updatedDeck.scheduledDate.asFormattedDate(),
                    previusScheduledDate = repeatedDeck.scheduledDate,
//                    previusScheduledDate = repeatedDeck.scheduledDate.calculateDetailedScheduledRange(),
//                    previusScheduledDate = repeatedDeck.scheduledDate.asFormattedDate(),
                    lastRepetitionDate = repeatedDeck.lastRepeatDate.asFormattedDate(),
                    repetitionQuantity = updatedDeck.repeatQuantity.toString(),
                    lastSuccessMark = repeatedDeck.isLastRepetitionSucceeded.toString()
                )
            }
        ) {
            updateDeck(updatedDeck = updatedDeck)
        }
    }

    private fun getUpdatedDesk(deckForUpdating: Deck): Deck {
        val updatedLastRepetitionDate: Long
        val currentRepetitionDuration: Long
        val updatedLastSucceededRepetition: Boolean

        if (deckForUpdating.repeatQuantity % 2 != 0) {
            updatedLastRepetitionDate = getCurrentDateAsLong()
            currentRepetitionDuration = timer.savedTotalTime
            updatedLastSucceededRepetition = deckForUpdating.isRepetitionSucceeded(
                currentRepetitionDuration = currentRepetitionDuration
            )
        } else {
            updatedLastRepetitionDate = deckForUpdating.lastRepeatDate
            currentRepetitionDuration = deckForUpdating.lastRepeatDuration
            updatedLastSucceededRepetition = deckForUpdating.isLastRepetitionSucceeded
        }

        return deckForUpdating.copy(
            repeatDay = deckForUpdating.getUpdatedRepeatDay(),
            scheduledDate = deckForUpdating.calculateNextScheduledRepeatDate(
                currentRepetitionDuration
            ),
            lastRepeatDate = updatedLastRepetitionDate,
            repeatQuantity = deckForUpdating.repeatQuantity + 1,
            lastRepeatDuration = currentRepetitionDuration,
            isLastRepetitionSucceeded = updatedLastSucceededRepetition
        )
    }

    private fun scheduleDeckRepetition(repeatedDeck: Deck, updatedDeck: Deck) {
        val currentTime = System.currentTimeMillis()

        if (
            updatedDeck.scheduledDate > currentTime
            && updatedDeck.repeatQuantity >= MINIMUM_NUMBER_OF_FIRST_REPETITIONS
            && updatedDeck.repeatQuantity % 2 == 0
        ) {
            WorkManager.getInstance(context).scheduleDeckRepetition(
                deckName = repeatedDeck.name,
                deckId = repeatedDeck.id,
                scheduledTime = updatedDeck.scheduledDate
            )
        }
    }
}