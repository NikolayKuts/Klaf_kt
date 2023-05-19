package com.kuts.klaf.presentation.deckRepetitionInfo

import androidx.lifecycle.viewModelScope
import com.kuts.domain.common.Emptiable
import com.kuts.domain.common.catchWithCrashlyticsReport
import com.kuts.domain.entities.DeckRepetitionInfo
import com.kuts.domain.repositories.CrashlyticsRepository
import com.kuts.domain.useCases.FetchDeckRepetitionInfoUseCase
import com.kuts.klaf.R
import com.kuts.klaf.presentation.common.EventMessage
import com.kuts.klaf.presentation.common.tryEmitAsNegative
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.*

class DeckRepetitionInfoViewModel @AssistedInject constructor(
    @Assisted private val deckId: Int,
    fetchDeckRepetitionInfo: FetchDeckRepetitionInfoUseCase,
    crashlytics: CrashlyticsRepository,
) : BaseDeckRepetitionInfoViewModel() {

    override val eventMessage = MutableSharedFlow<EventMessage>(replay = 1)

    override val repetitionInfo: StateFlow<Emptiable<DeckRepetitionInfo?>> =
        fetchDeckRepetitionInfo(deckId = deckId)
            .catchWithCrashlyticsReport(crashlytics = crashlytics) {
                eventMessage.tryEmitAsNegative(resId = R.string.problem_with_fetching_deck_repetition_info)
            }.map { info -> Emptiable.Content(data = info) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = Emptiable.Empty()
            )
}