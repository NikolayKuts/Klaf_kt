package com.example.klaf.presentation.cardManagement.cardAddition

import androidx.lifecycle.viewModelScope
import com.example.domain.common.CoroutineStateHolder.Companion.launchWithState
import com.example.domain.common.CoroutineStateHolder.Companion.onException
import com.example.domain.common.CoroutineStateHolder.Companion.onExceptionWithCrashlyticsReport
import com.example.domain.common.catchWithCrashlyticsReport
import com.example.domain.common.generateLetterInfos
import com.example.domain.common.updatedAt
import com.example.domain.entities.Card
import com.example.domain.entities.Deck
import com.example.domain.ipa.IpaHolder
import com.example.domain.ipa.LetterInfo
import com.example.domain.ipa.toRowIpaItemHolders
import com.example.domain.repositories.CrashlyticsRepository
import com.example.domain.useCases.AddNewCardIntoDeckUseCase
import com.example.domain.useCases.FetchDeckByIdUseCase
import com.example.domain.useCases.FetchWordAutocompleteUseCase
import com.example.klaf.R
import com.example.klaf.data.networking.CardAudioPlayer
import com.example.klaf.presentation.cardManagement.cardAddition.CardAdditionEvent.*
import com.example.klaf.presentation.cardManagement.common.MAX_IPA_LENGTH
import com.example.klaf.presentation.cardManagement.common.MAX_WORD_LENGTH
import com.example.klaf.presentation.common.EventMessage
import com.example.klaf.presentation.common.tryEmitAsNegative
import com.example.klaf.presentation.common.tryEmitAsPositive
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*

class CardAdditionViewModel @AssistedInject constructor(
    @Assisted deckId: Int,
    @Assisted smartSelectedWord: String?,
    fetchDeckById: FetchDeckByIdUseCase,
    private val addNewCardIntoDeck: AddNewCardIntoDeckUseCase,
    override val audioPlayer: CardAudioPlayer,
    private val fetchWordAutocomplete: FetchWordAutocompleteUseCase,
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

    private val nativeWordState = MutableStateFlow(value = cardAdditionState.value.nativeWord)
    private val foreignWordState = MutableStateFlow(value = cardAdditionState.value.foreignWord)
    private val ipaHoldersState = MutableStateFlow(value = cardAdditionState.value.ipaHolders)
    private val letterInfosState = MutableStateFlow(value = cardAdditionState.value.letterInfos)

    private var autocompleteFetchingJob: Job? = null

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
            PronounceForeignWord -> audioPlayer.play()
            CloseAutocompleteMenu -> {
                autocompleteState.update { it.copy(isActive = false) }
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
            CardAdditionState.Adding(
                letterInfos = letterInfos,
                nativeWord = nativeWord,
                foreignWord = foreignWord,
                ipaHolders = ipaHolders
            )
        }.onEach { addingState ->
            cardAdditionState.value = addingState
        }.launchIn(viewModelScope)
    }

    private fun observeForeignWordChanges() {
        foreignWordState.onEach { foreignWord ->
            audioPlayer.preparePronunciation(word = foreignWord)
        }.launchIn(viewModelScope)
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

            viewModelScope.launchWithState {
                addNewCardIntoDeck(card = newCard)
                resetAddingState()
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
        if (word.length < MAX_WORD_LENGTH) {
            nativeWordState.value = word
        }
    }

    private fun updateDataOnForeignWordChanged(word: String) {
        if (word.length < MAX_WORD_LENGTH) {
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
            } onException { _, throwable -> crashlytics.report(exception = throwable) }
        }
    }

    private fun updateDataOnAutocompleteSelected(word: String) {
        letterInfosState.value = word.generateLetterInfos()
        foreignWordState.value = word
        ipaHoldersState.value = emptyList()
        autocompleteState.value = AutocompleteState()
    }

    private fun updateIpa(letterGroupIndex: Int, ipa: String) {
        if (ipa.length < MAX_IPA_LENGTH) {
            ipaHoldersState.update { ipaHolders ->
                ipaHolders.updatedAt(index = letterGroupIndex) { oldValue ->
                    oldValue.copy(ipa = ipa)
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