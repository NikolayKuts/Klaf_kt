package com.kuts.klaf.presentation.cardManagement.cardEditing

import androidx.lifecycle.viewModelScope
import com.kuts.domain.common.CoroutineStateHolder.Companion.launchWithState
import com.kuts.domain.common.CoroutineStateHolder.Companion.onException
import com.kuts.domain.common.CoroutineStateHolder.Companion.onExceptionWithCrashlyticsReport
import com.kuts.domain.common.catchWithCrashlyticsReport
import com.kuts.domain.common.ifNotNull
import com.kuts.domain.entities.Card
import com.kuts.domain.entities.Deck
import com.kuts.domain.ipa.IpaHolder
import com.kuts.domain.ipa.LetterInfo
import com.kuts.domain.repositories.CrashlyticsRepository
import com.kuts.domain.useCases.FetchCardUseCase
import com.kuts.domain.useCases.FetchDeckByIdUseCase
import com.kuts.domain.useCases.FetchWordAutocompleteUseCase
import com.kuts.domain.useCases.UpdateCardUseCase
import com.kuts.klaf.R
import com.kuts.klaf.data.networking.CardAudioPlayer
import com.kuts.klaf.presentation.cardManagement.cardAddition.AutocompleteState
import com.kuts.klaf.presentation.common.EventMessage
import com.kuts.klaf.presentation.common.tryEmit
import com.kuts.klaf.presentation.common.tryEmitAsNegative
import com.kuts.klaf.presentation.common.tryEmitAsPositive
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