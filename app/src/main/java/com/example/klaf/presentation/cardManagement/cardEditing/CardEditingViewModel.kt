package com.example.klaf.presentation.cardManagement.cardEditing

import androidx.lifecycle.viewModelScope
import com.example.domain.common.CoroutineStateHolder.Companion.launchWithState
import com.example.domain.common.CoroutineStateHolder.Companion.onException
import com.example.domain.common.CoroutineStateHolder.Companion.onExceptionWithCrashlyticsReport
import com.example.domain.common.catchWithCrashlyticsReport
import com.example.domain.common.ifNotNull
import com.example.domain.entities.Card
import com.example.domain.entities.Deck
import com.example.domain.ipa.IpaHolder
import com.example.domain.ipa.LetterInfo
import com.example.domain.repositories.CrashlyticsRepository
import com.example.domain.useCases.FetchCardUseCase
import com.example.domain.useCases.FetchDeckByIdUseCase
import com.example.domain.useCases.FetchWordAutocompleteUseCase
import com.example.domain.useCases.UpdateCardUseCase
import com.example.klaf.R
import com.example.klaf.data.networking.CardAudioPlayer
import com.example.klaf.presentation.cardManagement.cardAddition.AutocompleteState
import com.example.klaf.presentation.common.EventMessage
import com.example.klaf.presentation.common.tryEmit
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*

class CardEditingViewModel @AssistedInject constructor(
    @Assisted(DECK_ARGUMENT_NAME) private val deckId: Int,
    @Assisted(CARD_ARGUMENT_NAME) cardId: Int,
    override val audioPlayer: CardAudioPlayer,
    fetchDeckById: FetchDeckByIdUseCase,
    fetchCard: FetchCardUseCase,
    private val updateCard: UpdateCardUseCase,
    private val fetchWordAutocomplete: FetchWordAutocompleteUseCase,
    private val crashlytics: CrashlyticsRepository,
) : BaseCardEditingViewModel() {

    companion object {

        const val DECK_ARGUMENT_NAME = "deck_id"
        const val CARD_ARGUMENT_NAME = "card_id"
    }

    override val eventMessage = MutableSharedFlow<EventMessage>(extraBufferCapacity = 1)

    override val deck: SharedFlow<Deck?> = fetchDeckById(deckId = deckId)
        .catchWithCrashlyticsReport(crashlytics = crashlytics) {
            eventMessage.tryEmit(messageId = R.string.problem_with_fetching_deck)
        }.shareIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            replay = 1
        )

    override val card: SharedFlow<Card?> = fetchCard(cardId = cardId)
        .catchWithCrashlyticsReport(crashlytics = crashlytics) {
            eventMessage.tryEmit(messageId = R.string.problem_with_fetching_card)
        }.onEach { card: Card? ->
            card?.ifNotNull { audioPlayer.preparePronunciation(word = it.foreignWord) }
        }.shareIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            replay = 1
        )

    override val cardEditingState = MutableStateFlow(value = CardEditingState.NOT_CHANGED)

    override val autocompleteState = MutableStateFlow(value = AutocompleteState())

    override val pronunciationLoadingState = audioPlayer.loadingState
    private var autocompleteFetchingJob: Job? = null

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
                eventMessage.tryEmit(messageId = R.string.native_and_foreign_words_must_be_filled)
            }
            updatedCard == oldCard -> {
                eventMessage.tryEmit(messageId = R.string.card_has_not_been_changed)
            }
            else -> {
                viewModelScope.launchWithState {
                    updateCard(newCard = updatedCard)
                    eventMessage.tryEmit(R.string.card_has_been_changed)
                    cardEditingState.value = CardEditingState.CHANGED
                }.onExceptionWithCrashlyticsReport(crashlytics = crashlytics) { _, _ ->
                    eventMessage.tryEmit(messageId = R.string.problem_with_updating_card)
                }
            }
        }
    }

    override fun pronounce() {
        audioPlayer.play()
    }

    override fun preparePronunciation(word: String) {
        audioPlayer.preparePronunciation(word = word)
    }

    override fun updateAutocompleteState(word: String) {
        val clearedWord = word.trim()

        preparePronunciation(word = word)
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
    }

    override fun closeAutocompleteMenu() {
        autocompleteState.update { it.copy(isActive = false) }
    }
}