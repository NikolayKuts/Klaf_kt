package com.kuts.klaf.presentation.deckManagment

import androidx.lifecycle.viewModelScope
import com.kuts.domain.common.catchWithCrashlyticsReport
import com.kuts.domain.repositories.CrashlyticsRepository
import com.kuts.domain.useCases.FetchDeckByIdUseCase
import com.kuts.klaf.R
import com.kuts.klaf.presentation.common.EventMessage
import com.kuts.klaf.presentation.common.tryEmitAsNegative
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.days

class DeckManagementViewModel @AssistedInject constructor(
    @Assisted private val deckId: Int,
    private val fetchDeckById: FetchDeckByIdUseCase,
    private val crashlytics: CrashlyticsRepository,
) : BaseDeckManagementViewModel() {

    override val eventMessage = MutableSharedFlow<EventMessage>(extraBufferCapacity = 1)

    override val deckState = MutableStateFlow<DeckManagementState>(DeckManagementState())

    private val deck = fetchDeckById(deckId = deckId)
        .catchWithCrashlyticsReport(crashlytics = crashlytics) {
            eventMessage.tryEmitAsNegative(resId = R.string.problem_with_fetching_deck)
        }.onEach { deck ->
            deck?.let {
                deckState.value = DeckManagementState(
                    name = it.name,
                    creationDate = it.creationDate.days.toString(),
                    repetitionIterationDates = it.repetitionIterationDates.map { it.toString()},
                    scheduledIterationDates = it.scheduledIterationDates.map { it.toString() },
                    scheduledDateInterval = it.scheduledDateInterval,
                    repetitionQuantity = it.repetitionQuantity,
                    cardQuantity = it.cardQuantity,
                    lastFirstRepetitionDuration = it.lastFirstRepetitionDuration,
                    lastSecondRepetitionDuration = it.lastSecondRepetitionDuration,
                    lastRepetitionIterationDuration = it.lastRepetitionIterationDuration,
                    isLastIterationSucceeded = it.isLastIterationSucceeded,
                    id = it.id
                )
            }
//            Deck(
//                val name: String,
//            val creationDate: Long,
//            val repetitionIterationDates: List<Long> = emptyList(),
//            val scheduledIterationDates: List<Long> = emptyList(),
//            val scheduledDateInterval: Long = 0,
//            val repetitionQuantity: Int = 0,
//            val cardQuantity: Int = 0,
//            val lastFirstRepetitionDuration: Long = 0,
//            val lastSecondRepetitionDuration: Long = 0,
//            val lastRepetitionIterationDuration: Long = 0,
//            val isLastIterationSucceeded: Boolean = true,
//            val id: Int = 0
        }.shareIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            replay = 1
        )

    override fun sendEvent(event: DeckManagementEvent) {

    }

    fun subscribeToDeckState() {
//        viewModelScope.launch {
//            fetchDeckById(deckId = deckId)
//                .catchWithCrashlyticsReport(crashlytics = crashlytics) {
//                    eventMessage.tryEmitAsNegative(resId = R.string.problem_with_fetching_deck)
//                }.onEach { deck ->
//                    deck?.let {
//                        deckState.value = DeckManagementState(
//                            name = it.name,
//                            creationDate = it.creationDate.days.toString(),
//                            repetitionIterationDates = it.repetitionIterationDates.map { it.toString()},
//                            scheduledIterationDates = it.scheduledIterationDates.map { it.toString() },
//                            scheduledDateInterval = it.scheduledDateInterval,
//                            repetitionQuantity = it.repetitionQuantity,
//                            cardQuantity = it.cardQuantity,
//                            lastFirstRepetitionDuration = it.lastFirstRepetitionDuration,
//                            lastSecondRepetitionDuration = it.lastSecondRepetitionDuration,
//                            lastRepetitionIterationDuration = it.lastRepetitionIterationDuration,
//                            isLastIterationSucceeded = it.isLastIterationSucceeded,
//                            id = it.id
//                        )
//                    }
//                }.shareIn(
//                    scope = viewModelScope,
//                    started = SharingStarted.Eagerly,
//                    replay = 1
//                )
//        }
    }
}