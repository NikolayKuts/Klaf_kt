package com.kuts.klaf.deckManagment

import androidx.lifecycle.viewModelScope
import com.kuts.domain.common.CoroutineStateHolder.Companion.launchWithState
import com.kuts.domain.common.CoroutineStateHolder.Companion.onException
import com.kuts.domain.common.calculateDetailedScheduledInterval
import com.kuts.domain.common.calculateDetailedScheduledIntervalAsLong
import com.kuts.domain.common.catchWithCrashlyticsReport
import com.kuts.domain.entities.Deck
import com.kuts.domain.repositories.ICrashlyticsRepository
import com.kuts.domain.useCases.FetchDeckByIdUseCase
import com.kuts.domain.useCases.UpdateDeckUseCase
import com.kuts.klaf.presentation.R
import com.kuts.klaf.common.EventMessage
import com.kuts.klaf.common.DateFormatPattern
import com.kuts.klaf.common.asFormattedDate
import com.kuts.klaf.common.tryEmitAsNegative
import com.lib.lokdroid.core.logD
import com.lib.lokdroid.core.logE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class DeckManagementViewModel(
    private val deckId: Int,
    private val fetchDeckById: FetchDeckByIdUseCase,
    private val updateDeck: UpdateDeckUseCase,
    private val crashlytics: ICrashlyticsRepository,
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
                logE("Failed to fetch deck for management\n${throwable.stackTraceToString()}")
                eventMessage.tryEmitAsNegative(resId = R.string.problem_with_fetching_deck)
            }.onEach { receivedDeck ->
                logE("receivedDeck: $receivedDeck")

                deck.value = receivedDeck
                receivedDeck?.let { deck -> updateDeckState(deck = deck) }
            }.flowOn(Dispatchers.IO)
            .launchIn(viewModelScope)
    }

    private fun updateDeckState(deck: Deck) {
        deckManagementState.update {
            it.copy(
                name = it.name.copy(value = deck.name),
                creationDate = it.creationDate.copy(
                    value = deck.creationDate.asFormattedDate(pattern = DateFormatPattern.FULL)
                ),
                repetitionIterationDates = it.repetitionIterationDates,
                scheduledIterationDates = it.scheduledIterationDates,
                scheduledDateInterval = it.scheduledDateInterval.copy(value = deck.scheduledDateInterval.calculateDetailedScheduledInterval()),
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
        logD("sendAction() called. Action: $action")

        when (action) {
            is IDeckManagementAction.ScheduledDateIntervalChangeRequested -> {
                deckManagementState.update { managementState ->
                    managementState.copy(
                        scheduledDateIntervalChangeState = IScheduledDataIntervalChangeState.Required(
                            dateData = managementState.scheduledDateInterval.value
                        )
                    )
                }
            }

            is IDeckManagementAction.DismissScheduledDateIntervalDialog -> {
                deckManagementState.update { state ->
                    state.copy(scheduledDateIntervalChangeState = IScheduledDataIntervalChangeState.NotRequired)
                }
            }

            IDeckManagementAction.ScheduledDateIntervalChangeConfirmed -> {
                val scheduledDateIntervalChangeState =
                    deckManagementState.value.scheduledDateIntervalChangeState

                if (scheduledDateIntervalChangeState is IScheduledDataIntervalChangeState.Required) {
                    val validatedDateData = DateDataValidator().validateForSaving(
                        dateData = scheduledDateIntervalChangeState.dateData
                    )

                    if (validatedDateData == deckManagementState.value.scheduledDateInterval.value) {
                        // TODO: send event message
//                        eventMessage.tryEmitAsNeutral()
//                        return
                    }

                    viewModelScope.launchWithState(Dispatchers.IO) {
                        val interval = validatedDateData.calculateDetailedScheduledIntervalAsLong()

                        updateDeck(updatedDeck = deck.value!!.copy(scheduledDateInterval = interval))
                        deckManagementState.update { managementState ->
                            managementState.copy(
                                scheduledDateIntervalChangeState = IScheduledDataIntervalChangeState.NotRequired
                            )
                        }
                    }.onException { _, throwable ->
                        logE("Exception: ${throwable.stackTraceToString()}")
                    }

                }
            }

            is IDeckManagementAction.ScheduledDateIntervalChanged -> {
                val scheduledDateIntervalChangeState =
                    deckManagementState.value.scheduledDateIntervalChangeState

                if (scheduledDateIntervalChangeState is IScheduledDataIntervalChangeState.Required) {
                    deckManagementState.update { managementState ->
                        val sourceDateDate = scheduledDateIntervalChangeState.dateData

                        val updatedDateData = ButtonActionHandler().handle(
                            dateData = sourceDateDate,
                            dataUnit = action.dateUnit,
                            buttonAction = action.buttonAction
                        )
                        val updatedScheduledDateIntervalChangeState =
                            IScheduledDataIntervalChangeState.Required(dateData = updatedDateData)

                        managementState.copy(
                            scheduledDateIntervalChangeState = updatedScheduledDateIntervalChangeState
                        )
                    }
                }
            }
        }
    }
}
