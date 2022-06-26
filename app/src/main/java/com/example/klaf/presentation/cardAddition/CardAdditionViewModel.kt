package com.example.klaf.presentation.cardAddition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.klaf.R
import com.example.klaf.domain.common.launchWithExceptionHandler
import com.example.klaf.domain.entities.Card
import com.example.klaf.domain.entities.Deck
import com.example.klaf.domain.ipa.IpaProcessor
import com.example.klaf.domain.ipa.LetterInfo
import com.example.klaf.domain.useCases.AddNewCardIntoDeckUseCase
import com.example.klaf.domain.useCases.FetchDeckByIdUseCase
import com.example.klaf.presentation.common.EventMessage
import com.example.klaf.presentation.common.tryEmit
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CardAdditionViewModel @AssistedInject constructor(
    @Assisted deckId: Int,
    fetchDeckById: FetchDeckByIdUseCase,
    private val addNewCardIntoDeck: AddNewCardIntoDeckUseCase
) : ViewModel() {

    private val _eventMessage = MutableSharedFlow<EventMessage>(extraBufferCapacity = 1)
    val eventMessage = _eventMessage.asSharedFlow()

    val deck: SharedFlow<Deck?> = fetchDeckById(deckId = deckId).shareIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        replay = 0
    )

    fun addNewCard(
        deckId: Int,
        nativeWord: String,
        foreignWord: String,
        letterInfos: List<LetterInfo>,
        ipaTemplate: String
    ) {
        val newCard = Card(
            deckId = deckId,
            nativeWord = nativeWord,
            foreignWord = foreignWord,
            ipa = IpaProcessor.getEncodedIpa(
                letterInfos = letterInfos,
                ipaTemplate = ipaTemplate
            )
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
    }
}