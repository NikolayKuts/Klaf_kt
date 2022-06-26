package com.example.klaf.presentation.cardEdinting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.klaf.R
import com.example.klaf.domain.common.launchWithExceptionHandler
import com.example.klaf.domain.entities.Card
import com.example.klaf.domain.entities.Deck
import com.example.klaf.domain.useCases.FetchCardUseCase
import com.example.klaf.domain.useCases.FetchDeckByIdUseCase
import com.example.klaf.domain.useCases.UpdateCardUseCase
import com.example.klaf.presentation.common.EventMessage
import com.example.klaf.presentation.common.tryEmit
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.*

class CardEditingViewModel @AssistedInject constructor(
    @Assisted(DECK_ARGUMENT_NAME) private val deckId: Int,
    @Assisted(CARD_ARGUMENT_NAME) cardId: Int,
    fetchDeckById: FetchDeckByIdUseCase,
    fetchCard: FetchCardUseCase,
    private val updateCard: UpdateCardUseCase
) : ViewModel() {

    companion object {

        const val DECK_ARGUMENT_NAME = "deck_id"
        const val CARD_ARGUMENT_NAME = "card_id"
    }

    private val _eventMessage = MutableSharedFlow<EventMessage>(extraBufferCapacity = 1)
    val eventMessage = _eventMessage.asSharedFlow()

    val deck: SharedFlow<Deck?> = fetchDeckById(deckId = deckId)
        .catch { _eventMessage.tryEmit(messageId = R.string.problem_with_fetching_deck) }
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            replay = 1
        )

    val card: SharedFlow<Card?> = fetchCard(cardId = cardId)
        .catch { _eventMessage.tryEmit(messageId = R.string.problem_with_fetching_card) }
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            replay = 1
        )

    fun updateCard(
        deckId: Int,
        nativeWord: String,
        foreignWord: String,
        ipa: String,
        id: Int,
        onFinish: () -> Unit
    ) {
        val changedCard = Card(
            deckId = deckId,
            nativeWord = nativeWord,
            foreignWord = foreignWord,
            ipa = ipa,
            id = id
        )

        val oldCard = card.replayCache.firstOrNull()

        when {
            oldCard == null -> {
                _eventMessage.tryEmit(messageId = R.string.problem_with_fetching_card)
            }
            changedCard.nativeWord.isEmpty() || changedCard.foreignWord.isEmpty() -> {
                _eventMessage.tryEmit(messageId = R.string.native_and_foreign_words_must_be_filled)
            }
            changedCard == oldCard -> {
                _eventMessage.tryEmit(messageId = R.string.card_has_not_been_changed)
            }
            else -> {
                viewModelScope.launchWithExceptionHandler(
                    onException = { _, _ ->
                        _eventMessage.tryEmit(messageId = R.string.problem_with_updating_card)
                    },
                    onCompletion = { onFinish() }
                ) {
                    updateCard(newCard = changedCard)
                }
            }
        }
    }
}