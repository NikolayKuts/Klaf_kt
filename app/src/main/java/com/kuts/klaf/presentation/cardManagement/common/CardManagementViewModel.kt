package com.kuts.klaf.presentation.cardManagement.common

import androidx.lifecycle.viewModelScope
import com.kuts.domain.common.CoroutineStateHolder.Companion.onExceptionWithCrashlyticsReport
import com.kuts.domain.common.DebouncedMutableStateFlow
import com.kuts.domain.common.LoadingState
import com.kuts.domain.common.catchWithCrashlyticsReport
import com.kuts.domain.common.debouncedLaunch
import com.kuts.domain.common.generateLetterInfos
import com.kuts.domain.common.updatedAt
import com.kuts.domain.entities.Deck
import com.kuts.domain.ipa.IpaHolder
import com.kuts.domain.ipa.LetterInfo
import com.kuts.domain.ipa.toRowIpaItemHolders
import com.kuts.domain.repositories.CrashlyticsRepository
import com.kuts.domain.useCases.FetchDeckByIdUseCase
import com.kuts.domain.useCases.FetchWordAutocompleteUseCase
import com.kuts.domain.useCases.FetchWordInfoUseCase
import com.kuts.klaf.R
import com.kuts.klaf.data.networking.CardAudioPlayer
import com.kuts.klaf.data.networking.yandexApi.YandexWordInfoProvider
import com.kuts.klaf.presentation.cardManagement.cardAddition.AutocompleteState
import com.kuts.klaf.presentation.cardManagement.cardAddition.NativeWordSuggestionItem
import com.kuts.klaf.presentation.cardManagement.cardAddition.NativeWordSuggestionsState
import com.kuts.klaf.presentation.common.EventMessage
import com.kuts.klaf.presentation.common.tryEmitAsNegative
import com.lib.lokdroid.core.logE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update

