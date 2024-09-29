package com.kuts.klaf.presentation.deckManagment

import androidx.lifecycle.viewModelScope
import com.kuts.domain.common.CoroutineStateHolder.Companion.launchWithState
import com.kuts.domain.common.CoroutineStateHolder.Companion.onException
import com.kuts.domain.common.catchWithCrashlyticsReport
import com.kuts.domain.entities.Deck
import com.kuts.domain.repositories.CrashlyticsRepository
import com.kuts.domain.useCases.FetchDeckByIdUseCase
import com.kuts.domain.useCases.UpdateDeckUseCase
import com.kuts.klaf.R
import com.kuts.klaf.data.common.DateFormat
import com.kuts.klaf.data.common.asFormattedDate
import com.kuts.klaf.data.common.calculateDetailedScheduledInterval
import com.kuts.klaf.data.common.calculateDetailedScheduledIntervalAsLong
import com.kuts.klaf.presentation.common.EventMessage
import com.kuts.klaf.presentation.common.tryEmitAsNegative
import com.lib.lokdroid.core.logD
import com.lib.lokdroid.core.logE
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class DeckManagementViewModel @AssistedInject constructor(
    @Assisted private val deckId: Int,
    private val fetchDeckById: FetchDeckByIdUseCase,
    private val updateDeck: UpdateDeckUseCase,
    private val crashlytics: CrashlyticsRepository,
) : BaseDeckManagementViewModel() {

    override val eventMessage = MutableSharedFlow<EventMessage>(extraBufferCapacity = 1)

    override val event = MutableStateFlow<DeckManagementEvent>(value = DeckManagementEvent.None)

    override val deckManagementState = MutableStateFlow(DeckManagementState())

    private val deck = MutableStateFlow<Deck?>(null)

    init {
        subscribeToDeckUpdates()
    }

    private fun subscribeToDeckUpdates() {
        fetchDeckById(deckId = deckId)
            .catchWithCrashlyticsReport(crashlytics = crashlytics) {
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
                    value = deck.creationDate.asFormattedDate(pattern = DateFormat.FULL)
                ),
                repetitionIterationDates = it.repetitionIterationDates.map { it.toString() },
                scheduledIterationDates = it.scheduledIterationDates.map { it.toString() },
                scheduledDateInterval = it.scheduledDateInterval.copy(value = deck.scheduledDateInterval.calculateDetailedScheduledInterval()),
                repetitionQuantity = it.repetitionQuantity.copy(value = deck.repetitionQuantity.toString()),
                cardQuantity = it.cardQuantity.copy(value = deck.cardQuantity.toString()),
                lastFirstRepetitionDuration = it.lastFirstRepetitionDuration.copy(value = deck.lastFirstRepetitionDuration.toString()),
                lastSecondRepetitionDuration = it.lastSecondRepetitionDuration.copy(value = deck.lastSecondRepetitionDuration.toString()),
                lastRepetitionIterationDuration = it.lastRepetitionIterationDuration.copy(value = deck.lastRepetitionIterationDuration.toString()),
                isLastIterationSucceeded = it.isLastIterationSucceeded.copy(value = deck.isLastIterationSucceeded.toString()),
                id = it.id.copy(value = deck.id.toString()),
            )
        }
    }

    override fun sendAction(action: DeckManagementAction) {
        logD("sendAction() called. Action: $action")

        when (action) {
            is DeckManagementAction.ScheduledDateIntervalChangeRequested -> {
                deckManagementState.update { managementState ->
                    managementState.copy(
                        scheduledDateIntervalChangeState = ScheduledDataIntervalChangeState.Required(
                            dateData = managementState.scheduledDateInterval.value
                        )
                    )
                }
            }

            is DeckManagementAction.DismissScheduledDateIntervalDialog -> {
                deckManagementState.update { state ->
                    state.copy(scheduledDateIntervalChangeState = ScheduledDataIntervalChangeState.NotRequired)
                }
            }

            DeckManagementAction.ScheduledDateIntervalChangeConfirmed -> {
                val scheduledDateIntervalChangeState = deckManagementState.value.scheduledDateIntervalChangeState

                if (scheduledDateIntervalChangeState is ScheduledDataIntervalChangeState.Required) {
                    val dateDataToSave = scheduledDateIntervalChangeState.dateData
                    val validatedDateData = DateDataValidator().validateForSaving(dateData = dateDataToSave)

                    viewModelScope.launchWithState() {
                        val interval = validatedDateData.calculateDetailedScheduledIntervalAsLong()
                        updateDeck(updatedDeck = deck.value!!.copy(scheduledDateInterval = interval))
                        deckManagementState.update { managementState ->
                            managementState.copy(
                                scheduledDateIntervalChangeState = ScheduledDataIntervalChangeState.NotRequired
                            )
                        }
                    }.onException() { a, throwable ->
                        logE("Exception: ${throwable.stackTraceToString()}")
                    }

                }


            }

            is DeckManagementAction.ScheduledDateIntervalChanged -> {
                deckManagementState.update { managementState ->
                    val dateDataValidator = DateDataValidator()
                    val sourceDateDate = managementState.scheduledDateInterval.value
                    val clearedValueAsString =
                        dateDataValidator.clearedValueAsString(value = action.value)

                    val validDateData = if (clearedValueAsString.isEmpty()) {
                        val updatedDateDate = sourceDateDate.copyWithEmptyUnit(
                            unit = action.dateUnit,
                        )

                        logD("EMPTY")
                        logD("updatedDateDate: $updatedDateDate")

                        DateDataValidator().validate(
//                            sourceDate = sourceDateDate,
                            dateData = updatedDateDate
                        )
                    } else {
                        val valueAsInt = dateDataValidator.clearValue(value = action.value)
                        val updatedDateDate = sourceDateDate.copyByUnit(
                            unit = action.dateUnit,
                            value = valueAsInt
                        )

                        logD("NOT EMPTY")
                        logD("valueAsInt: $valueAsInt")
                        logD("updatedDateDate: $updatedDateDate")

                        DateDataValidator().validate(
//                            sourceDate = sourceDateDate,
                            dateData = updatedDateDate
                        )
                    }


//                    logD("valueAsLong: $valueAsInt")
//                    logD("updatedDateDate: $updatedDateDate")
//                    logD("validDateData: $validDateData")

                    managementState.copy(
                        scheduledDateIntervalChangeState = ScheduledDataIntervalChangeState.Required(
                            dateData = validDateData
                        )
                    )
                }
            }
        }
    }

}