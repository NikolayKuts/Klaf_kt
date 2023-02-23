package com.example.klaf.presentation.cardAddition

import androidx.lifecycle.viewModelScope
import com.example.domain.common.CoroutineStateHolder.Companion.launchWithState
import com.example.domain.common.CoroutineStateHolder.Companion.onException
import com.example.domain.common.generateLetterInfos
import com.example.domain.common.updatedAt
import com.example.domain.entities.Card
import com.example.domain.entities.Deck
import com.example.domain.ipa.LetterInfo
import com.example.domain.ipa.convertToEncodedIpa
import com.example.domain.ipa.convertToUncompletedIpa
import com.example.domain.ipa.toWord
import com.example.domain.useCases.AddNewCardIntoDeckUseCase
import com.example.domain.useCases.FetchDeckByIdUseCase
import com.example.domain.useCases.FetchWordAutocompleteUseCase
import com.example.klaf.R
import com.example.data.networking.CardAudioPlayer
import com.example.klaf.presentation.cardAddition.CardAdditionEvent.*
import com.example.klaf.presentation.common.EventMessage
import com.example.klaf.presentation.common.tryEmit
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CardAdditionViewModel @AssistedInject constructor(
    @Assisted deckId: Int,
    @Assisted smartSelectedWord: String?,
    fetchDeckById: FetchDeckByIdUseCase,
    private val addNewCardIntoDeck: AddNewCardIntoDeckUseCase,
    override val audioPlayer: CardAudioPlayer,
    private val fetchWordAutocomplete: FetchWordAutocompleteUseCase,
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
        )
    )

    override val autocompleteState: MutableStateFlow<AutocompleteState> =
        MutableStateFlow(value = AutocompleteState())

    private val letterInfosState = MutableStateFlow(value = cardAdditionState.value.letterInfos)
    private val nativeWordState = MutableStateFlow(value = cardAdditionState.value.nativeWord)
    private val ipaTemplateState = MutableStateFlow(value = cardAdditionState.value.ipaTemplate)

    private var autocompleteFetchingJob: Job? = null

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
        }.onEach { addingState ->
            cardAdditionState.value = addingState
            audioPlayer.preparePronunciation(word = addingState.foreignWord)
        }.launchIn(viewModelScope)
    }

    override fun sendEvent(event: CardAdditionEvent) {
        when (event) {
            is ChangeLetterSelectionWithIpaTemplate -> {
                changeLetterSelectionWithIpaTemplate(
                    index = event.index,
                    letterInfo = event.letterInfo
                )
            }
            is UpdateDataOnForeignWordChaneged -> updateDataOnForeignWordChanged(word = event.word)
            is UpdateDataOnAutocompleteSelected -> updateDataOnAutocompleteSelected(word = event.word)
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
            PronounceForeignWord -> audioPlayer.play()
            CloseAutocompleteMenu -> {
                autocompleteState.update { it.copy(isActive = false) }
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
            eventMessage.tryEmit(messageId = R.string.native_and_foreign_words_must_be_filled)
        } else {
            val newCard = Card(
                deckId = deckId,
                nativeWord = nativeWord,
                foreignWord = foreignWord,
                ipa = letterInfos.convertToEncodedIpa(ipaTemplate = ipaTemplate)
            )

            viewModelScope.launchWithState {
                addNewCardIntoDeck(card = newCard)
                resetAddingState()
                cardAdditionState.value = CardAdditionState.Finished
                eventMessage.tryEmit(messageId = R.string.card_has_been_added)
            } onException { _, _ ->
                eventMessage.tryEmit(messageId = R.string.exception_adding_card)
            }
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
                newValue = letterInfo.copy(isChecked = updatedIsChecked)
            )
        }

        ipaTemplateState.value = letterInfosState.value.convertToUncompletedIpa()
    }

    private fun updateNativeWord(word: String) {
        nativeWordState.value = word
    }

    private fun updateDataOnForeignWordChanged(word: String) {
        val clearedWord = word.trim()
        letterInfosState.value = clearedWord.generateLetterInfos()
        ipaTemplateState.value = letterInfosState.value.convertToUncompletedIpa()

        autocompleteFetchingJob?.cancel()
        autocompleteFetchingJob = viewModelScope.launch(Dispatchers.IO) {
            autocompleteState.value = AutocompleteState(
                prefix = clearedWord,
                autocomplete = fetchWordAutocomplete(prefix = clearedWord),
                isActive = true,
            )
        }
    }

    private fun updateDataOnAutocompleteSelected(word: String) {
        letterInfosState.value = word.generateLetterInfos()
        ipaTemplateState.value = letterInfosState.value.convertToUncompletedIpa()
        autocompleteState.value = AutocompleteState()
    }

    private fun updateIpa(ipa: String) {
        ipaTemplateState.value = ipa
    }


    private fun resetAddingState() {
        letterInfosState.value = emptyList()
        nativeWordState.value = ""
        ipaTemplateState.value = ""
    }
}