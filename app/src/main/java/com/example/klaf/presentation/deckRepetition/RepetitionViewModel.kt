package com.example.klaf.presentation.deckRepetition

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
import com.example.klaf.presentation.common.*
import com.example.klaf.presentation.deckRepetition.RepetitionScreenState.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import java.io.IOException
import java.util.*

class RepetitionViewModel @AssistedInject constructor(
    @Assisted deckId: Int,
    fetchCards: FetchCardsUseCase,
    fetchDeckById: FetchDeckByIdUseCase,
    val timer: RepetitionTimer,
    private val updateDeck: UpdateDeckUseCase,
    private val audioPlayer: CardAudioPlayer,
    private val workManager: WorkManager,
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
        .catch { throwable ->
            _eventMessage.tryEmit(messageId = R.string.problem_with_fetching_cards)
        }
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

    fun moveToStartScreenState() {
        _screenState.value = StartState
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
        val cardForMoving = currentCard.replayCache.firstOrNull()

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
            onException = { _, throwable ->
                log(message = throwable.cause.toString())
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
            onException = { _, throwable ->
                _eventMessage.tryEmit(messageId = R.string.problem_with_updating_deck)
            },
            onCompletion = {
                scheduleDeckRepetition(repeatedDeck = repeatedDeck, updatedDeck = updatedDeck)

                val currentIterationDuration = if (updatedDeck.repetitionQuantity.isEven()) {

                    updatedDeck.lastRepetitionIterationDuration.timeAsString
                } else {
                    UNASSIGNED_STRING_VALUE
                }

                _screenState.value = FinishState(
                    currentDuration = currentIterationDuration,
                    previousDuration = repeatedDeck.lastRepetitionIterationDuration.timeAsString,
                    scheduledDate = updatedDeck.scheduledDate ?: UNASSIGNED_LONG_VALUE,
                    previousScheduledDate = repeatedDeck.scheduledDate ?: UNASSIGNED_LONG_VALUE,
                    lastRepetitionIterationDate = repeatedDeck.lastRepetitionIterationDate
                        ?.asFormattedDate(),
                    repetitionQuantity = updatedDeck.repetitionQuantity.toString(),
                    lastSuccessMark = repeatedDeck.isLastIterationSucceeded.toString()
                )
            }
        ) {
            updateDeck(updatedDeck = updatedDeck)
        }
    }

    private fun getUpdatedDesk(deckForUpdating: Deck): Deck {

        return if (deckForUpdating.repetitionQuantity.isEven()) {
            deckForUpdating.copy(
                repetitionQuantity = deckForUpdating.repetitionQuantity + 1,
                lastFirstRepetitionDuration = timer.savedTotalTime,
                lastSecondRepetitionDuration = 0,
            )

        } else {
            val updatedIterationDates = deckForUpdating.repetitionIterationDates
                .addIntoNewInstance(newElement = getCurrentDateAsLong())

            val updatedLastSecondRepetitionDuration = timer.savedTotalTime

            val updatedLastRepetitionIterationDuration =
                deckForUpdating.lastFirstRepetitionDuration + updatedLastSecondRepetitionDuration

            val updatedScheduledDate = deckForUpdating.scheduledIterationDates.addIntoNewInstance(
                newElement = deckForUpdating.calculateNextScheduledRepeatDate(
                    currentRepetitionIterationDuration = updatedLastRepetitionIterationDuration
                )
            )

            val updatedScheduledDateInterval = deckForUpdating.getNewInterval(
                currentIterationDuration = updatedLastRepetitionIterationDuration
            )

            val updatedIsLastIterationSucceeded = deckForUpdating.isRepetitionSucceeded(
                currentRepetitionDuration = timer.savedTotalTime
            )

            deckForUpdating.copy(
                repetitionIterationDates = updatedIterationDates,
                scheduledIterationDates = updatedScheduledDate,
                scheduledDateInterval = updatedScheduledDateInterval,
                repetitionQuantity = deckForUpdating.repetitionQuantity + 1,
                lastSecondRepetitionDuration = updatedLastSecondRepetitionDuration,
                lastRepetitionIterationDuration = updatedLastRepetitionIterationDuration,
                isLastIterationSucceeded = updatedIsLastIterationSucceeded,
            )
        }
    }

    private fun scheduleDeckRepetition(repeatedDeck: Deck, updatedDeck: Deck) {
        val currentTime = System.currentTimeMillis()
        val scheduledDate = updatedDeck.scheduledDate ?: return

        if (
            scheduledDate > currentTime
            && updatedDeck.repetitionQuantity >= MINIMUM_NUMBER_OF_FIRST_REPETITIONS
            && updatedDeck.repetitionQuantity.isEven()
        ) {
            log(message = "scheduling")
            log(message = scheduledDate.asFormattedDate() ?: "null", pointerMessage = "scheduled date")

            workManager.scheduleDeckRepetition(
                deckName = repeatedDeck.name,
                deckId = repeatedDeck.id,
                atTime = scheduledDate
            )
        }
    }
}