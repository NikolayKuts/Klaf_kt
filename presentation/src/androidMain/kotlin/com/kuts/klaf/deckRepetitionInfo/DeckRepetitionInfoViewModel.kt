package com.kuts.klaf.deckRepetitionInfo

import androidx.lifecycle.viewModelScope
import com.kuts.domain.common.IEmptiable
import com.kuts.domain.common.catchWithCrashlyticsReport
import com.kuts.domain.entities.DeckRepetitionInfo
import com.kuts.domain.repositories.ICrashlyticsRepository
import com.kuts.domain.useCases.FetchDeckRepetitionInfoUseCase
import com.kuts.klaf.presentation.R
import com.kuts.klaf.common.EventMessage
import com.kuts.klaf.common.tryEmitAsNegative
import com.lib.lokdroid.core.logE
import kotlinx.coroutines.flow.*

class DeckRepetitionInfoViewModel(
    private val deckId: Int,
    fetchDeckRepetitionInfo: FetchDeckRepetitionInfoUseCase,
    crashlytics: ICrashlyticsRepository,
) : BaseDeckRepetitionInfoViewModel() {

    override val eventMessage = MutableSharedFlow<EventMessage>(replay = 1)

    override val repetitionInfo: StateFlow<IEmptiable<DeckRepetitionInfo?>> =
        fetchDeckRepetitionInfo(deckId = deckId)
            .catchWithCrashlyticsReport(crashlytics = crashlytics) { throwable ->
                logE("Failed to fetch deck repetition info\n${throwable.stackTraceToString()}")
                eventMessage.tryEmitAsNegative(resId = R.string.problem_with_fetching_deck_repetition_info)
            }.map { info -> IEmptiable.Content(data = info) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = IEmptiable.Empty
            )
}
