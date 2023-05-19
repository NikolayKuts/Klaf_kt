package com.kuts.klaf.presentation.cardViewing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuts.domain.common.catchWithCrashlyticsReport
import com.kuts.domain.entities.Card
import com.kuts.domain.entities.Deck
import com.kuts.domain.repositories.CrashlyticsRepository
import com.kuts.domain.useCases.FetchCardsUseCase
import com.kuts.domain.useCases.FetchDeckByIdUseCase
import com.kuts.klaf.R
import com.kuts.klaf.presentation.common.EventMessage
import com.kuts.klaf.presentation.common.tryEmitAsNegative
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.*

class CardViewingViewModel @AssistedInject constructor(
    @Assisted deckId: Int,
    fetchDeckById: FetchDeckByIdUseCase,
    private val fetchCards: FetchCardsUseCase,
    private val crashlytics: CrashlyticsRepository,
) : ViewModel() {

    private val _eventMessage = MutableSharedFlow<EventMessage>(extraBufferCapacity = 1)
    val eventMessage = _eventMessage.asSharedFlow()

    val deck: SharedFlow<Deck?> = fetchDeckById(deckId = deckId)
        .onEach { _cards.value = getCardsByDeckId(id = deckId) }
        .catchWithCrashlyticsReport(crashlytics = crashlytics) {
            _eventMessage.tryEmitAsNegative(resId = R.string.problem_with_fetching_deck)
        }.shareIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            replay = 1
        )

    private val _cards = MutableStateFlow<List<Card>>(emptyList())
    val cards = _cards.asStateFlow()

    private suspend fun getCardsByDeckId(id: Int): List<Card> = fetchCards(deckId = id)
        .catchWithCrashlyticsReport(crashlytics = crashlytics) {
            _eventMessage.tryEmitAsNegative(resId = R.string.problem_with_fetching_cards)
        }.firstOrNull() ?: emptyList()
}