abstract class CardManagementViewModel(
    deckId: Int,
    audioPlayer: CardAudioPlayer,
    private val fetchWordAutocomplete: FetchWordAutocompleteUseCase,
    private val fetchWordInfo: FetchWordInfoUseCase,
    protected val crashlytics: CrashlyticsRepository,
    fetchDeckById: FetchDeckByIdUseCase,
) : BaseCardManagementViewModel(audioPlayer = audioPlayer) {

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
    override val pronunciationLoadingState: StateFlow<LoadingState<Unit>> = audioPlayer.loadingState
    override val nativeWordSuggestionsState = MutableStateFlow(value = NativeWordSuggestionsState())
    override val transcriptionState = MutableStateFlow(value = "")

    override val cardManagementState =
        MutableStateFlow<CardManagementState>(value = CardManagementState.InProgress())
    protected val nativeWordState: MutableStateFlow<String> = MutableStateFlow(value = "")
    protected val foreignWordState: MutableStateFlow<String> = MutableStateFlow(value = "")
    protected val ipaHoldersState = MutableStateFlow<List<IpaHolder>>(value = emptyList())
    protected val letterInfosState = MutableStateFlow<List<LetterInfo>>(value = emptyList())

    init {
        combineAndObserveCardManagementChanges()
        observeForeignWordChanges()
    }

    override fun sendEvent(event: CardManagementEvent) {
        when (event) {
            is CardManagementEvent.ChangeLetterSelectionWithIpaTemplate -> {
                changeLetterSelectionWithIpaTemplate(
                    index = event.index,
                    letterInfo = event.letterInfo
                )
            }

            is CardManagementEvent.UpdateDataOnForeignWordChanged -> {
                updateDataOnForeignWordChanged(word = event.word)
            }

            is CardManagementEvent.UpdateDataOnAutocompleteSelected -> {
                updateDataOnAutocompleteSelected(word = event.word)
            }

            is CardManagementEvent.NativeWordSelected -> {
                updateDataOnNativeWordSelected(wordIndex = event.wordIndex)
            }

            CardManagementEvent.ConfirmSuggestionsSelection -> {
                handleNativeWordSuggestionsSelectionConfirmation()
            }

            CardManagementEvent.ClearNativeWordSuggestionsSelectionClicked -> {
                handleClearNativeWordSuggestionsSelectionClicked()
            }

            is CardManagementEvent.UpdateIpa -> {
                updateIpa(letterGroupIndex = event.letterGroupIndex, ipa = event.ipa)
            }

            is CardManagementEvent.UpdateNativeWord -> {
                updateNativeWord(word = event.word)
            }

            is CardManagementEvent.CardManagementConfirmed -> {
                onCardManagementConfirmed()
            }

            CardManagementEvent.PronounceForeignWordClicked -> {
                audioPlayer.play()
            }

            CardManagementEvent.NativeWordFeildIconClicked -> {
                autocompleteState.update { it.copy(isActive = false) }
                nativeWordSuggestionsState.update { it.copy(isActive = !it.isActive) }
            }

            CardManagementEvent.CloseAutocompleteMenu -> {
                autocompleteState.update { it.copy(isActive = false) }
            }

            CardManagementEvent.CloseNativeWordSuggestionsMenu -> {
                nativeWordSuggestionsState.update { it.copy(isActive = false) }
            }
        }
    }

    abstract fun onCardManagementConfirmed()

    private fun combineAndObserveCardManagementChanges() {
        combine(
            letterInfosState,
            nativeWordState,
            ipaHoldersState,
            foreignWordState,
        ) { letterInfos, nativeWord, ipaHolders, foreignWord ->
            CardManagementState.InProgress(
                letterInfos = letterInfos,
                nativeWord = nativeWord,
                foreignWord = foreignWord,
                ipaHolders = ipaHolders
            )
        }.onEach { addingState -> cardManagementState.value = addingState }
            .flowOn(Dispatchers.IO)
            .launchIn(viewModelScope)
    }

    private fun observeForeignWordChanges() {
        foreignWordState.map { it.trim() }.debouncedLaunch(
            scope = viewModelScope,
            execute = { foreignWord ->
                val trimmedForeignWord = foreignWord.trim()

                audioPlayer.preparePronunciation(word = trimmedForeignWord)

                if (trimmedForeignWord.isNotEmpty()) {
                    fetchWordInfo(word = trimmedForeignWord)
                } else {
                    flow { emit(LoadingState.Non) }
                }
            },
            onEach = { wordInfoState ->
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
            },
        )
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

    private fun updateDataOnForeignWordChanged(word: String) {
        if (word.length < MAX_FOREIGN_WORD_LENGTH) {
            foreignWordState.value = word
            letterInfosState.value = word.generateLetterInfos()
            nativeWordState.value = ""
            ipaHoldersState.value = emptyList()

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
        } else {
            eventMessage.tryEmitAsNegative(
                resId = R.string.foreign_word_is_too_long,
                duration = EventMessage.Duration.Long
            )
        }
    }

    private fun updateDataOnAutocompleteSelected(word: String) {
        foreignWordState.value = word
        nativeWordState.value = ""
        letterInfosState.value = word.generateLetterInfos()
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

    private fun updateNativeWord(word: String) {
        if (word.length < MAX_NATIVE_WORD_LENGTH) {
            nativeWordState.value = word
        } else {
            eventMessage.tryEmitAsNegative(
                resId = R.string.native_word_is_too_long,
                duration = EventMessage.Duration.Long
            )
        }
    }

    private fun handleWordInfoError(loadingState: LoadingState.Error) {
        val errorMessageId = when (val error = loadingState.value) {
            is YandexWordInfoProvider.WordInfoLoadingError -> {
                when (error) {
                    is YandexWordInfoProvider.WordInfoLoadingError.Common,
                    YandexWordInfoProvider.WordInfoLoadingError.JsonConvert -> {
                        R.string.word_info_retrieving_common_warning_message
                    }
                }
            }

            else -> R.string.word_info_retrieving_common_warning_message
        }

        eventMessage.tryEmitAsNegative(resId = errorMessageId)
    }

    private fun getWrappedTranscription(value: String): String {
        return if (value.isEmpty()) "" else "[$value]"
    }
}