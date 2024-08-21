package com.kuts.klaf.presentation.cardManagement.cardAddition

import androidx.lifecycle.viewModelScope
import com.kuts.domain.common.CoroutineStateHolder.Companion.launchWithState
import com.kuts.domain.common.CoroutineStateHolder.Companion.onException
import com.kuts.domain.common.CoroutineStateHolder.Companion.onExceptionWithCrashlyticsReport
import com.kuts.domain.common.LoadingState
import com.kuts.domain.common.catchWithCrashlyticsReport
import com.kuts.domain.common.generateLetterInfos
import com.kuts.domain.common.updatedAt
import com.kuts.domain.entities.Card
import com.kuts.domain.entities.Deck
import com.kuts.domain.ipa.IpaHolder
import com.kuts.domain.ipa.LetterInfo
import com.kuts.domain.ipa.toRowIpaItemHolders
import com.kuts.domain.repositories.CrashlyticsRepository
import com.kuts.domain.useCases.AddNewCardIntoDeckUseCase
import com.kuts.domain.useCases.FetchDeckByIdUseCase
import com.kuts.domain.useCases.FetchNativeWordSuggestionsUseCase
import com.kuts.domain.useCases.FetchWordAutocompleteUseCase
import com.kuts.klaf.R
import com.kuts.klaf.data.networking.CardAudioPlayer
import com.kuts.klaf.presentation.cardManagement.cardAddition.CardAdditionEvent.*
import com.kuts.klaf.presentation.cardManagement.common.MAX_IPA_LENGTH
import com.kuts.klaf.presentation.cardManagement.common.MAX_FOREIGN_WORD_LENGTH
import com.kuts.klaf.presentation.cardManagement.common.MAX_NATIVE_WORD_LENGTH
import com.kuts.klaf.presentation.common.EventMessage
import com.kuts.klaf.presentation.common.tryEmitAsNegative
import com.kuts.klaf.presentation.common.tryEmitAsPositive
import com.lib.lokdroid.core.logD
import com.lib.lokdroid.core.logE
import com.lib.lokdroid.core.logW
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
    private val fetchNativeWordSuggestions: FetchNativeWordSuggestionsUseCase,
    private val crashlytics: CrashlyticsRepository,
) : BaseCardAdditionViewModel() {

    override val eventMessage = MutableSharedFlow<EventMessage>(extraBufferCapacity = 1)

    override val deck: SharedFlow<Deck?> = fetchDeckById(deckId = deckId)
        .catchWithCrashlyticsReport(crashlytics = crashlytics) {
            eventMessage.tryEmitAsNegative(resId = R.string.problem_with_fetching_deck)
        }.shareIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            replay = 1
        )

    override val cardAdditionState = MutableStateFlow<CardAdditionState>(
        value = CardAdditionState.Adding(
            letterInfos = smartSelectedWord?.generateLetterInfos() ?: emptyList(),
            foreignWord = smartSelectedWord ?: ""
        )
    )

    override val autocompleteState = MutableStateFlow(value = AutocompleteState())

    override val pronunciationLoadingState = audioPlayer.loadingState

    override val nativeWordSuggestionsState = MutableStateFlow(value = NativeWordSuggestionsState())

    private val nativeWordState = MutableStateFlow(value = cardAdditionState.value.nativeWord)
    private val foreignWordState = MutableStateFlow(value = cardAdditionState.value.foreignWord)
    private val ipaHoldersState = MutableStateFlow(value = cardAdditionState.value.ipaHolders)
    private val letterInfosState = MutableStateFlow(value = cardAdditionState.value.letterInfos)

    private var autocompleteFetchingJob: Job? = null
    private var nativeWordSuggestionsFetchingJob: Job? = null

    init {
        observeCardManagementChanges()
        observeForeignWordChanges()
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
            is NativeWordSelected -> updateDataOnNativeWordSelected(wordIndex = event.wordIndex)
            ConfirmSuggestionsSelection -> handleNativeWordSuggestionsSelectionConfirmation()
            ClearNativeWordSuggestionsSelectionClicked -> {
                handleClearNativeWordSuggestionsSelectionClicked()
            }

            is UpdateIpa -> {
                updateIpa(letterGroupIndex = event.letterGroupIndex, ipa = event.ipa)
            }

            is UpdateNativeWord -> updateNativeWord(word = event.word)
            is AddNewCard -> {
                saveNewCard(
                    deckId = event.deckId,
                    nativeWord = event.nativeWord,
                    foreignWord = event.foreignWord,
                    ipaHolders = event.ipaHolders,
                )
            }

            PronounceForeignWordClicked -> audioPlayer.play()
            ManageNativeWordSuggestionsMenuState -> {
                nativeWordSuggestionsState.update { it.copy(isActive = !it.isActive) }
            }

            CloseAutocompleteMenu -> {
                autocompleteState.update { it.copy(isActive = false) }
            }

            CloseNativeWordSuggestionsMenu -> {
                nativeWordSuggestionsState.update { it.copy(isActive = false) }
            }
        }
    }

    private fun observeCardManagementChanges() {
        combine(
            letterInfosState,
            nativeWordState,
            ipaHoldersState,
            foreignWordState,
        ) { letterInfos, nativeWord, ipaHolders, foreignWord ->
            logD {
                message("observeCardManagementChanges() called")
                message("letterInfos: $letterInfos")
                message("nativeWord: $nativeWord")
                message("ipaHolders: $ipaHolders")
                message("foreignWord: $foreignWord")
            }
            CardAdditionState.Adding(
                letterInfos = letterInfos,
                nativeWord = nativeWord,
                foreignWord = foreignWord,
                ipaHolders = ipaHolders
            )
        }.onEach { addingState -> cardAdditionState.value = addingState }
            .flowOn(Dispatchers.IO)
            .launchIn(viewModelScope)
    }

    private fun observeForeignWordChanges() {
        foreignWordState.onEach { foreignWord ->
            logD {
                message("observeForeignWordChanges() called")
                message("foreignWord: $foreignWord")
                message("isActive: ${autocompleteState.value.isActive}")
            }

            audioPlayer.preparePronunciation(word = foreignWord)
            nativeWordState.value = ""
            nativeWordSuggestionsFetchingJob?.cancel()
            nativeWordSuggestionsFetchingJob = viewModelScope.launch {
                retrieveNativeWordSuggestionsState(foreignWord = foreignWord)
            }
        }.flowOn(Dispatchers.IO)
            .launchIn(viewModelScope)
    }

    private suspend fun retrieveNativeWordSuggestionsState(foreignWord: String) {
        if (foreignWord.isNotEmpty()) {
            try {
                nativeWordSuggestionsState.update { it.copy(loadingState = LoadingState.Loading) }

                val suggestions = fetchNativeWordSuggestions(word = foreignWord)
                val suggestionItems = suggestions.map { suggestion ->
                    NativeWordSuggestionItem(word = suggestion, isSelected = false)
                }

                nativeWordSuggestionsState.value = NativeWordSuggestionsState(
                    suggestions = suggestionItems,
                    isActive = false,
                    loadingState = LoadingState.Success(data = Unit)
                )
                logD("retrievedSuggestions: $suggestions")
            } catch (e: io.ktor.serialization.JsonConvertException) {
                logE("observeForeignWordChanges() catch for foreignWord: '$foreignWord'")
                throw e
            } catch (e: Exception) {
                logW("observeForeignWordChanges() catch for foreignWord: $foreignWord: ${e.stackTraceToString()}")
            }
        } else {
            nativeWordSuggestionsState.value = NativeWordSuggestionsState()
        }
    }

    private fun saveNewCard(
        deckId: Int,
        nativeWord: String,
        foreignWord: String,
        ipaHolders: List<IpaHolder>,
    ) {
        if (nativeWordState.value.isEmpty() || foreignWord.isEmpty()) {
            eventMessage.tryEmitAsNegative(resId = R.string.native_and_foreign_words_must_be_filled)
        } else {
            val newCard = Card(
                deckId = deckId,
                nativeWord = nativeWord,
                foreignWord = foreignWord,
                ipa = ipaHolders.map { ipaHolder -> ipaHolder.copy(ipa = ipaHolder.ipa.trim()) }
            )

            viewModelScope.launchWithState(Dispatchers.IO) {
                addNewCardIntoDeck(card = newCard)
                resetAddingState()
                audioPlayer.preparePronunciation(word = "")
                cardAdditionState.value = CardAdditionState.Finished
                eventMessage.tryEmitAsPositive(resId = R.string.card_has_been_added)
            }.onExceptionWithCrashlyticsReport(crashlytics = crashlytics) { _, _ ->
                eventMessage.tryEmitAsNegative(resId = R.string.exception_adding_card)
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
        if (word.length < MAX_NATIVE_WORD_LENGTH) {
            nativeWordState.value = word
        } else {
            eventMessage.tryEmitAsNegative(
                resId = R.string.native_word_is_too_long,
                duration =  EventMessage.Duration.Long
            )
        }
    }

    private fun updateDataOnForeignWordChanged(word: String) {
        if (word.length < MAX_FOREIGN_WORD_LENGTH) {
            foreignWordState.value = word
            letterInfosState.value = word.generateLetterInfos()
            ipaHoldersState.value = emptyList()

            autocompleteFetchingJob?.cancel()
            autocompleteFetchingJob = viewModelScope.launchWithState(context = Dispatchers.IO) {
                autocompleteState.value = AutocompleteState(
                    prefix = word,
                    autocomplete = fetchWordAutocomplete(prefix = word),
                    isActive = true,
                )
            } onException { _, throwable ->
                crashlytics.report(exception = throwable)
                logE(throwable.stackTraceToString())
            }
        } else {
            eventMessage.tryEmitAsNegative(
                resId = R.string.foreign_word_is_too_long,
                duration = EventMessage.Duration.Long
            )
        }
    }

    private fun updateDataOnAutocompleteSelected(word: String) {
        letterInfosState.value = word.generateLetterInfos()
        foreignWordState.value = word
        ipaHoldersState.value = emptyList()
        autocompleteState.value = AutocompleteState()
    }

    private fun updateDataOnNativeWordSelected(wordIndex: Int) {
        nativeWordSuggestionsState.update { suggestionsState ->
            val updatedSuggestions = suggestionsState.suggestions
                .updatedAt(index = wordIndex) { oldValue ->
                    oldValue.copy(isSelected = !oldValue.isSelected)
                }

            suggestionsState.copy(suggestions = updatedSuggestions)
        }
    }

    private fun handleNativeWordSuggestionsSelectionConfirmation() {
        nativeWordSuggestionsState.update { it.copy(isActive = false) }

        val selectedNativeWordSuggestions = nativeWordSuggestionsState.value.suggestions
            .filter { suggestionItem -> suggestionItem.isSelected }
            .joinToString(separator = ", ") { suggestionItem -> suggestionItem.word }

        if (selectedNativeWordSuggestions.length <= MAX_NATIVE_WORD_LENGTH) {
            nativeWordState.value = selectedNativeWordSuggestions
        } else {
            eventMessage.tryEmitAsNegative(
                resId = R.string.native_word_is_too_long,
                duration =  EventMessage.Duration.Long
            )
        }
    }

    private fun handleClearNativeWordSuggestionsSelectionClicked() {
        nativeWordSuggestionsState.update {
            val updatedSuggestions = it.suggestions.map { suggestionItem ->
                suggestionItem.copy(isSelected = false)
            }

            it.copy(suggestions = updatedSuggestions, isActive = false)
        }
        nativeWordState.value = ""
    }

    private fun updateIpa(letterGroupIndex: Int, ipa: String) {
        if (ipa.length < MAX_IPA_LENGTH) {
            ipaHoldersState.update { ipaHolders ->
                ipaHolders.updatedAt(index = letterGroupIndex) { oldValue ->
                    oldValue.copy(ipa = ipa)
                }
            }
        } else {
            eventMessage.tryEmitAsNegative(
                resId = R.string.ipa_is_too_long,
                duration = EventMessage.Duration.Long
            )
        }
    }

    private fun resetAddingState() {
        letterInfosState.value = emptyList()
        nativeWordState.value = ""
        foreignWordState.value = ""
        ipaHoldersState.value = emptyList()
    }
}