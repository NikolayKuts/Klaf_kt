package com.example.klaf.presentation.cardAddition

import androidx.lifecycle.viewModelScope
import com.example.klaf.R
import com.example.klaf.domain.common.generateLetterInfos
import com.example.klaf.domain.common.launchWithExceptionHandler
import com.example.klaf.domain.common.updatedAt
import com.example.klaf.domain.entities.Card
import com.example.klaf.domain.entities.Deck
import com.example.klaf.domain.ipa.LetterInfo
import com.example.klaf.domain.ipa.convertToEncodedIpa
import com.example.klaf.domain.ipa.convertToUncompletedIpa
import com.example.klaf.domain.ipa.toWord
import com.example.klaf.domain.useCases.AddNewCardIntoDeckUseCase
import com.example.klaf.domain.useCases.FetchDeckByIdUseCase
import com.example.klaf.presentation.cardAddition.CardAdditionEvent.*
import com.example.klaf.presentation.common.EventMessage
import com.example.klaf.presentation.common.tryEmit
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.*

class CardAdditionViewModel @AssistedInject constructor(
    @Assisted deckId: Int,
    @Assisted smartSelectedWord: String?,
    fetchDeckById: FetchDeckByIdUseCase,
    private val addNewCardIntoDeck: AddNewCardIntoDeckUseCase,
) : BaseCardAdditionViewModel() {

    override val eventMessage = MutableSharedFlow<EventMessage>(extraBufferCapacity = 1)

    override val deck: SharedFlow<Deck?> = fetchDeckById(deckId = deckId)
        .catch { eventMessage.tryEmit(messageId = R.string.problem_with_fetching_deck) }
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            replay = 1
        )

    override val cardAdditionState = MutableStateFlow<CardAdditionState>(
        value = CardAdditionState.Adding(
            letterInfos = smartSelectedWord?.generateLetterInfos() ?: emptyList(),
            nativeWord = "",
            foreignWord = "",
            ipaTemplate = ""
        )
    )

    private val letterInfosState = MutableStateFlow(value = cardAdditionState.value.letterInfos)
    private val nativeWordState = MutableStateFlow(value = cardAdditionState.value.nativeWord)
    private val ipaTemplateState = MutableStateFlow(value = cardAdditionState.value.ipaTemplate)

    init {
        combine(
            letterInfosState,
            nativeWordState,
            ipaTemplateState
        ) { letterInfos, nativeWord, ipaTemplate ->
            CardAdditionState.Adding(
                letterInfos = letterInfos,
                nativeWord = nativeWord,
                foreignWord = letterInfos.toWord(),
                ipaTemplate = ipaTemplate
            )
        }.onEach { addingState -> cardAdditionState.value = addingState }
            .launchIn(viewModelScope)
    }

    override fun sendEvent(event: CardAdditionEvent) {
        when (event) {
            is ChangeLetterSelectionWithIpaTemplate -> {
                changeLetterSelectionWithIpaTemplate(index = event.index, letterInfo = event.letterInfo)
            }
            is UpdateForeignWordWithIpaTemplate -> updateForeignWordWithIpaTemplate(word = event.word)
            is UpdateIpaTemplate -> updateIpa(ipa = event.ipa)
            is UpdateNativeWord -> updateNativeWord(word = event.word)
            is AddNewCard -> {
                addNewCard(
                    deckId = event.deckId,
                    nativeWord = event.nativeWord,
                    foreignWord = event.foreignWord,
                    letterInfos = event.letterInfos,
                    ipaTemplate = event.ipaTemplate
                )
            }
        }
    }

    private fun addNewCard(
        deckId: Int,
        nativeWord: String,
        foreignWord: String,
        letterInfos: List<LetterInfo>,
        ipaTemplate: String,
    ) {
        if (nativeWordState.value.isEmpty() || foreignWord.isEmpty()) {
            eventMessage.tryEmit(
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
                    eventMessage.tryEmit(messageId = R.string.exception_adding_card)
                },
                onCompletion = {
                    cardAdditionState.value = CardAdditionState.Finished
                    eventMessage.tryEmit(messageId = R.string.card_has_been_added)
                },
                task = { addNewCardIntoDeck(card = newCard) }
            )
        }
    }

    private fun changeLetterSelectionWithIpaTemplate(index: Int, letterInfo: LetterInfo) {
        val updatedIsChecked = when (letterInfo.letter) {
            LetterInfo.EMPTY_LETTER -> false
            else -> !letterInfo.isChecked
        }

        letterInfosState.update { infos ->
            infos.updatedAt(
                index = index,
                updateValue = letterInfo.copy(isChecked = updatedIsChecked)
            )
        }

        ipaTemplateState.value = letterInfosState.value.convertToUncompletedIpa()
    }

    private fun updateNativeWord(word: String) {
        nativeWordState.value = word
    }

    private fun updateForeignWordWithIpaTemplate(word: String) {
        letterInfosState.value = word.generateLetterInfos()
        ipaTemplateState.value = letterInfosState.value.convertToUncompletedIpa()
    }

    private fun updateIpa(ipa: String) {
        ipaTemplateState.value = ipa
    }
}