package com.kuts.klaf.cardManagement.common

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.viewModelScope
import com.cambridge.dictionary.client.CambridgeClient
import com.kuts.domain.common.CoroutineStateHolder.Companion.onExceptionWithCrashlyticsReport
import com.kuts.domain.common.DebouncedMutableStateFlow
import com.kuts.domain.common.LoadingState
import com.kuts.domain.common.catchWithCrashlyticsReport
import com.kuts.domain.common.generateLetterInfos
import com.kuts.domain.common.ifTrue
import com.kuts.domain.common.updatedAt
import com.kuts.domain.entities.Deck
import com.kuts.domain.ipa.LetterInfo
import com.kuts.domain.ipa.toRowIpaItemHolders
import com.kuts.domain.managers.IAudioPlayerManager
import com.kuts.domain.repositories.ICrashlyticsRepository
import com.kuts.domain.repositories.IWordInfoRepository
import com.kuts.domain.repositories.IWordInfoRepository.IWordInfoLoadingError
import com.kuts.domain.useCases.CheckIfCardExistsUseCase
import com.kuts.domain.useCases.FetchDeckByIdUseCase
import com.kuts.domain.useCases.FetchWordAutocompleteUseCase
import com.kuts.domain.useCases.FetchWordInfoUseCase
import com.kuts.klaf.presentation.R
import com.kuts.klaf.cardManagement.cardAddition.AutocompleteState
import com.kuts.klaf.cardManagement.cardAddition.NativeWordSuggestionItem
import com.kuts.klaf.cardManagement.cardAddition.NativeWordSuggestionsState
import com.kuts.klaf.common.EventMessage
import com.kuts.klaf.common.tryEmitAsNegative
import com.lib.lokdroid.core.logE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

