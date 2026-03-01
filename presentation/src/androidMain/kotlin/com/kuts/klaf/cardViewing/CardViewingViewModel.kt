package com.kuts.klaf.cardViewing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuts.domain.common.catchWithCrashlyticsReport
import com.kuts.domain.entities.Card
import com.kuts.domain.entities.Deck
import com.kuts.domain.repositories.ICrashlyticsRepository
import com.kuts.domain.useCases.FetchCardsUseCase
import com.kuts.domain.useCases.FetchDeckByIdUseCase
import com.kuts.klaf.presentation.R
import com.kuts.klaf.common.EventMessage
import com.kuts.klaf.common.tryEmitAsNegative
import com.lib.lokdroid.core.logE
import kotlinx.coroutines.flow.*

class CardViewingViewModel(
    deckId: Int,
    fetchDeckById: FetchDeckByIdUseCase,
    private val fetchCards: FetchCardsUseCase,
    private val crashlytics: ICrashlyticsRepository,
) : ViewModel() {

    private val _eventMessage = MutableSharedFlow<EventMessage>(extraBufferCapacity = 1)
    val eventMessage = _eventMessage.asSharedFlow()

    val deck: SharedFlow<Deck?> = fetchDeckById(deckId = deckId)
        .onEach { _cards.value = getCardsByDeckId(id = deckId) }
        .catchWithCrashlyticsReport(crashlytics = crashlytics) { throwable ->
            logE("Failed to fetch deck for card viewing\n${throwable.stackTraceToString()}")
            _eventMessage.tryEmitAsNegative(resId = R.string.problem_with_fetching_deck)
        }.shareIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            replay = 1
        )

    private val _cards = MutableStateFlow<List<Card>>(emptyList())
    val cards = _cards.asStateFlow()

    private suspend fun getCardsByDeckId(id: Int): List<Card> = fetchCards(deckId = id)
        .catchWithCrashlyticsReport(crashlytics = crashlytics) { throwable ->
            logE("Failed to fetch cards for card viewing\n${throwable.stackTraceToString()}")
            _eventMessage.tryEmitAsNegative(resId = R.string.problem_with_fetching_cards)
        }.firstOrNull() ?: emptyList()
}
