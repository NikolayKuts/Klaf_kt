package com.kuts.klaf.deckManagment

import androidx.lifecycle.viewModelScope
import com.kuts.domain.common.CoroutineStateHolder.Companion.launchWithState
import com.kuts.domain.common.CoroutineStateHolder.Companion.onException
import com.kuts.domain.common.ICoroutineContextProvider
import com.kuts.domain.common.calculateDetailedScheduledInterval
import com.kuts.domain.common.calculateDetailedScheduledIntervalAsLong
import com.kuts.domain.common.catchWithCrashlyticsReport
import com.kuts.domain.common.getCurrentDateAsLong
import com.kuts.domain.entities.Deck
import com.kuts.domain.managers.IDeckReviewNotifierManager
import com.kuts.domain.managers.IDeckReviewScheduler
import com.kuts.domain.repositories.ICrashlyticsRepository
import com.kuts.domain.useCases.FetchDeckByIdUseCase
import com.kuts.domain.useCases.UpdateDeckUseCase
import com.kuts.klaf.presentation.resources.*
import com.kuts.klaf.common.EventMessage
import com.kuts.klaf.common.tryEmitAsNegative
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

private const val SCHEDULED_REVIEW_UPDATE_SUCCESS = "Scheduled review has been updated"
private const val SCHEDULED_REVIEW_UPDATE_FAILURE = "Scheduled review was not updated"

