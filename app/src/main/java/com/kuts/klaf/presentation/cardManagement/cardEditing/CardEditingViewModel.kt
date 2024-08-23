package com.kuts.klaf.presentation.cardManagement.cardEditing

import androidx.lifecycle.viewModelScope
import com.kuts.domain.common.CoroutineStateHolder.Companion.launchWithState
import com.kuts.domain.common.CoroutineStateHolder.Companion.onExceptionWithCrashlyticsReport
import com.kuts.domain.common.catchWithCrashlyticsReport
import com.kuts.domain.entities.Card
import com.kuts.domain.ipa.toLetterInfos
import com.kuts.domain.repositories.CrashlyticsRepository
import com.kuts.domain.useCases.FetchCardUseCase
import com.kuts.domain.useCases.FetchDeckByIdUseCase
import com.kuts.domain.useCases.FetchWordAutocompleteUseCase
import com.kuts.domain.useCases.FetchWordInfoUseCase
import com.kuts.domain.useCases.UpdateCardUseCase
import com.kuts.klaf.R
import com.kuts.klaf.data.networking.CardAudioPlayer
import com.kuts.klaf.presentation.cardManagement.common.CardManagementState
import com.kuts.klaf.presentation.cardManagement.common.CardManagementViewModel
import com.kuts.klaf.presentation.common.tryEmitAsNegative
import com.kuts.klaf.presentation.common.tryEmitAsPositive
import com.lib.lokdroid.core.logW
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull

class CardEditingViewModel @AssistedInject constructor(
    @Assisted(DECK_ARGUMENT_NAME) private val deckId: Int,
    @Assisted(CARD_ARGUMENT_NAME) cardId: Int,
    private val fetchCard: FetchCardUseCase,
    private val updateCard: UpdateCardUseCase,
    audioPlayer: CardAudioPlayer,
    fetchWordAutocomplete: FetchWordAutocompleteUseCase,
    fetchWordInfo: FetchWordInfoUseCase,
    crashlytics: CrashlyticsRepository,
    fetchDeckById: FetchDeckByIdUseCase,
) : CardManagementViewModel(
    deckId = deckId,
    audioPlayer = audioPlayer,
    fetchWordAutocomplete = fetchWordAutocomplete,
    fetchWordInfo = fetchWordInfo,
    crashlytics = crashlytics,
    fetchDeckById = fetchDeckById,
) {

    companion object {

        const val DECK_ARGUMENT_NAME = "deck_id"
        const val CARD_ARGUMENT_NAME = "card_id"
    }

    private val originalCardState = MutableStateFlow<Card?>(value = null)

    init {
        fetchCardAndConfigureStates(cardId = cardId)
    }

    override fun onCardManagementConfirmed() {
        val originalCard = originalCardState.value ?: return
        val deckId = deck.replayCache.first()?.id ?: return
        val nativeWord = cardManagementState.value.nativeWord
        val foreignWord = cardManagementState.value.foreignWord
        val ipaHoldersState = cardManagementState.value.ipaHolders
        val trimmedIpaHoldersState =
            ipaHoldersState.map { ipaHolder -> ipaHolder.copy(ipa = ipaHolder.ipa.trim()) }

        if (nativeWord.isEmpty() || foreignWord.isEmpty()) {
            eventMessage.tryEmitAsNegative(resId = R.string.native_and_foreign_words_must_be_filled)
        } else {
            val updatedCard = originalCard.copy(
                deckId = deckId,
                nativeWord = nativeWord,
                foreignWord = foreignWord,
                ipa = trimmedIpaHoldersState
            )

            when {
                updatedCard.nativeWord.isEmpty() || updatedCard.foreignWord.isEmpty() -> {
                    eventMessage.tryEmitAsNegative(resId = R.string.native_and_foreign_words_must_be_filled)
                }

                updatedCard == originalCard -> {
                    eventMessage.tryEmitAsNegative(resId = R.string.card_has_not_been_changed)
                }

                else -> {
                    viewModelScope.launchWithState {
                        updateCard(newCard = updatedCard)
                        eventMessage.tryEmitAsPositive(resId = R.string.card_has_been_changed)
                        cardManagementState.value = CardManagementState.Finished
                    }.onExceptionWithCrashlyticsReport(crashlytics = crashlytics) { _, _ ->
                        eventMessage.tryEmitAsNegative(resId = R.string.problem_with_updating_card)
                    }
                }
            }
        }
    }

    private fun fetchCardAndConfigureStates(cardId: Int) {
        viewModelScope.launchWithState {
            fetchCard(cardId = cardId)
                .catchWithCrashlyticsReport(crashlytics = crashlytics) {
                    eventMessage.tryEmitAsNegative(resId = R.string.problem_with_fetching_card)
                }.firstOrNull()
                ?.let { card: Card? ->
                    originalCardState.value = card

                    if (card != null) {
                        audioPlayer.preparePronunciation(word = card.foreignWord)
                        foreignWordState.value = card.foreignWord
                        letterInfosState.value = card.toLetterInfos()
                        ipaHoldersState.value = card.ipa
                        nativeWordState.value = card.nativeWord
                    }
                }
        }.onExceptionWithCrashlyticsReport(crashlytics = crashlytics) { _, throwable ->
            logW(throwable.stackTraceToString())
            eventMessage.tryEmitAsNegative(resId = R.string.problem_with_fetching_card)
        }
    }
}