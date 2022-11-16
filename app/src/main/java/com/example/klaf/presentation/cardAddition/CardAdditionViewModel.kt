package com.example.klaf.presentation.cardAddition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.klaf.R
import com.example.klaf.domain.common.launchWithExceptionHandler
import com.example.klaf.domain.entities.Card
import com.example.klaf.domain.entities.Deck
import com.example.klaf.domain.ipa.LetterInfo
import com.example.klaf.domain.ipa.convertToEncodedIpa
import com.example.klaf.domain.useCases.AddNewCardIntoDeckUseCase
import com.example.klaf.domain.useCases.FetchDeckByIdUseCase
import com.example.klaf.presentation.common.EventMessage
import com.example.klaf.presentation.common.tryEmit
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.*

class CardAdditionViewModel @AssistedInject constructor(
    @Assisted deckId: Int,
    fetchDeckById: FetchDeckByIdUseCase,
    private val addNewCardIntoDeck: AddNewCardIntoDeckUseCase,
) : ViewModel() {

    private val _eventMessage = MutableSharedFlow<EventMessage>(extraBufferCapacity = 1)
    val eventMessage = _eventMessage.asSharedFlow()

    val deck: SharedFlow<Deck?> = fetchDeckById(deckId = deckId)
        .catch { _eventMessage.tryEmit(messageId = R.string.problem_with_fetching_deck) }
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            replay = 1
        )

    private val _cardAdditionState = MutableStateFlow(value = CardAdditionState.NOT_ADDED)
    val cardAdditionState = _cardAdditionState.asStateFlow()

    fun addNewCard(
        deckId: Int,
        nativeWord: String,
        foreignWord: String,
        letterInfos: List<LetterInfo>,
        ipaTemplate: String,
    ) {
        if (nativeWord.isEmpty() || foreignWord.isEmpty()) {
            _eventMessage.tryEmit(
                messageId = R.string.native_and_foreign_words_must_be_filled
            )
        } else {
            val newCard = Card(
                deckId = deckId,
                nativeWord = nativeWord,
                foreignWord = foreignWord,
                ipa = letterInfos.convertToEncodedIpa(ipaTemplate = ipaTemplate)
            )

            viewModelScope.launchWithExceptionHandler(
                onException = { _, _ ->
                    _eventMessage.tryEmit(messageId = R.string.exception_adding_card)
                },
                onCompletion = {
                    _eventMessage.tryEmit(messageId = R.string.card_has_been_added)
                }
            ) {
                addNewCardIntoDeck(card = newCard)
            }

            _cardAdditionState.value = CardAdditionState.ADDED
        }
    }

    fun resetCardAdditionState() {
        _cardAdditionState.value = CardAdditionState.NOT_ADDED
    }
}