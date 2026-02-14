package com.kuts.klaf.presentation.deckRepetition

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.kuts.domain.common.CardRepetitionOrder.FOREIGN_TO_NATIVE
import com.kuts.domain.common.CardRepetitionOrder.NATIVE_TO_FOREIGN
import com.kuts.domain.common.CardSide.BACK
import com.kuts.domain.common.CardSide.FRONT
import com.kuts.domain.common.CoroutineStateHolder.Companion.launchWithState
import com.kuts.domain.common.CoroutineStateHolder.Companion.onExceptionWithCrashlyticsReport
import com.kuts.domain.common.DeckRepetitionState
import com.kuts.domain.common.DeckReviewPassSuccessMark
import com.kuts.domain.common.LoadingState
import com.kuts.domain.common.MINIMUM_NUMBER_OF_FIRST_REPETITIONS
import com.kuts.domain.common.UNASSIGNED_LONG_VALUE
import com.kuts.domain.common.UnitSurrogate
import com.kuts.domain.common.addIntoNewInstance
import com.kuts.domain.common.calculateNextScheduledRepeatDate
import com.kuts.domain.common.catchWithCrashlyticsReport
import com.kuts.domain.common.getCurrentDateAsLong
import com.kuts.domain.common.getMaxTime
import com.kuts.domain.common.getNewInterval
import com.kuts.domain.common.isEven
import com.kuts.domain.common.isNotNull
import com.kuts.domain.common.isOdd
import com.kuts.domain.common.isRepetitionIterationSucceeded
import com.kuts.domain.common.lastReviewPassSuccessMark
import com.kuts.domain.common.launchIn
import com.kuts.domain.common.update
import com.kuts.domain.entities.Card
import com.kuts.domain.entities.Deck
import com.kuts.domain.entities.DeckRepetitionInfo
import com.kuts.domain.enums.DifficultyRecallingLevel
import com.kuts.domain.enums.DifficultyRecallingLevel.EASY
import com.kuts.domain.enums.DifficultyRecallingLevel.GOOD
import com.kuts.domain.enums.DifficultyRecallingLevel.HARD
import com.kuts.domain.managers.IAudioPlayerManager
import com.kuts.domain.managers.IDeckReviewNotifierManager
import com.kuts.domain.managers.IDeckReviewScheduler
import com.kuts.domain.repositories.ICrashlyticsRepository
import com.kuts.domain.useCases.DeleteCardsFromDeckUseCase
import com.kuts.domain.useCases.FetchCardsUseCase
import com.kuts.domain.useCases.FetchDeckByIdUseCase
import com.kuts.domain.useCases.SaveDeckReviewInfoUseCase
import com.kuts.domain.useCases.UpdateDeckUseCase
import com.kuts.klaf.presentation.R
import com.kuts.klaf.presentation.common.ButtonState
import com.kuts.klaf.presentation.common.EventMessage
import com.kuts.klaf.presentation.common.RepetitionTimer
import com.kuts.klaf.presentation.common.tryEmitAsNegative
import com.kuts.klaf.presentation.common.tryEmitAsPositive
import com.kuts.klaf.presentation.deckRepetition.RepetitionScreenState.FinishState
import com.kuts.klaf.presentation.deckRepetition.RepetitionScreenState.RepetitionState
import com.kuts.klaf.presentation.deckRepetition.RepetitionScreenState.StartState
import com.kuts.klaf.presentation.deckRepetition.savedStateHandle.deckReviewDelegates
import com.kuts.klaf.presentation.deckRepetitionInfo.RepetitionInfoEvent
import com.lib.lokdroid.core.logD
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.LinkedList

