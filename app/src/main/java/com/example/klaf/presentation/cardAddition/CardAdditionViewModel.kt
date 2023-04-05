package com.example.klaf.presentation.cardAddition

import androidx.lifecycle.viewModelScope
import com.example.domain.common.CoroutineStateHolder.Companion.launchWithState
import com.example.domain.common.CoroutineStateHolder.Companion.onException
import com.example.domain.common.generateLetterInfos
import com.example.domain.common.updatedAt
import com.example.domain.entities.Card
import com.example.domain.entities.Deck
import com.example.domain.ipa.IpaHolder
import com.example.domain.ipa.LetterInfo
import com.example.domain.ipa.toRowIpaItemHolders
import com.example.domain.ipa.toWord
import com.example.domain.useCases.AddNewCardIntoDeckUseCase
import com.example.domain.useCases.FetchDeckByIdUseCase
import com.example.domain.useCases.FetchWordAutocompleteUseCase
import com.example.klaf.R
import com.example.klaf.data.networking.CardAudioPlayer
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

    companion object {

        const val MAX_IPA_LENGTH = 10
        const val MAX_WORD_LENGTH = 30
    }

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

    override val autocompleteState = MutableStateFlow(value = AutocompleteState())

    private val letterInfosState = MutableStateFlow(value = cardAdditionState.value.letterInfos)
    private val nativeWordState = MutableStateFlow(value = cardAdditionState.value.nativeWord)
    private val ipaHoldersState = MutableStateFlow(value = cardAdditionState.value.ipaHolders)

    private var autocompleteFetchingJob: Job? = null

    init {
        combine(
            letterInfosState,
            nativeWordState,
            ipaHoldersState
        ) { letterInfos, nativeWord, ipaHolders ->
            CardAdditionState.Adding(
                letterInfos = letterInfos,
                nativeWord = nativeWord,
                foreignWord = letterInfos.toWord(),
                ipaHolders = ipaHolders
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
            is UpdateDataOnForeignWordChanged -> updateDataOnForeignWordChanged(word = event.word)
            is UpdateDataOnAutocompleteSelected -> updateDataOnAutocompleteSelected(word = event.word)
            is UpdateIpa -> {
                updateIpa(letterGroupIndex = event.letterGroupIndex, ipa = event.ipa)
            }
            is UpdateNativeWord -> updateNativeWord(word = event.word)
            is AddNewCard -> {
                addNewCard(
                    deckId = event.deckId,
                    nativeWord = event.nativeWord,
                    foreignWord = event.foreignWord,
                    ipaHolders = event.ipaHolders
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
        ipaHolders: List<IpaHolder>,
    ) {
        if (nativeWordState.value.isEmpty() || foreignWord.isEmpty()) {
            eventMessage.tryEmit(messageId = R.string.native_and_foreign_words_must_be_filled)
        } else {
            val newCard = Card(
                deckId = deckId,
                nativeWord = nativeWord,
                foreignWord = foreignWord,
                ipa = ipaHolders
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
        val updatedCheckState = when (letterInfo.letter) {
            LetterInfo.EMPTY_LETTER -> false
            else -> letterInfo.isNotChecked
        }

        letterInfosState.update { infos ->
            infos.updatedAt(
                index = index,
                newValue = letterInfo.copy(isChecked = updatedCheckState)
            )
        }

        ipaHoldersState.value = letterInfosState.value.toRowIpaItemHolders()
    }

    private fun updateNativeWord(word: String) {
        if (word.length < MAX_WORD_LENGTH) {
            nativeWordState.value = word
        }
    }

    private fun updateDataOnForeignWordChanged(word: String) {
        if (word.length < MAX_WORD_LENGTH) {
            letterInfosState.value = word.generateLetterInfos()
            ipaHoldersState.value = emptyList()

            autocompleteFetchingJob?.cancel()
            autocompleteFetchingJob = viewModelScope.launch(Dispatchers.IO) {
                autocompleteState.value = AutocompleteState(
                    prefix = word,
                    autocomplete = fetchWordAutocomplete(prefix = word),
                    isActive = true,
                )
            }
        }
    }

    private fun updateDataOnAutocompleteSelected(word: String) {
        letterInfosState.value = word.generateLetterInfos()
        ipaHoldersState.value = emptyList()
        autocompleteState.value = AutocompleteState()
    }

    private fun updateIpa(letterGroupIndex: Int, ipa: String) {
        if (ipa.length < MAX_IPA_LENGTH) {
            ipaHoldersState.update { ipaHolders ->
                ipaHolders.updatedAt(index = letterGroupIndex) { oldValue ->
                    oldValue.copy(ipa = ipa.trim())
                }
            }
        }
    }

    private fun resetAddingState() {
        letterInfosState.value = emptyList()
        nativeWordState.value = ""
        ipaHoldersState.value = emptyList()
    }
}