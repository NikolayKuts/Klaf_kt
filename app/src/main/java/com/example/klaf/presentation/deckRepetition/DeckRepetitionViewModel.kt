package com.example.klaf.presentation.deckRepetition

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
import com.example.klaf.domain.useCases.DeleteCardFromDeckUseCase
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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class DeckRepetitionViewModel @AssistedInject constructor(
    @Assisted deckId: Int,
    fetchCards: FetchCardsUseCase,
    fetchDeckById: FetchDeckByIdUseCase,
    override val timer: RepetitionTimer,
    override val audioPlayer: CardAudioPlayer,
    private val updateDeck: UpdateDeckUseCase,
    private val deleteCardFromDeck: DeleteCardFromDeckUseCase,
    private val workManager: WorkManager,
) : BaseDeckRepetitionViewModel() {

    companion object {

        private const val ONE_QUARTER: Double = 1.0 / 4.0
        private const val THREE_QUADS: Double = 3.0 / 4.0
    }

    override val eventMessage = MutableSharedFlow<EventMessage>(replay = 1)

    override val deck: SharedFlow<Deck?> = fetchDeckById(deckId = deckId)
        .catch { eventMessage.tryEmit(messageId = R.string.problem_with_fetching_deck) }
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            replay = 1
        )

    private val cardsSource: SharedFlow<List<Card>> = fetchCards(deckId)
        .catch { eventMessage.tryEmit(messageId = R.string.problem_with_fetching_cards) }
        .shareIn(
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

    override val screenState = MutableStateFlow<RepetitionScreenState>(StartState)

    private val savedProgressCards: MutableList<Card> = LinkedList()

    private val cardSide = MutableStateFlow(FRONT)
    private val repetitionOrder = MutableStateFlow(value = NATIVE_TO_FOREIGN)

    private var startRepetitionCard: Card? = null

    private val goodeCards = mutableSetOf<Card>()
    private val hardCards = mutableSetOf<Card>()
    private var isWaitingForFinish = false

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

    init {
        observeCardSource()
        observeCurrentCard()
    }

    override fun pronounceWord() {
        cardState.replayCache.firstOrNull()?.let { cardRepetitionState ->
            val repetitionOrder = cardRepetitionState.repetitionOrder
            val cardSide = cardRepetitionState.side

            if ((repetitionOrder == NATIVE_TO_FOREIGN && cardSide == BACK)
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
        }
    }

    override fun moveToStartScreenState() {
        screenState.value = StartState
    }

    override fun turnCard() {
        val side = if (cardSide.value == FRONT) BACK else FRONT
        cardSide.value = side
    }

    override fun changeRepetitionOrder() {
        val order = if (repetitionOrder.value == NATIVE_TO_FOREIGN) {
            FOREIGN_TO_NATIVE
        } else {
            NATIVE_TO_FOREIGN
        }

        repetitionOrder.value = order
    }

    override fun moveCardByDifficultyRecallingLevel(level: DifficultyRecallingLevel) {
        val cardForMoving = currentCard.replayCache.firstOrNull()

        if (repetitionCards.value.isEmpty()) {
            eventMessage.tryEmit(messageId = R.string.problem_with_fetching_cards)
        } else if (cardForMoving != null) {
            val updatedCardList =
                getUpdatedCardList(cardForMoving = cardForMoving, level = level)

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

    override fun resumeTimerCounting() {
        timer.resumeCounting()
    }

    override fun pauseTimerCounting() {
        timer.pauseCounting()
    }

    override fun deleteCard(cardId: Int, deckId: Int) {
        viewModelScope.launchWithExceptionHandler(
            onException = { _, _ ->
                eventMessage.tryEmit(messageId = R.string.problem_with_removing_card)
            },
            onCompletion = { eventMessage.tryEmit(messageId = R.string.card_has_been_deleted) }
        ) {
            deleteCardFromDeck(cardId = cardId, deckId = deckId)
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
            onException = { _, _ ->
                eventMessage.tryEmit(messageId = R.string.problem_with_fetching_cards)
            }
        ) {
            cardsSource.collect { receivedCards ->
                if (
                    (screenState.value !is StartState)
                    && (receivedCards.size != repetitionCards.value.size)
                ) {
                    screenState.value = StartState
                }

                repetitionCards.value = getCardsByProgress(receivedCards = receivedCards)
            }
        }
    }

    private fun observeCurrentCard() {
        currentCard.onEach { card: Card? ->
            card?.let { notNullCard ->
                audioPlayer.preparePronunciation(card = notNullCard)
            }
        }.catch { eventMessage.tryEmit(messageId = R.string.problem_with_fetching_card) }
            .launchIn(viewModelScope)
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
            GOOD -> (updatedCards.size * ONE_QUARTER).toInt()
            HARD -> (updatedCards.size * THREE_QUADS).toInt()
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

        viewModelScope.launchWithExceptionHandler(
            onException = { _, _ ->
                eventMessage.tryEmit(messageId = R.string.problem_with_updating_deck)
            },
            onCompletion = {
                scheduleDeckRepetition(repeatedDeck = repeatedDeck, updatedDeck = updatedDeck)

                val currentIterationDuration = if (updatedDeck.repetitionQuantity.isEven()) {
                    updatedDeck.lastRepetitionIterationDuration.timeAsString
                } else {
                    UNASSIGNED_STRING_VALUE
                }

                screenState.value = FinishState(
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

    private fun scheduleDeckRepetition(repeatedDeck: Deck, updatedDeck: Deck) {
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
        }
    }
}