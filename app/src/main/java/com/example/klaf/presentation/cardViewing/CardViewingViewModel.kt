package com.example.klaf.presentation.cardViewing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.klaf.R
import com.example.klaf.domain.entities.Card
import com.example.klaf.domain.entities.Deck
import com.example.klaf.domain.useCases.FetchCardsUseCase
import com.example.klaf.domain.useCases.FetchDeckByIdUseCase
import com.example.klaf.presentation.common.EventMessage
import com.example.klaf.presentation.common.tryEmit
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.*

class CardViewingViewModel @AssistedInject constructor(
    @Assisted deckId: Int,
    fetchDeckById: FetchDeckByIdUseCase,
    private val fetchCards: FetchCardsUseCase,
) : ViewModel() {

    private val _eventMessage = MutableSharedFlow<EventMessage>(extraBufferCapacity = 1)
    val eventMessage = _eventMessage.asSharedFlow()

    val deck: SharedFlow<Deck?> = fetchDeckById(deckId = deckId)
        .onEach { _cards.value = getCardsByDeckId(id = deckId) }
        .catch { _eventMessage.tryEmit(messageId = R.string.problem_with_fetching_deck) }
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            replay = 1
        )

    private val _cards = MutableStateFlow<List<Card>>(emptyList())
    val cards = _cards.asStateFlow()

    private suspend fun getCardsByDeckId(id: Int): List<Card> {
        return fetchCards(deckId = id)
            .catch { _eventMessage.tryEmit(messageId = R.string.problem_with_fetching_cards) }
            .firstOrNull() ?: emptyList()
    }
}