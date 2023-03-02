package com.example.klaf.presentation.deckRepetitionInfo

import androidx.lifecycle.viewModelScope
import com.example.domain.common.Emptiable
import com.example.domain.entities.DeckRepetitionInfo
import com.example.domain.useCases.FetchDeckRepetitionInfoUseCase
import com.example.klaf.R
import com.example.klaf.presentation.common.EventMessage
import com.example.klaf.presentation.common.tryEmit
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.*

class DeckRepetitionInfoViewModel @AssistedInject constructor(
    @Assisted private val deckId: Int,
    fetchDeckRepetitionInfoUseCase: FetchDeckRepetitionInfoUseCase
) : BaseDeckRepetitionInfoViewModel() {

    override val eventMessage = MutableSharedFlow<EventMessage>()
    override val repetitionInfo: StateFlow<Emptiable<DeckRepetitionInfo?>> =
        fetchDeckRepetitionInfoUseCase(deckId = deckId)
            .map { info -> Emptiable.Content(data = info) }
            .catch {
                eventMessage.tryEmit(messageId = R.string.problem_with_fetching_deck_repetition_info)
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = Emptiable.Empty()
            )
}