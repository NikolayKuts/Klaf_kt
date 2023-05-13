package com.example.klaf.presentation.deckRepetitionInfo

import androidx.lifecycle.viewModelScope
import com.example.domain.common.Emptiable
import com.example.domain.common.catchWithCrashlyticsReport
import com.example.domain.entities.DeckRepetitionInfo
import com.example.domain.repositories.CrashlyticsRepository
import com.example.domain.useCases.FetchDeckRepetitionInfoUseCase
import com.example.klaf.R
import com.example.klaf.presentation.common.EventMessage
import com.example.klaf.presentation.common.tryEmitAsNegative
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