package com.example.klaf.presentation.deckRepetition

import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.example.domain.common.*
import com.example.domain.common.CardRepetitionOrder.FOREIGN_TO_NATIVE
import com.example.domain.common.CardRepetitionOrder.NATIVE_TO_FOREIGN
import com.example.domain.common.CardSide.BACK
import com.example.domain.common.CardSide.FRONT
import com.example.domain.common.CoroutineStateHolder.Companion.launchWithState
import com.example.domain.common.CoroutineStateHolder.Companion.onExceptionWithCrashlyticsReport
import com.example.domain.entities.Card
import com.example.domain.entities.Deck
import com.example.domain.entities.DeckRepetitionInfo
import com.example.domain.enums.DifficultyRecallingLevel
import com.example.domain.enums.DifficultyRecallingLevel.*
import com.example.domain.repositories.CrashlyticsRepository
import com.example.domain.useCases.*
import com.example.klaf.R
import com.example.klaf.data.common.DeckRepetitionReminder.Companion.scheduleDeckRepetition
import com.example.klaf.data.common.calculateNextScheduledRepeatDate
import com.example.klaf.data.common.getNewInterval
import com.example.klaf.data.common.isRepetitionSucceeded
import com.example.klaf.data.common.lastIterationSuccessMark
import com.example.klaf.data.common.notifications.DeckRepetitionNotifier
import com.example.klaf.data.networking.CardAudioPlayer
import com.example.klaf.presentation.common.ButtonState
import com.example.klaf.presentation.common.EventMessage
import com.example.klaf.presentation.common.RepetitionTimer
import com.example.klaf.presentation.common.tryEmit
import com.example.klaf.presentation.deckRepetition.RepetitionScreenState.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import java.util.*