abstract class CardManagementViewModel(
    deckId: Int,
    audioPlayer: IAudioPlayerManager,
    cambridgeClient: CambridgeClient,
    private val fetchWordAutocomplete: FetchWordAutocompleteUseCase,
    private val fetchWordInfo: FetchWordInfoUseCase,
    protected val crashlytics: ICrashlyticsRepository,
    protected val checkIfWordExists: CheckIfCardExistsUseCase,
    fetchDeckById: FetchDeckByIdUseCase,
) : BaseCardManagementViewModel(
    audioPlayer = audioPlayer,
    cambridgeClient = cambridgeClient
) {

    companion object {

        private val ipaKeys = listOf(
            "θ", "ð", "ʃ", "ʒ", "ŋ", "tʃ", "dʒ", "ʔ", "ɹ",
            "æ", "ʌ", "ɜː", "ə", "ɪ", "ʊ", "ɔː", "ɒ",
            "aɪ", "eɪ", "aʊ", "ɔɪ", "əʊ", "ɪə", "eə", "ʊə"
        )
    }

    override val eventMessage = MutableSharedFlow<EventMessage>(extraBufferCapacity = 1)

    override val deck: SharedFlow<Deck?> = fetchDeckById(deckId = deckId)
        .catchWithCrashlyticsReport(crashlytics = crashlytics) {
            eventMessage.tryEmitAsNegative(resId = R.string.problem_with_fetching_deck)
        }.shareIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            replay = 1
        )

    override val autocompleteState = DebouncedMutableStateFlow(value = AutocompleteState())
    override val pronunciationLoadingState: StateFlow<LoadingState<Unit, Unit>> =
        audioPlayer.loadingState
    override val nativeWordSuggestionsState = MutableStateFlow(value = NativeWordSuggestionsState())
    override val transcriptionState = MutableStateFlow(value = "")

    override val cardManagementState =
        MutableStateFlow<CardManagementState>(value = CardManagementState.InProgress())
    protected val nativeWordFieldValueState = MutableStateFlow(value = TextFieldValue())
    protected val foreignWordFieldValueState = MutableStateFlow(value = TextFieldValue())
    protected val textFieldValueIpaHoldersState =
        MutableStateFlow<List<TextFieldValueIpaHolder>>(value = emptyList())
    protected val letterInfosState = MutableStateFlow<List<LetterInfo>>(value = emptyList())

    override val cambridgeDataState = MutableStateFlow<ICambridgeDataState>(
        value = ICambridgeDataState.Empty
    )

    override val ipaKeyboardState = MutableStateFlow(value = IpaKeyboardState(keys = ipaKeys))

    init {
        combineAndObserveCardManagementChanges()
        observeForeignWordChanges()
        observeTextFieldValueIpaHoldersState()
    }

    protected open suspend fun onForeignWordChanged(word: String) {
        if (word.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val wordData = cambridgeClient.fetchWordData(word = word)

            if (wordData == null) {
                cambridgeDataState.value = ICambridgeDataState.Empty
            } else {
                cambridgeDataState.value = ICambridgeDataState.Fetched(word = wordData)
            }
        }
    }

    override fun sendAction(action: ICardManagementAction) {
        when (action) {
            is ICardManagementAction.ChangeLetterSelectionWithIpaTemplate -> {
                changeLetterSelectionWithIpaTemplate(
                    index = action.index,
                    letterInfo = action.letterInfo
                )
            }

            is ICardManagementAction.UpdateDataOnForeignWordChanged -> {
                updateDataOnForeignWordChanged(wordFieldValue = action.wordFieldValue)
            }

            is ICardManagementAction.UpdateDataOnAutocompleteSelected -> {
                updateDataOnAutocompleteSelected(word = action.word)
            }

            is ICardManagementAction.NativeWordSelected -> {
                updateDataOnNativeWordSelected(wordIndex = action.wordIndex)
            }

            ICardManagementAction.ConfirmSuggestionsSelection -> {
                handleNativeWordSuggestionsSelectionConfirmation()
            }

            ICardManagementAction.ClearNativeWordSuggestionsSelectionClicked -> {
                handleClearNativeWordSuggestionsSelectionClicked()
            }

            is ICardManagementAction.UpdateIpa -> {
                updateIpa(letterGroupIndex = action.letterGroupIndex, ipaTextFieldValue = action.ipa)
            }

            is ICardManagementAction.UpdateNativeWord -> {
                updateNativeWord(wordFieldValue = action.wordFieldValue)
            }

            is ICardManagementAction.CardManagementConfirmed -> {
                onCardManagementConfirmed()
            }

            is ICardManagementAction.IpaTextFieldFocusChanged -> {
                handleIpaTextFieldFocusChanged(action = action)
            }

            ICardManagementAction.PronounceForeignWordClicked -> {
                audioPlayer.play()
            }

            ICardManagementAction.FetchGeminiInsightsClicked -> {
                onGeminiInsightsClicked()
            }

            ICardManagementAction.NativeWordFieldIconClicked -> {
                autocompleteState.update { it.copy(isActive = false) }
                nativeWordSuggestionsState.update { it.copy(isActive = !it.isActive) }
            }

            ICardManagementAction.CloseAutocompleteMenu -> {
                autocompleteState.update { it.copy(isActive = false) }
            }

            ICardManagementAction.CloseNativeWordSuggestionsMenu -> {
                nativeWordSuggestionsState.update { it.copy(isActive = false) }
            }
        }
    }

    abstract fun onCardManagementConfirmed()

    protected open fun onGeminiInsightsClicked() = Unit

    private fun handleIpaTextFieldFocusChanged(
        action: ICardManagementAction.IpaTextFieldFocusChanged
    ) {
        textFieldValueIpaHoldersState.update {
            it.toMutableList().apply {
                forEachIndexed { index, _ ->
                    if (action.focusList.size <= index) return
                    this[index] = this[index].copy(isFocused = action.focusList[index].isFocused)
                }
            }
        }
    }

    private fun combineAndObserveCardManagementChanges() {
        combine(
            letterInfosState,
            nativeWordFieldValueState,
            textFieldValueIpaHoldersState,
            foreignWordFieldValueState,
        ) { letterInfos, nativeWordFieldValue, textFieldValueIpaHolders, foreignWordFieldValue ->
            CardManagementState.InProgress(
                letterInfos = letterInfos,
                nativeWord = nativeWordFieldValue,
                foreignWord = foreignWordFieldValue,
                ipaHolders = textFieldValueIpaHolders
            )
        }.onEach { addingState -> cardManagementState.value = addingState }
            .flowOn(Dispatchers.IO)
            .launchIn(viewModelScope)
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    private fun observeForeignWordChanges() {
        foreignWordFieldValueState.map { fieldValue -> fieldValue.text.trim() }
            .distinctUntilChanged()
            .debounce(1500L)
            .onEach { onForeignWordChanged(word = it) }
            .flowOn(Dispatchers.IO)
            .launchIn(viewModelScope)

        foreignWordFieldValueState.map { fieldValue -> fieldValue.text.trim() }
            .distinctUntilChanged()
            .debounce(300)
            .flatMapLatest { foreignWord ->
                audioPlayer.preparePronunciation(word = foreignWord)

                if (foreignWord.isNotEmpty()) {
                    fetchWordInfo.invoke(word = foreignWord)
                } else {
                    flow { emit(LoadingState.Non) }
                }
            }.onEach { wordInfoState ->
                when (wordInfoState) {
                    LoadingState.Non -> {
                        transcriptionState.value = ""
                        nativeWordSuggestionsState.value = NativeWordSuggestionsState()
                    }

                    is LoadingState.Error -> {
                        handleWordInfoError(loadingState = wordInfoState)
                    }

                    LoadingState.Loading -> {
                        transcriptionState.value = ""
                        nativeWordSuggestionsState.update {
                            it.copy(loadingState = LoadingState.Loading)
                        }
                    }

                    is LoadingState.Success -> {
                        transcriptionState.value =
                            getWrappedTranscription(value = wordInfoState.data.transcription)

                        val suggestionItems =
                            wordInfoState.data.translations.map { suggestion ->
                                NativeWordSuggestionItem(word = suggestion, isSelected = false)
                            }

                        nativeWordSuggestionsState.value = NativeWordSuggestionsState(
                            suggestions = suggestionItems,
                            isActive = false,
                            loadingState = LoadingState.Success(data = Unit)
                        )
                    }
                }

            }.flowOn(Dispatchers.IO)
            .launchIn(viewModelScope)
    }

    private fun observeTextFieldValueIpaHoldersState() {
        textFieldValueIpaHoldersState.onEach { ipaValueHolders ->
            ipaKeyboardState.update { keyboardState ->
                val focusedHolder = ipaValueHolders.firstOrNull { holder -> holder.isFocused }

                keyboardState.copy(
                    enabled = ipaValueHolders.any { it.isFocused }
                ).let {
                    if (focusedHolder != null) {
                        it.copy(
                            holderIndex = ipaValueHolders.indexOf(focusedHolder),
                            ipaTextFieldValue = focusedHolder.ipaTextFieldValue
                        )
                    } else {
                        it
                    }
                }
            }
        }.flowOn(Dispatchers.IO)
            .launchIn(viewModelScope)
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

        textFieldValueIpaHoldersState.value = letterInfosState.value.toRowIpaItemHolders()
            .map { ipaHolder -> ipaHolder.toTextFieldValueIpaHolder() }
    }

    private fun updateDataOnForeignWordChanged(wordFieldValue: TextFieldValue) {
        val word = wordFieldValue.text
        val isForeignWordChanged = word != foreignWordFieldValueState.value.text

        if (word.length < MAX_FOREIGN_WORD_LENGTH) {
            foreignWordFieldValueState.value = wordFieldValue

            isForeignWordChanged.ifTrue {
                letterInfosState.value = word.generateLetterInfos()
                nativeWordFieldValueState.value = TextFieldValue()
                textFieldValueIpaHoldersState.value = emptyList()

                autocompleteState.launchUpdateWithState(
                    scope = viewModelScope,
                    context = Dispatchers.IO
                ) {
                    AutocompleteState(
                        prefix = word,
                        autocomplete = fetchWordAutocomplete(prefix = word),
                        isActive = true,
                    )
                }.onExceptionWithCrashlyticsReport(crashlytics = crashlytics) { _, throwable ->
                    logE("fetchWordAutocomplete() caught ERROR: ${throwable.stackTraceToString()}")
                }
            }
        } else {
            eventMessage.tryEmitAsNegative(
                resId = R.string.foreign_word_is_too_long,
                duration = EventMessage.Duration.Long
            )
        }
    }

    private fun updateDataOnAutocompleteSelected(word: String) {
        foreignWordFieldValueState.update {
            TextFieldValue(text = word, selection = TextRange(word.length))
        }
        nativeWordFieldValueState.update { it.copy(text = "") }
        letterInfosState.value = word.generateLetterInfos()
        textFieldValueIpaHoldersState.value = emptyList()
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
            nativeWordFieldValueState.value = TextFieldValue(
                text = selectedNativeWordSuggestions,
                selection = TextRange(selectedNativeWordSuggestions.length)
            )
        } else {
            eventMessage.tryEmitAsNegative(
                resId = R.string.native_word_is_too_long,
                duration = EventMessage.Duration.Long
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
        nativeWordFieldValueState.value = TextFieldValue()
    }

    private fun updateIpa(letterGroupIndex: Int, ipaTextFieldValue: TextFieldValue) {
        if (ipaTextFieldValue.text.length < MAX_IPA_LENGTH) {
            textFieldValueIpaHoldersState.update { textFieldValueIpaHolders ->
                textFieldValueIpaHolders.updatedAt(index = letterGroupIndex) { oldValue ->
                    oldValue.copy(ipaTextFieldValue = ipaTextFieldValue)
                }
            }
        } else {
            eventMessage.tryEmitAsNegative(
                resId = R.string.ipa_is_too_long,
                duration = EventMessage.Duration.Long
            )
        }
    }

    private fun updateNativeWord(wordFieldValue: TextFieldValue) {
        val word = wordFieldValue.text

        if (word.length < MAX_NATIVE_WORD_LENGTH) {
            nativeWordFieldValueState.value = wordFieldValue
        } else {
            eventMessage.tryEmitAsNegative(
                resId = R.string.native_word_is_too_long,
                duration = EventMessage.Duration.Long
            )
        }
    }

    private fun handleWordInfoError(loadingState: LoadingState.Error<IWordInfoRepository.IWordInfoLoadingError>) {
        val errorMessageId = when (loadingState.value) {
            IWordInfoLoadingError.Common,
            IWordInfoLoadingError.JsonConvert -> {
                R.string.word_info_retrieving_common_warning_message
            }
        }

        eventMessage.tryEmitAsNegative(resId = errorMessageId)
    }

    private fun getWrappedTranscription(value: String): String {
        return if (value.isEmpty()) "" else "[$value]"
    }

    protected suspend fun checkIfForeignWordExists(word: String) {
        val decksWithSameForeignWord = checkIfWordExists.invoke(foreignWord = word.lowercase())

        if (decksWithSameForeignWord.isNotEmpty()) {
            val deckNamesAsString = decksWithSameForeignWord.joinToString(", ") { it.name }

            eventMessage.tryEmitAsNegative(
                resId = R.string.foreign_word_already_exists,
                args = arrayOf(word, deckNamesAsString),
            )
        }
    }
}
