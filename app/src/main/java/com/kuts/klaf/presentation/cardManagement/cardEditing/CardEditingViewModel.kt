package com.kuts.klaf.presentation.cardManagement.cardEditing

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.viewModelScope
import com.kuts.domain.common.CoroutineStateHolder.Companion.launchWithState
import com.kuts.domain.common.CoroutineStateHolder.Companion.onExceptionWithCrashlyticsReport
import com.kuts.domain.common.catchWithCrashlyticsReport
import com.kuts.domain.entities.Card
import com.kuts.domain.ipa.toLetterInfos
import com.kuts.domain.repositories.CrashlyticsRepository
import com.kuts.domain.useCases.CheckIfCardExistsUseCase
import com.kuts.domain.useCases.FetchCardUseCase
import com.kuts.domain.useCases.FetchDeckByIdUseCase
import com.kuts.domain.useCases.FetchWordAutocompleteUseCase
import com.kuts.domain.useCases.FetchWordInfoUseCase
import com.kuts.domain.useCases.UpdateCardUseCase
import com.kuts.klaf.R
import com.kuts.klaf.data.networking.CardAudioPlayer
import com.kuts.klaf.presentation.cardManagement.common.CardManagementState
import com.kuts.klaf.presentation.cardManagement.common.CardManagementViewModel
import com.kuts.klaf.presentation.cardManagement.common.toDomainEntity
import com.kuts.klaf.presentation.cardManagement.common.toTextFieldValueIpaHolder
import com.kuts.klaf.presentation.common.tryEmitAsNegative
import com.kuts.klaf.presentation.common.tryEmitAsPositive
import com.lib.lokdroid.core.logD
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
    checkIfWordExists: CheckIfCardExistsUseCase,
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
    checkIfWordExists = checkIfWordExists,
) {

    companion object {

        const val DECK_ARGUMENT_NAME = "deck_id"
        const val CARD_ARGUMENT_NAME = "card_id"
    }

    private val originalCardState = MutableStateFlow<Card?>(value = null)

    init {
        fetchCardAndConfigureStates(cardId = cardId)
    }

    override suspend fun onForeignWordChanged(word: String) {
        val originalForeignWord = originalCardState.value?.foreignWord

        logD("onForeignWordChanged() called. foreignWord -> $word, originalForeignWord -> $originalForeignWord")

        if (word.isNotEmpty() && originalForeignWord != word) {
            checkIfForeignWordExists(word = word)
        }
    }

    override fun onCardManagementConfirmed() {
        val originalCard = originalCardState.value ?: return
        val deckId = deck.replayCache.first()?.id ?: return
        val nativeWord = cardManagementState.value.nativeWordFieldValue.text
        val foreignWord = cardManagementState.value.foreignWordFieldValue.text
        val trimmedTextFieldValueIpaHoldersState = cardManagementState.value
            .textFieldValueIpaHolders.map { textFieldValueIpaHolder ->
                val trimmedText = textFieldValueIpaHolder.ipaTextFieldValue.text.trim()
                val trimmedTextFieldValue =
                    textFieldValueIpaHolder.ipaTextFieldValue.copy(text = trimmedText)

                textFieldValueIpaHolder.copy(ipaTextFieldValue = trimmedTextFieldValue)
            }

        if (nativeWord.isEmpty() || foreignWord.isEmpty()) {
            eventMessage.tryEmitAsNegative(resId = R.string.native_and_foreign_words_must_be_filled)
        } else {
            val updatedCard = originalCard.copy(
                deckId = deckId,
                nativeWord = nativeWord,
                foreignWord = foreignWord,
                ipa = trimmedTextFieldValueIpaHoldersState.map { it.toDomainEntity() },
            )

            when {
                updatedCard.nativeWord.isEmpty() || updatedCard.foreignWord.isEmpty() -> {
                    eventMessage.tryEmitAsNegative(resId = R.string.native_and_foreign_words_must_be_filled)
                }

                updatedCard == originalCard -> {
                    eventMessage.tryEmitAsNegative(resId = R.string.card_has_not_been_changed)
                }

                else -> {
                    manageUpdatingCard(
                        originalCard = originalCard,
                        updatedCard = updatedCard,
                        foreignWord = foreignWord
                    )
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
                        foreignWordFieldValueState.value =
                            TextFieldValue(text = card.foreignWord)
                        letterInfosState.value = card.toLetterInfos()
                        textFieldValueIpaHoldersState.value = card.ipa.map {
                            it.toTextFieldValueIpaHolder()
                        }
                        nativeWordFieldValueState.value = TextFieldValue(text = card.nativeWord)
                    }
                }
        }.onExceptionWithCrashlyticsReport(crashlytics = crashlytics) { _, throwable ->
            logW(throwable.stackTraceToString())
            eventMessage.tryEmitAsNegative(resId = R.string.problem_with_fetching_card)
        }
    }

    private fun manageUpdatingCard(
        originalCard: Card,
        updatedCard: Card,
        foreignWord: String,
    ) {
        viewModelScope.launchWithState {
            if (updatedCard.foreignWord == originalCard.foreignWord) {
                performUpdatingCard(updatedCard = updatedCard)
            } else {
                val decksWithSameForeignWord = checkIfWordExists.invoke(
                    foreignWord = foreignWord
                )

                if (decksWithSameForeignWord.isEmpty()) {
                    performUpdatingCard(updatedCard = updatedCard)
                } else {
                    val deckNamesAsString =
                        decksWithSameForeignWord.joinToString(", ") { it.name }

                    eventMessage.tryEmitAsNegative(
                        resId = R.string.foreign_word_already_exists,
                        args = arrayOf(foreignWord, deckNamesAsString),
                    )
                }
            }
        }.onExceptionWithCrashlyticsReport(crashlytics = crashlytics) { _, _ ->
            eventMessage.tryEmitAsNegative(resId = R.string.problem_with_updating_card)
        }
    }

    private suspend fun performUpdatingCard(updatedCard: Card) {
        updateCard.invoke(newCard = updatedCard)
        eventMessage.tryEmitAsPositive(resId = R.string.card_has_been_changed)
        cardManagementState.value = CardManagementState.Finished
    }
}