class DeckRepetitionViewModel @AssistedInject constructor(
    @Assisted private val deckId: Int,
    fetchCards: FetchCardsUseCase,
    fetchDeckById: FetchDeckByIdUseCase,
    override val timer: RepetitionTimer,
    override val audioPlayer: CardAudioPlayer,
    private val updateDeck: UpdateDeckUseCase,
    private val deleteCardsFromDeck: DeleteCardsFromDeckUseCase,
    private val workManager: WorkManager,
    private val saveDeckRepetitionInfo: SaveDeckRepetitionInfoUseCase,
    private val deckRepetitionNotifier: DeckRepetitionNotifier,
    private val crashlytics: CrashlyticsRepository,
) : BaseDeckRepetitionViewModel() {

    companion object {

        private const val ONE_QUARTER: Double = 1.0 / 4.0
        private const val THREE_QUADS: Double = 3.0 / 4.0
    }

    override val eventMessage = MutableSharedFlow<EventMessage>(extraBufferCapacity = 1)

    override val deck: SharedFlow<Deck?> = fetchDeckById(deckId = deckId)
        .catchWithCrashlyticsReport(crashlytics = crashlytics) {
            eventMessage.tryEmit(messageId = R.string.problem_with_fetching_deck)
        }.shareIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            replay = 1
        )

    override val mainButtonState = MutableStateFlow(value = ButtonState.UNPRESSED)
    override val screenState = MutableStateFlow<RepetitionScreenState>(StartState)

    private val cardsSource: SharedFlow<List<Card>> = fetchCards(deckId)
        .catchWithCrashlyticsReport(crashlytics = crashlytics) {
            eventMessage.tryEmit(messageId = R.string.problem_with_fetching_cards)
        }.shareIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            replay = 1
        )

    private val repetitionCards = MutableStateFlow<List<Card>>(LinkedList())

    private val currentCard = repetitionCards.map { cards -> cards.firstOrNull() }
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            replay = 1
        )

    private val cardSide = MutableStateFlow(FRONT)
    private val repetitionOrder = MutableStateFlow(value = NATIVE_TO_FOREIGN)

    override val cardState = combine(
        currentCard,
        cardSide,
        repetitionOrder,
    ) { card, side, repetitionOrder ->
        DeckRepetitionState(card = card, side = side, repetitionOrder = repetitionOrder)
    }.shareIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        replay = 1
    )

    private var startRepetitionCard: Card? = null
    private val goodeCards = mutableSetOf<Card>()
    private val hardCards = mutableSetOf<Card>()
    private var isWaitingForFinish = false
    private val savedProgressCards: MutableList<Card> = LinkedList()

    init {
        observeCardSource()
        observeCurrentCard()
    }

    override fun pronounceWord() {
        cardState.replayCache.firstOrNull()?.let { cardRepetitionState ->
            val repetitionOrder = cardRepetitionState.repetitionOrder
            val cardSide = cardRepetitionState.side

            if (
                (repetitionOrder == NATIVE_TO_FOREIGN && cardSide == BACK)
                || (repetitionOrder == FOREIGN_TO_NATIVE && cardSide == FRONT)
            ) {
                audioPlayer.play()
            }
        }
    }

    override fun startRepeating() {
        if (repetitionCards.value.isEmpty()) {
            eventMessage.tryEmit(messageId = R.string.problem_with_fetching_cards)
        } else if (screenState.value is StartState || screenState.value is FinishState) {
            startRepetitionCard = currentCard.replayCache.first()
            screenState.value = RepetitionState
            timer.runCounting()
            mainButtonState.value = ButtonState.UNPRESSED
        }
    }

    override fun turnCard() {
        cardSide.value = if (cardSide.value == FRONT) BACK else FRONT
    }

    override fun changeRepetitionOrder() {
        repetitionOrder.value = when (repetitionOrder.value) {
            NATIVE_TO_FOREIGN -> FOREIGN_TO_NATIVE
            else -> NATIVE_TO_FOREIGN
        }
    }

    override fun moveCardByDifficultyRecallingLevel(level: DifficultyRecallingLevel) {
        val cardForMoving = currentCard.replayCache.firstOrNull()

        if (repetitionCards.value.isEmpty()) {
            eventMessage.tryEmit(messageId = R.string.problem_with_fetching_cards)
        } else if (cardForMoving != null) {
            var actualLevel: DifficultyRecallingLevel = level

            when (level) {
                EASY -> {
                    if (cardForMoving in goodeCards) {
                        goodeCards.remove(cardForMoving)
                    } else if (cardForMoving in hardCards) {
                        hardCards.remove(cardForMoving)
                        goodeCards.add(cardForMoving)
                        actualLevel = GOOD
                    }
                }
                GOOD -> goodeCards.add(cardForMoving)
                HARD -> hardCards.add(cardForMoving)
            }

            timer.runCounting()
            repetitionCards.value =
                getUpdatedCardList(cardForMoving = cardForMoving, level = actualLevel)
            checkRepetitionStartPosition()

            if (actualLevel == EASY && mustRepetitionBeFinished()) {
                finishRepetition()
            }

            manageCardSide()
            saveRepetitionProgress(cards = repetitionCards.value)
            mainButtonState.value = ButtonState.UNPRESSED
        }
    }

    override fun resumeTimerCounting() {
        timer.resumeCounting()
    }

    override fun pauseTimerCounting() {
        timer.pauseCounting()
    }

    override fun deleteCard(cardId: Int, deckId: Int) {
        viewModelScope.launchWithState {
            deleteCardsFromDeck(deckId = deckId, cardIds = intArrayOf(cardId))
            eventMessage.tryEmit(messageId = R.string.card_has_been_deleted)
        }.onExceptionWithCrashlyticsReport(crashlytics = crashlytics) { _, _ ->
            eventMessage.tryEmit(messageId = R.string.problem_with_removing_card)
        }
    }

    override fun changeStateOnMainButtonClick() {
        when (mainButtonState.value) {
            ButtonState.PRESSED -> {
                mainButtonState.value = ButtonState.UNPRESSED
                timer.resumeCounting()
            }
            ButtonState.UNPRESSED -> {
                mainButtonState.value = ButtonState.PRESSED
                timer.pauseCounting()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timer.disableAndClear()
    }

    private fun manageCardSide() {
        cardState.replayCache.firstOrNull()?.let { cardState ->
            if (cardState.side == BACK) {
                turnCard()
            }
        }
    }

    private fun observeCardSource() {
        cardsSource.catchWithCrashlyticsReport(crashlytics = crashlytics) {
            eventMessage.tryEmit(messageId = R.string.problem_with_fetching_cards)
        }.onEach { receivedCards ->
            if (
                (screenState.value !is StartState)
                && (receivedCards.size != repetitionCards.value.size)
            ) {
                screenState.value = StartState
                timer.stopCounting()
            }

            repetitionCards.value = getCardsByProgress(receivedCards = receivedCards)
        }.launchIn(scope = viewModelScope, context = Dispatchers.IO)
    }

    private fun observeCurrentCard() {
        currentCard.filterNotNull()
            .onEach { card -> audioPlayer.preparePronunciation(word = card.foreignWord) }
            .catchWithCrashlyticsReport(crashlytics = crashlytics) {
                eventMessage.tryEmit(messageId = R.string.problem_with_fetching_card)
            }.launchIn(scope = viewModelScope, context = Dispatchers.IO)
    }

    private fun clearRepetitionProgress() {
        savedProgressCards.clear()
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
            GOOD -> (updatedCards.size * THREE_QUADS).toInt()
            HARD -> (updatedCards.size * ONE_QUARTER).toInt()
        }
    }

    private fun checkRepetitionStartPosition() {
        if (
            repetitionCards.value.firstOrNull()?.id == startRepetitionCard?.id
            && startRepetitionCard.isNotNull()
        ) {
            isWaitingForFinish = true
        }
    }

    private fun saveRepetitionProgress(cards: List<Card>) {
        savedProgressCards.update(cards)
    }

    private fun getCardsByProgress(receivedCards: List<Card>): List<Card> {
        val result = LinkedList<Card>()

        val newAddedCards = mutableListOf<Card>().apply {
            if (savedProgressCards.size < receivedCards.size) {

                receivedCards.forEach { receivedCard ->
                    if (!savedProgressCards.contains(receivedCard)) {
                        add(receivedCard)
                    }
                }
            }
        }

        val temporaryCardList = mutableListOf(*receivedCards.toTypedArray())
            .apply { removeAll(newAddedCards) }

        savedProgressCards.forEach { savedCard ->
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

        isWaitingForFinish = false
        clearRepetitionProgress()
        timer.stopCounting()

        val updatedDeck = getUpdatedDesk(deckForUpdating = repeatedDeck)

        viewModelScope.launchWithState {
            val (
                currentIterationDuration: Long,
                currentIterationSuccessMark: DeckRepetitionSuccessMark,
            ) = if (updatedDeck.repetitionQuantity.isEven()) {
                updatedDeck.lastRepetitionIterationDuration to updatedDeck.lastIterationSuccessMark
            } else {
                UNASSIGNED_LONG_VALUE to DeckRepetitionSuccessMark.UNASSIGNED
            }

            updateDeck(updatedDeck = updatedDeck)
            saveDeckRepetitionInfo(
                deckRepetitionInfo = DeckRepetitionInfo(
                    deckId = deckId,
                    currentDuration = currentIterationDuration,
                    previousDuration = repeatedDeck.lastRepetitionIterationDuration,
                    scheduledDate = updatedDeck.scheduledDate ?: UNASSIGNED_LONG_VALUE,
                    previousScheduledDate = repeatedDeck.scheduledDateOrUnassignedValue,
                    lastIterationDate = repeatedDeck.lastRepetitionIterationDate,
                    repetitionQuantity = updatedDeck.repetitionQuantity,
                    currentIterationSuccessMark = currentIterationSuccessMark,
                    previousIterationSuccessMark = repeatedDeck.lastIterationSuccessMark
                )
            )
            manageSchedulingAndNotificationState(
                repeatedDeck = repeatedDeck,
                updatedDeck = updatedDeck
            )
            screenState.value = FinishState
            screenState.value = StartState
        }.onExceptionWithCrashlyticsReport(crashlytics = crashlytics) { _, _ ->
            eventMessage.tryEmit(messageId = R.string.problem_with_updating_deck)
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

            val updatedScheduledDate =
                deckForUpdating.scheduledIterationDates.addIntoNewInstance(
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

    private fun manageSchedulingAndNotificationState(repeatedDeck: Deck, updatedDeck: Deck) {
        val currentTime = System.currentTimeMillis()
        val scheduledDate = updatedDeck.scheduledDate ?: return

        if (
            scheduledDate > currentTime
            && updatedDeck.repetitionQuantity >= MINIMUM_NUMBER_OF_FIRST_REPETITIONS
            && updatedDeck.repetitionQuantity.isEven()
        ) {
            workManager.scheduleDeckRepetition(
                deckName = repeatedDeck.name,
                deckId = repeatedDeck.id,
                atTime = scheduledDate
            )

            deckRepetitionNotifier.removeNotificationFromNotificationBar(deckId = repeatedDeck.id)
        }
    }
}