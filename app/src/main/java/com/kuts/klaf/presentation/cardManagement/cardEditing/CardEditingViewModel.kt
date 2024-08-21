package com.kuts.klaf.presentation.cardManagement.cardEditing

import androidx.lifecycle.viewModelScope
import com.kuts.domain.common.CoroutineStateHolder.Companion.launchWithState
import com.kuts.domain.common.CoroutineStateHolder.Companion.onException
import com.kuts.domain.common.CoroutineStateHolder.Companion.onExceptionWithCrashlyticsReport
import com.kuts.domain.common.LoadingState
import com.kuts.domain.common.catchWithCrashlyticsReport
import com.kuts.domain.common.debouncedLaunch
import com.kuts.domain.common.ifNotNull
import com.kuts.domain.entities.Card
import com.kuts.domain.entities.Deck
import com.kuts.domain.ipa.IpaHolder
import com.kuts.domain.ipa.LetterInfo
import com.kuts.domain.repositories.CrashlyticsRepository
import com.kuts.domain.useCases.FetchCardUseCase
import com.kuts.domain.useCases.FetchDeckByIdUseCase
import com.kuts.domain.useCases.FetchWordAutocompleteUseCase
import com.kuts.domain.useCases.FetchWordInfoUseCase
import com.kuts.domain.useCases.UpdateCardUseCase
import com.kuts.klaf.R
import com.kuts.klaf.data.networking.CardAudioPlayer
import com.kuts.klaf.data.networking.yandexApi.YandexWordInfoProvider
import com.kuts.klaf.presentation.cardManagement.cardAddition.AutocompleteState
import com.kuts.klaf.presentation.cardManagement.cardAddition.NativeWordSuggestionItem
import com.kuts.klaf.presentation.cardManagement.cardAddition.NativeWordSuggestionsState
import com.kuts.klaf.presentation.common.EventMessage
import com.kuts.klaf.presentation.common.tryEmitAsNegative
import com.kuts.klaf.presentation.common.tryEmitAsPositive
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update

class CardEditingViewModel @AssistedInject constructor(
    @Assisted(DECK_ARGUMENT_NAME) private val deckId: Int,
    @Assisted(CARD_ARGUMENT_NAME) cardId: Int,
    override val audioPlayer: CardAudioPlayer,
    fetchDeckById: FetchDeckByIdUseCase,
    fetchCard: FetchCardUseCase,
    private val updateCard: UpdateCardUseCase,
    private val fetchWordAutocomplete: FetchWordAutocompleteUseCase,
    private val fetchWordInfo: FetchWordInfoUseCase,
    private val crashlytics: CrashlyticsRepository,
) : BaseCardEditingViewModel() {

    companion object {

        const val DECK_ARGUMENT_NAME = "deck_id"
        const val CARD_ARGUMENT_NAME = "card_id"
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

    override val card: SharedFlow<Card?> = fetchCard(cardId = cardId)
        .catchWithCrashlyticsReport(crashlytics = crashlytics) {
            eventMessage.tryEmitAsNegative(resId = R.string.problem_with_fetching_card)
        }.onEach { card: Card? ->
            card?.ifNotNull {
                audioPlayer.preparePronunciation(word = it.foreignWord)
                typedForeignWord.value = card.foreignWord
            }
        }.shareIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            replay = 1
        )

    override val cardEditingState = MutableStateFlow(value = CardEditingState.NOT_CHANGED)

    override val autocompleteState = MutableStateFlow(value = AutocompleteState())

    override val pronunciationLoadingState = audioPlayer.loadingState

    override val nativeWordSuggestionsState = MutableStateFlow(value = NativeWordSuggestionsState())

    override val transcriptionState = MutableStateFlow(value = "")

    private val typedForeignWord = MutableStateFlow("")

    private var autocompleteFetchingJob: Job? = null

    init {
        observeTypedForeignWordChange()
    }

    override fun updateCard(
        oldCard: Card,
        deckId: Int,
        nativeWord: String,
        foreignWord: String,
        letterInfos: List<LetterInfo>,
        ipaHolders: List<IpaHolder>,
    ) {
        val updatedCard = oldCard.copy(
            deckId = deckId,
            nativeWord = nativeWord,
            foreignWord = foreignWord,
            ipa = ipaHolders.map { ipaHolder -> ipaHolder.copy(ipa = ipaHolder.ipa.trim()) }
        )

        when {
            updatedCard.nativeWord.isEmpty() || updatedCard.foreignWord.isEmpty() -> {
                eventMessage.tryEmitAsNegative(resId = R.string.native_and_foreign_words_must_be_filled)
            }

            updatedCard == oldCard -> {
                eventMessage.tryEmitAsNegative(resId = R.string.card_has_not_been_changed)
            }

            else -> {
                viewModelScope.launchWithState {
                    updateCard(newCard = updatedCard)
                    eventMessage.tryEmitAsPositive(resId = R.string.card_has_been_changed)
                    cardEditingState.value = CardEditingState.CHANGED
                }.onExceptionWithCrashlyticsReport(crashlytics = crashlytics) { _, _ ->
                    eventMessage.tryEmitAsNegative(resId = R.string.problem_with_updating_card)
                }
            }
        }
    }

    override fun pronounce() {
        audioPlayer.play()
    }

    private fun preparePronunciation(word: String) {
        audioPlayer.preparePronunciation(word = word)
    }

    override fun updateEditingState(word: String) {
        val clearedWord = word.trim()

        preparePronunciation(word = clearedWord)
        typedForeignWord.value = clearedWord
        autocompleteFetchingJob?.cancel()
        autocompleteFetchingJob = viewModelScope.launchWithState(context = Dispatchers.IO) {
            autocompleteState.value = AutocompleteState(
                prefix = clearedWord,
                autocomplete = fetchWordAutocomplete(prefix = clearedWord),
                isActive = true,
            )
        } onException { _, throwable -> crashlytics.report(exception = throwable) }
    }

    override fun setSelectedAutocomplete(selectedWord: String) {
        autocompleteState.value = AutocompleteState()
        preparePronunciation(word = selectedWord)
        typedForeignWord.value = selectedWord
    }

    override fun closeAutocompleteMenu() {
        autocompleteState.update { it.copy(isActive = false) }
    }

    override fun manageNativeWordSuggestionsState(state: NativeWordSuggestionsState) {
        nativeWordSuggestionsState.value = state
    }

    override fun sendEventMessage(message: EventMessage) {
        eventMessage.tryEmit(value = message)
    }

    private fun observeTypedForeignWordChange() {
        typedForeignWord.debouncedLaunch(
            scope = viewModelScope,
            execute = { foreignWord ->
                val trimmedForeignWord = foreignWord.trim()

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
                        handleRetrieveWordInfoError(loadingState = wordInfoState)
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

    private fun handleRetrieveWordInfoError(loadingState: LoadingState.Error) {
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