class DeckReviewViewModel @AssistedInject constructor(
    @Assisted private val deckId: Int,
    @Assisted private val handle: SavedStateHandle,
    fetchCards: FetchCardsUseCase,
    fetchDeckById: FetchDeckByIdUseCase,
    override val timer: RepetitionTimer,
    override val audioPlayer: IAudioPlayerManager,
    private val updateDeck: UpdateDeckUseCase,
    private val deleteCardsFromDeck: DeleteCardsFromDeckUseCase,
    private val deckReviewScheduler: IDeckReviewScheduler,
    private val saveDeckReviewInfo: SaveDeckReviewInfoUseCase,
    private val deckReviewNotifier: IDeckReviewNotifierManager,
    private val crashlytics: ICrashlyticsRepository,
) : BaseDeckReviewViewModel() {

    companion object Companion {

        private const val HARD_WORD_POSITION_SHIFT = 5
        private const val GOOD_WORD_POSITION_SHIFT = 10
    }

    private val handleDelegates = handle.deckReviewDelegates

    override val eventMessage = MutableSharedFlow<EventMessage>(extraBufferCapacity = 1)

    override val deck: SharedFlow<Deck?> = fetchDeckById(deckId = deckId)
        .catchWithCrashlyticsReport(crashlytics = crashlytics) {
            eventMessage.tryEmitAsNegative(resId = R.string.problem_with_fetching_deck)
        }.shareIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            replay = 1
        )

    override val mainButtonState: MutableStateFlow<ButtonState> = handleDelegates.mainButtonState

    override val screenState = handleDelegates.screenState.apply {
        if (replayCache.isEmpty()) {
            viewModelScope.launch { emit(StartState) }
        }
    }
    override val cardDeletingState = handleDelegates.cardDeletingState

    private val cardsSource: SharedFlow<List<Card>> = fetchCards(deckId)
        .catchWithCrashlyticsReport(crashlytics = crashlytics) {
            eventMessage.tryEmitAsNegative(resId = R.string.problem_with_fetching_cards)
        }.shareIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            replay = 1
        )

    private val cardsToReview = handleDelegates.repetitionCards

    private val currentCard = cardsToReview.map { cards -> cards.firstOrNull() }
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            replay = 1
        )

    private val cardSide = handleDelegates.cardSide
    private val repetitionOrder = handleDelegates.repetitionOrder

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

    private val reviewedCardIds = handleDelegates.reviewedCardIds

    override val deckReviewState = handleDelegates.deckReviewState

    private val goodeCardsIds = mutableSetOf<Int>()
    private val hardCardsIds = mutableSetOf<Int>()

    init {
        timer.setStartTime(time = handleDelegates.timerTime)
        manageMainButtonStateBySavedTimerState()
        observeCardSource()
        observeCurrentCard()
        observeReviewedCardIdList()
        observeDeck()
        observeTimerState()
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
        logD("startRepeating() called")

        val currentScreenState = screenState.replayCache.firstOrNull()

        viewModelScope.launch(Dispatchers.IO) {
            if (cardsToReview.value.isEmpty()) {
                eventMessage.tryEmitAsNegative(resId = R.string.problem_with_fetching_cards)
            } else if (currentScreenState is StartState || currentScreenState is FinishState) {
                handleDelegates.startRepetitionCard = currentCard.replayCache.first()
                handleDelegates.lastRepetitionCard = cardsToReview.value.last()
                screenState.emit(RepetitionState)
                timer.runCounting()
                mainButtonState.value = ButtonState.UNPRESSED

                reviewedCardIds.update {
                    handleDelegates.startRepetitionCard?.id?.let { setOf(it) }
                        ?: emptySet()
                }
            }
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

        if (cardsToReview.value.isEmpty()) {
            eventMessage.tryEmitAsNegative(resId = R.string.problem_with_fetching_cards)
        } else if (cardForMoving != null) {
            var actualLevel: DifficultyRecallingLevel = level
            logD {
                message("cardForMoving: $cardForMoving")
                message("goodeCardsIds: $goodeCardsIds")
                message("hardCardsIds: $hardCardsIds")
            }

            when (level) {
                EASY -> {
                    if (cardForMoving.id in goodeCardsIds) {
                        goodeCardsIds.remove(cardForMoving.id)
                    } else if (cardForMoving.id in hardCardsIds) {
                        hardCardsIds.remove(cardForMoving.id)
                        goodeCardsIds.add(cardForMoving.id)
                        actualLevel = GOOD
                    }
                }

                GOOD -> goodeCardsIds.add(cardForMoving.id)
                HARD -> hardCardsIds.add(cardForMoving.id)
            }

            timer.runCounting()
            cardsToReview.value = getUpdatedCardList(
                cardForMoving = cardForMoving,
                level = actualLevel
            )

            reviewedCardIds.update { list ->
                cardsToReview.value.firstOrNull()?.let { list + it.id } ?: list
            }

            checkRepetitionStartPosition()
            manageAllCardRepeatedState()

            if (actualLevel == EASY && mustRepetitionBeFinished()) {
                finishRepetition()
            }

            manageCardSide()
            saveRepetitionProgress(cards = cardsToReview.value)
            mainButtonState.value = ButtonState.UNPRESSED
        }
    }

    override fun deleteCard(cardId: Int, deckId: Int) {
        viewModelScope.launchWithState {
            cardDeletingState.value = LoadingState.Loading
            deleteCardsFromDeck(deckId = deckId, cardIds = intArrayOf(cardId))
            eventMessage.tryEmitAsPositive(resId = R.string.card_has_been_deleted)
            cardDeletingState.value = LoadingState.Success(data = UnitSurrogate)
        }.onExceptionWithCrashlyticsReport(crashlytics = crashlytics) { _, t ->
            cardDeletingState.value = LoadingState.Non
            eventMessage.tryEmitAsNegative(resId = R.string.problem_with_removing_card)
        }
    }

    override fun changeButtonsStateOnCommonButtonClick() {
        when (mainButtonState.value) {
            ButtonState.PRESSED -> {
                mainButtonState.value = ButtonState.UNPRESSED
                resumeTimerCounting()
            }

            ButtonState.UNPRESSED -> {
                mainButtonState.value = ButtonState.PRESSED
                pauseTimerCounting()
            }
        }
    }

    override fun resumeTimerCounting() {
        val currentScreenState = screenState.replayCache.firstOrNull()

        if (mainButtonState.value == ButtonState.UNPRESSED && currentScreenState == RepetitionState) {
            timer.resumeCounting()
        }
    }

    override fun pauseTimerCounting() {
        timer.pauseCounting()
    }

    override fun onCleared() {
        super.onCleared()
        timer.disableAndClear()
    }

    override fun resetScreenState() {
        viewModelScope.launch { screenState.emit(StartState) }
    }

    private fun manageCardSide() {
        cardState.replayCache.firstOrNull()?.let { cardState ->
            if (cardState.side == BACK) {
                turnCard()
            }
        }
    }

    private fun observeCardSource() {
        var isFirstEmissionAfterRestore = cardsToReview.value.isNotEmpty()

        cardsSource.catchWithCrashlyticsReport(crashlytics = crashlytics) {
            eventMessage.tryEmitAsNegative(resId = R.string.problem_with_fetching_cards)
        }.onEach { receivedCards ->
            val currentScreenState = screenState.replayCache.firstOrNull() ?: return@onEach

            if (isFirstEmissionAfterRestore) {
                isFirstEmissionAfterRestore = false
                return@onEach
            }

            val shouldResetToStartState = currentScreenState !is StartState &&
                        receivedCards.size != cardsToReview.value.size

            if (shouldResetToStartState) {
                screenState.emit(StartState)
                timer.stopCounting()
            }

            cardsToReview.value = getCardsByProgress(receivedCards = receivedCards.shuffled())
        }.launchIn(scope = viewModelScope, context = Dispatchers.IO)
    }

    private fun observeCurrentCard() {
        currentCard.filterNotNull()
            .onEach { card -> audioPlayer.preparePronunciation(word = card.foreignWord) }
            .catchWithCrashlyticsReport(crashlytics = crashlytics) {
                eventMessage.tryEmitAsNegative(resId = R.string.problem_with_fetching_card)
            }.launchIn(scope = viewModelScope, context = Dispatchers.IO)
    }

    private fun observeReviewedCardIdList() {
        reviewedCardIds.map { it.size }
            .onEach { deckReviewState.update { state -> state.copy(reviewedCardsCount = it) } }
            .flowOn(Dispatchers.IO)
            .launchIn(scope = viewModelScope)
    }

    private fun observeDeck() {
        viewModelScope.launch(Dispatchers.IO) {
            deck.collect {
                if (it != null) {
                    deckReviewState.update { state ->
                        val leftTime = if (it.reviewCount.isOdd()) {
                            handleDelegates.savedTime = it.lastFirstReviewDuration
                            (it.getMaxTime()) - (handleDelegates.savedTime)
                        } else {
                            (it.getMaxTime())
                        }
                        state.copy(
                            maxTime = it.getMaxTime(),
                            leftTime = leftTime,
                        )
                    }
                }
            }
        }
    }

    private fun observeTimerState() {
        viewModelScope.launch(Dispatchers.IO) {
            timer.timerState.collect { timerState ->
                deck.firstOrNull()?.let {
                    handleDelegates.timerTime = timerState.totalSeconds
                    deckReviewState.update { state ->
                        state.copy(
                            leftTime = (it.getMaxTime()) - (handleDelegates.savedTime + timerState.totalSeconds)
                        )
                    }
                }
            }
        }
    }

    private fun manageMainButtonStateBySavedTimerState() {
        if (handleDelegates.timerTime > 0) {
            handleDelegates.mainButtonState.value = ButtonState.PRESSED
        }
    }

    private fun clearRepetitionProgress() {
        handleDelegates.savedProgressCards.clear()
    }

    private fun mustRepetitionBeFinished(): Boolean {
        return goodeCardsIds.isEmpty()
                && hardCardsIds.isEmpty()
                && handleDelegates.isWaitingForFinish
                && handleDelegates.isAllCardsRepeated
    }

    private fun getUpdatedCardList(
        cardForMoving: Card,
        level: DifficultyRecallingLevel,
    ): List<Card> {
        return cardsToReview.value.toMutableList().apply {
            removeAt(0)
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

        val calculateNewPosition: (positionShift: Int) -> Int = { positionShift ->
            if (positionShift >= updatedCards.lastIndex) updatedCards.lastIndex else positionShift
        }
        return when (level) {
            EASY -> updatedCards.size
            GOOD -> calculateNewPosition(GOOD_WORD_POSITION_SHIFT)
            HARD -> calculateNewPosition(HARD_WORD_POSITION_SHIFT)
        }
    }

    private fun checkRepetitionStartPosition() {
        if (
            cardsToReview.value.firstOrNull()?.id == handleDelegates.startRepetitionCard?.id
            && handleDelegates.startRepetitionCard.isNotNull()
        ) {
            handleDelegates.isWaitingForFinish = true
        }
    }

    private fun manageAllCardRepeatedState() {
        if (cardsToReview.value.firstOrNull()?.id == handleDelegates.lastRepetitionCard?.id) {
            handleDelegates.isAllCardsRepeated = true
        }
    }

    private fun saveRepetitionProgress(cards: List<Card>) {
        handleDelegates.savedProgressCards.update(cards)
    }

    private fun getCardsByProgress(receivedCards: List<Card>): List<Card> {
        val result = LinkedList<Card>()

        val newAddedCards = mutableListOf<Card>().apply {
            if (handleDelegates.savedProgressCards.size < receivedCards.size) {

                receivedCards.forEach { receivedCard ->
                    if (!handleDelegates.savedProgressCards.contains(receivedCard)) {
                        add(receivedCard)
                    }
                }
            }
        }

        val temporaryCardList = mutableListOf(*receivedCards.toTypedArray())
            .apply { removeAll(newAddedCards) }

        handleDelegates.savedProgressCards.forEach { savedCard ->
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

        logD("finishRepetition() called")

        viewModelScope.launchWithState(Dispatchers.IO) {
            screenState.emit(FinishState(repetitionInfoEvent = RepetitionInfoEvent.Non))
            handleDelegates.isWaitingForFinish = false
            clearRepetitionProgress()
            timer.stopCounting()
            cardsToReview.update { it.shuffled() }

            val updatedDeck = getUpdatedDesk(deckForUpdating = repeatedDeck)
            logD("is repetition Even (repeated) -> ${repeatedDeck.reviewCount.isEven()}")
            logD("is repetition Even (updated) -> ${updatedDeck.reviewCount.isEven()}")
            logD("repeatedDeck -> $repeatedDeck")
            logD("updatedDeck -> $updatedDeck")

            val (
                currentIterationDuration: Long,
                currentIterationSuccessMark: DeckReviewPassSuccessMark,
            ) = if (updatedDeck.reviewCount.isEven()) {
                if (updatedDeck.lastReviewPassSuccessMark == DeckReviewPassSuccessMark.SUCCESS) {
                    updatedDeck.lastReviewPassDuration
                } else {
                    updatedDeck.lastFirstReviewDuration + updatedDeck.lastSecondReviewDuration
                } to updatedDeck.lastReviewPassSuccessMark
            } else {
                UNASSIGNED_LONG_VALUE to DeckReviewPassSuccessMark.UNASSIGNED
            }

            updateDeck.invoke(updatedDeck = updatedDeck)

            logD("Deck updated successfully")
            logD("currentIterationSuccessMark for DeckRepetitionInfo -> $currentIterationSuccessMark")

            val deckRepetitionInfo = DeckRepetitionInfo(
                deckId = deckId,
                currentDuration = currentIterationDuration,
                previousDuration = repeatedDeck.lastReviewPassDuration,
                scheduledDate = updatedDeck.scheduledDate ?: UNASSIGNED_LONG_VALUE,
                previousScheduledDate = repeatedDeck.scheduledDateOrUnassignedValue,
                lastIterationDate = repeatedDeck.lastRepetitionIterationDate,
                repetitionQuantity = updatedDeck.reviewCount,
                currentIterationSuccessMark = currentIterationSuccessMark,
                previousIterationSuccessMark = repeatedDeck.lastReviewPassSuccessMark
            )

            logD("deckRepetitionInfo -> $deckRepetitionInfo")

            saveDeckReviewInfo.invoke(deckRepetitionInfo = deckRepetitionInfo)
            logD("Deck repetition info saved successfully")

            val infoEvent = manageSchedulingAndNotificationState(
                repeatedDeck = repeatedDeck,
                updatedDeck = updatedDeck,
            )

            screenState.emit(FinishState(repetitionInfoEvent = infoEvent))
            resetScreenState()
        }.onExceptionWithCrashlyticsReport(crashlytics = crashlytics) { _, _ ->
            eventMessage.tryEmitAsNegative(resId = R.string.problem_with_updating_deck)
        }
    }

    private fun getUpdatedDesk(deckForUpdating: Deck): Deck {
        logD("getUpdatedDesk() called")

        val initialisedScheduledIterationDates = deckForUpdating.scheduledReviewDates.ifEmpty {
            listOf(getCurrentDateAsLong())
        }

        val increasedRepetitionQuantity = deckForUpdating.reviewCount + 1

        return if (deckForUpdating.reviewCount.isEven()) {
            deckForUpdating.copy(
                reviewCount = increasedRepetitionQuantity,
                lastFirstReviewDuration = timer.savedTotalTimeInSeconds,
                lastSecondReviewDuration = 0,
                scheduledReviewDates = initialisedScheduledIterationDates,
            )
        } else {
            val updatedIterationDates = deckForUpdating.reviewPassDates
                .addIntoNewInstance(newElement = getCurrentDateAsLong())

            val updatedLastSecondRepetitionDuration = timer.savedTotalTimeInSeconds

            val updatedLastRepetitionIterationDuration =
                deckForUpdating.lastFirstReviewDuration + updatedLastSecondRepetitionDuration

            val updatedScheduledDate = initialisedScheduledIterationDates.addIntoNewInstance(
                newElement = deckForUpdating.calculateNextScheduledRepeatDate(
                    currentRepetitionIterationDuration = updatedLastRepetitionIterationDuration
                )
            )

            val updatedScheduledDateInterval = deckForUpdating.getNewInterval(
                currentIterationDuration = updatedLastRepetitionIterationDuration
            )

            val updatedIsLastIterationSucceeded = deckForUpdating.isRepetitionIterationSucceeded(
                currentRepetitionDuration = updatedLastRepetitionIterationDuration
            )

            logD("updatedIsLastIterationSucceeded -> $updatedIsLastIterationSucceeded")

            // TODO("change condition expression")
            val duration =
                if (updatedIsLastIterationSucceeded || increasedRepetitionQuantity == 6) {
                    updatedLastRepetitionIterationDuration
                } else {
                    deckForUpdating.lastReviewPassDuration
                }

            deckForUpdating.copy(
                reviewPassDates = updatedIterationDates,
                scheduledReviewDates = updatedScheduledDate,
                scheduledDateInterval = updatedScheduledDateInterval,
                reviewCount = increasedRepetitionQuantity,
                lastSecondReviewDuration = updatedLastSecondRepetitionDuration,
                lastReviewPassDuration = duration,
                isLastPassSucceeded = updatedIsLastIterationSucceeded,
            )
        }
    }

    private suspend fun manageSchedulingAndNotificationState(
        repeatedDeck: Deck,
        updatedDeck: Deck,
    ): RepetitionInfoEvent = try {
        withContext(Dispatchers.IO) {
            val currentTime = System.currentTimeMillis()
            val scheduledDate = updatedDeck.scheduledDate ?: currentTime

            val isIterationFinished = scheduledDate > currentTime
                    && updatedDeck.reviewCount >= MINIMUM_NUMBER_OF_FIRST_REPETITIONS
                    && updatedDeck.reviewCount.isEven()

            if (isIterationFinished) {
                deckReviewScheduler.schedule(
                    deckName = repeatedDeck.name,
                    deckId = repeatedDeck.id,
                    atTime = scheduledDate
                )

                deckReviewNotifier.removeNotificationFromNotificationBar(deckId = repeatedDeck.id)
                RepetitionInfoEvent.ScheduledSuccessfully
            } else {
                RepetitionInfoEvent.OneRepetitionToFinish
            }
        }
    } catch (e: Exception) {
        RepetitionInfoEvent.SchedulingFailed
    }
}
