package com.example.klaf.presentation.cardRemoving

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.klaf.R
import com.example.klaf.domain.common.launchWithExceptionHandler
import com.example.klaf.domain.useCases.RemoveCardFromDeckUseCase
import com.example.klaf.presentation.common.EventMessage
import com.example.klaf.presentation.common.tryEmit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class CardRemovingDialogViewModel @Inject constructor(
    private val removeCard: RemoveCardFromDeckUseCase,
) : ViewModel() {

    private val _isCardRemoved = MutableStateFlow(value = false)
    val isCardRemoved = _isCardRemoved.asStateFlow()

    private val _eventMessage = MutableSharedFlow<EventMessage>(extraBufferCapacity = 1)
    val eventMessage = _eventMessage.asSharedFlow()

    fun removeCardFromDeck(cardId: Int, deckId: Int) {
        viewModelScope.launchWithExceptionHandler(
            onException = { _, _ ->
                _eventMessage.tryEmit(messageId = R.string.problem_with_removing_card)
            },
            onCompletion = {
                _eventMessage.tryEmit(messageId = R.string.card_has_been_deleted)
                _isCardRemoved.value = true
            }
        ) {
            removeCard(cardId = cardId, deckId = deckId)
        }
    }
}