class DeckManagementViewModel(
    private val deckId: Int,
    private val fetchDeckById: FetchDeckByIdUseCase,
    private val updateDeck: UpdateDeckUseCase,
    private val deckReviewScheduler: IDeckReviewScheduler,
    private val deckReviewNotifier: IDeckReviewNotifierManager,
    private val crashlytics: ICrashlyticsRepository,
    private val coroutineContextProvider: ICoroutineContextProvider,
) : BaseDeckManagementViewModel() {

    override val eventMessage = MutableSharedFlow<EventMessage>(extraBufferCapacity = 1)

    override val event = MutableStateFlow<IDeckManagementEvent>(value = IDeckManagementEvent.None)

    override val deckManagementState = MutableStateFlow(DeckManagementState())

    private val deck = MutableStateFlow<Deck?>(null)

    init {
        subscribeToDeckUpdates()
    }

    private fun subscribeToDeckUpdates() {
        fetchDeckById(deckId = deckId)
            .catchWithCrashlyticsReport(crashlytics = crashlytics) { throwable ->
                // logE("Failed to fetch deck for management\n${throwable.stackTraceToString()}")
                eventMessage.tryEmitAsNegative(resId = Res.string.problem_with_fetching_deck)
            }.onEach { receivedDeck ->
                // logE("receivedDeck: $receivedDeck")

                deck.value = receivedDeck
                receivedDeck?.let { deck -> updateDeckState(deck = deck) }
            }.flowOn(coroutineContextProvider.io)
            .launchIn(viewModelScope)
    }

    private fun updateDeckState(deck: Deck) {
        deckManagementState.update {
            it.copy(
                name = it.name.copy(value = deck.name),
                creationDate = it.creationDate.copy(value = deck.creationDate),
                repetitionIterationDates = it.repetitionIterationDates,
                scheduledIterationDates = it.scheduledIterationDates,
                scheduledDateInterval = it.scheduledDateInterval.copy(value = deck.scheduledDateInterval.calculateDetailedScheduledInterval()),
                scheduledReview = it.scheduledReview.copy(value = deck.scheduledDate),
                repetitionQuantity = it.repetitionQuantity.copy(value = deck.reviewCount.toString()),
                cardQuantity = it.cardQuantity.copy(value = deck.cardQuantity.toString()),
                lastFirstRepetitionDuration = it.lastFirstRepetitionDuration.copy(value = deck.lastFirstReviewDuration.toString()),
                lastSecondRepetitionDuration = it.lastSecondRepetitionDuration.copy(value = deck.lastSecondReviewDuration.toString()),
                lastRepetitionIterationDuration = it.lastRepetitionIterationDuration.copy(value = deck.lastReviewPassDuration.toString()),
                isLastIterationSucceeded = it.isLastIterationSucceeded.copy(value = deck.isLastPassSucceeded.toString()),
                id = it.id.copy(value = deck.id.toString()),
            )
        }
    }

    override fun sendAction(action: IDeckManagementAction) {
        // logD("sendAction() called. Action: $action")

        when (action) {
            is IDeckManagementAction.ScheduledDateIntervalChangeRequested -> {
                deckManagementState.update { managementState ->
                    managementState.copy(
                        scheduledDateIntervalChangeState = IDateDataChangeState.Required(
                            dateData = managementState.scheduledDateInterval.value
                        )
                    )
                }
            }

            is IDeckManagementAction.DismissScheduledDateIntervalDialog -> {
                deckManagementState.update { state ->
                    state.copy(scheduledDateIntervalChangeState = IDateDataChangeState.NotRequired)
                }
            }

            IDeckManagementAction.ScheduledDateIntervalChangeConfirmed -> {
                val scheduledDateIntervalChangeState =
                    deckManagementState.value.scheduledDateIntervalChangeState

                if (scheduledDateIntervalChangeState is IDateDataChangeState.Required) {
                    val validatedDateData = DateDataValidator().validateForSaving(
                        dateData = scheduledDateIntervalChangeState.dateData
                    )

                    if (validatedDateData == deckManagementState.value.scheduledDateInterval.value) {
                        // TODO: send event message
//                        eventMessage.tryEmitAsNeutral()
//                        return
                    }

                    viewModelScope.launchWithState(coroutineContextProvider.io) {
                        val interval = validatedDateData.calculateDetailedScheduledIntervalAsLong()

                        updateDeck(updatedDeck = deck.value!!.copy(scheduledDateInterval = interval))
                        deckManagementState.update { managementState ->
                            managementState.copy(
                                scheduledDateIntervalChangeState = IDateDataChangeState.NotRequired
                            )
                        }
                    }.onException { _, throwable ->
                        // logE("Exception: ${throwable.stackTraceToString()}")
                    }

                }
            }

            is IDeckManagementAction.ScheduledDateIntervalChanged -> {
                val scheduledDateIntervalChangeState =
                    deckManagementState.value.scheduledDateIntervalChangeState

                if (scheduledDateIntervalChangeState is IDateDataChangeState.Required) {
                    deckManagementState.update { managementState ->
                        val sourceDateDate = scheduledDateIntervalChangeState.dateData

                        val updatedDateData = ButtonActionHandler().handle(
                            dateData = sourceDateDate,
                            dataUnit = action.dateUnit,
                            buttonAction = action.buttonAction
                        )
                        val updatedScheduledDateIntervalChangeState =
                            IDateDataChangeState.Required(dateData = updatedDateData)

                        managementState.copy(
                            scheduledDateIntervalChangeState = updatedScheduledDateIntervalChangeState
                        )
                    }
                }
            }

            IDeckManagementAction.ScheduledReviewChangeRequested -> {
                val scheduledDate = deck.value?.scheduledDate ?: return
                val currentTime = getCurrentDateAsLong()
                if (scheduledDate < currentTime) return

                deckManagementState.update { managementState ->
                    managementState.copy(
                        scheduledReviewChangeState = IDateDataChangeState.Required(
                            dateData = (scheduledDate - currentTime)
                                .calculateDetailedScheduledInterval()
                        )
                    )
                }
            }

            IDeckManagementAction.DismissScheduledReviewDialog -> {
                deckManagementState.update { state ->
                    state.copy(scheduledReviewChangeState = IDateDataChangeState.NotRequired)
                }
            }

            IDeckManagementAction.ScheduledReviewChangeConfirmed -> {
                val scheduledReviewChangeState = deckManagementState.value.scheduledReviewChangeState

                if (scheduledReviewChangeState is IDateDataChangeState.Required) {
                    val validatedDateData = DateDataValidator().validateForSaving(
                        dateData = scheduledReviewChangeState.dateData
                    )

                    viewModelScope.launchWithState(coroutineContextProvider.io) {
                        val sourceDeck = deck.value ?: run {
                            emitScheduledReviewUpdateFailure()
                            return@launchWithState
                        }
                        val sourceScheduledDate = sourceDeck.scheduledDate ?: run {
                            emitScheduledReviewUpdateFailure()
                            return@launchWithState
                        }
                        val currentTime = getCurrentDateAsLong()
                        if (sourceScheduledDate < currentTime) {
                            emitScheduledReviewUpdateFailure()
                            return@launchWithState
                        }

                        val scheduledDate = currentTime +
                            validatedDateData.calculateDetailedScheduledIntervalAsLong()
                        val updatedDeck = sourceDeck.copy(
                            scheduledReviewDates = sourceDeck.scheduledReviewDates.replaceLast(
                                value = scheduledDate
                            )
                        )

                        updateDeck(updatedDeck = updatedDeck)
                        deckReviewScheduler.cancel(deckId = sourceDeck.id)
                        deckReviewNotifier.removeNotificationFromNotificationBar(deckId = sourceDeck.id)
                        if (scheduledDate >= currentTime) {
                            deckReviewScheduler.schedule(
                                deckName = sourceDeck.name,
                                deckId = sourceDeck.id,
                                atTime = scheduledDate,
                            )
                        }
                        emitScheduledReviewUpdateSuccess()
                        deckManagementState.update { managementState ->
                            managementState.copy(
                                scheduledReviewChangeState = IDateDataChangeState.NotRequired
                            )
                        }
                    }.onException { _, throwable ->
                        // logE("Exception: ${throwable.stackTraceToString()}")
                        emitScheduledReviewUpdateFailure()
                        deckManagementState.update { managementState ->
                            managementState.copy(
                                scheduledReviewChangeState = IDateDataChangeState.NotRequired
                            )
                        }
                    }
                }
            }

            is IDeckManagementAction.ScheduledReviewChanged -> {
                val scheduledReviewChangeState = deckManagementState.value.scheduledReviewChangeState

                if (scheduledReviewChangeState is IDateDataChangeState.Required) {
                    deckManagementState.update { managementState ->
                        val updatedDateData = ButtonActionHandler().handle(
                            dateData = scheduledReviewChangeState.dateData,
                            dataUnit = action.dateUnit,
                            buttonAction = action.buttonAction,
                        )

                        managementState.copy(
                            scheduledReviewChangeState = IDateDataChangeState.Required(
                                dateData = updatedDateData
                            )
                        )
                    }
                }
            }
        }
    }

    private fun List<Long>.replaceLast(value: Long): List<Long> {
        if (isEmpty()) return this

        return toMutableList().apply {
            this[lastIndex] = value
        }
    }

    private fun emitScheduledReviewUpdateSuccess() {
        eventMessage.tryEmit(
            value = EventMessage(
                value = SCHEDULED_REVIEW_UPDATE_SUCCESS,
                type = EventMessage.Type.Positive,
            )
        )
    }

    private fun emitScheduledReviewUpdateFailure() {
        eventMessage.tryEmit(
            value = EventMessage(
                value = SCHEDULED_REVIEW_UPDATE_FAILURE,
                type = EventMessage.Type.Negative,
                duration = EventMessage.Duration.Long,
            )
        )
    }
}
