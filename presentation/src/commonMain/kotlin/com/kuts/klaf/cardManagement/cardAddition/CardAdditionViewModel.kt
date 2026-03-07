package com.kuts.klaf.cardManagement.cardAddition

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.viewModelScope
import com.kuts.domain.common.CoroutineStateHolder.Companion.launchWithState
import com.kuts.domain.common.CoroutineStateHolder.Companion.onExceptionWithCrashlyticsReport
import com.kuts.domain.entities.Card
import com.kuts.domain.ipa.toRowInfos
import com.kuts.domain.managers.IAudioPlayerManager
import com.kuts.domain.repositories.ICrashlyticsRepository
import com.kuts.domain.useCases.AddNewCardIntoDeckUseCase
import com.kuts.domain.useCases.CheckIfCardExistsUseCase
import com.kuts.domain.useCases.FetchDeckByIdUseCase
import com.kuts.domain.useCases.FetchWordAutocompleteUseCase
import com.kuts.domain.useCases.FetchWordInfoUseCase
import com.kuts.klaf.presentation.resources.*
import com.kuts.klaf.cardManagement.common.ICardManagementAction
import com.kuts.klaf.cardManagement.common.ICambridgeWordDataProvider
import com.kuts.klaf.cardManagement.common.CardManagementState
import com.kuts.klaf.cardManagement.common.CardManagementViewModel
import com.kuts.klaf.cardManagement.common.toDomainEntity
import com.kuts.klaf.common.tryEmitAsNegative
import com.kuts.klaf.common.tryEmitAsPositive
import kotlinx.coroutines.Dispatchers

class CardAdditionViewModel(
    deckId: Int,
    smartSelectedWord: String?,
    private val addNewCardIntoDeck: AddNewCardIntoDeckUseCase,
    checkIfWordExists: CheckIfCardExistsUseCase,
    audioPlayer: IAudioPlayerManager,
    cambridgeWordDataProvider: ICambridgeWordDataProvider,
    fetchWordAutocomplete: FetchWordAutocompleteUseCase,
    fetchWordInfo: FetchWordInfoUseCase,
    crashlytics: ICrashlyticsRepository,
    fetchDeckById: FetchDeckByIdUseCase,
) : CardManagementViewModel(
    deckId = deckId,
    audioPlayer = audioPlayer,
    cambridgeWordDataProvider = cambridgeWordDataProvider,
    fetchWordAutocomplete = fetchWordAutocomplete,
    fetchWordInfo = fetchWordInfo,
    crashlytics = crashlytics,
    fetchDeckById = fetchDeckById,
    checkIfWordExists = checkIfWordExists
) {

    init {
        handleAddingStateBySelectedWord(word = smartSelectedWord)
    }

    override suspend fun onForeignWordChanged(word: String) {
        super.onForeignWordChanged(word = word)
        // logD("onForeignWordChanged() called. foreignWord -> $word")

        if (word.isNotEmpty()) {
            checkIfForeignWordExists(word = word)
        }
    }

    override fun onCardManagementConfirmed() {
        val deckId = deck.replayCache.first()?.id ?: return
        val nativeWord = cardManagementState.value.nativeWordFieldValue.text
        val foreignWord = cardManagementState.value.foreignWordFieldValue.text
        val textFieldIpaHoldersState = cardManagementState.value.textFieldValueIpaHolders

        if (nativeWord.isEmpty() || foreignWord.isEmpty()) {
            eventMessage.tryEmitAsNegative(resId = Res.string.native_and_foreign_words_must_be_filled)
        } else {
            val ipaHolders = textFieldIpaHoldersState.map { textFieldValueIpaHolder ->
                textFieldValueIpaHolder.toDomainEntity()
                    .copy(ipa = textFieldValueIpaHolder.ipaTextFieldValue.text.trim())
            }
            val newCard = Card(
                deckId = deckId,
                nativeWord = nativeWord,
                foreignWord = foreignWord,
                ipa = ipaHolders
            )

            viewModelScope.launchWithState(Dispatchers.IO) {
                val decksWithSameForeignWord = checkIfWordExists.invoke(foreignWord = foreignWord)

                if (decksWithSameForeignWord.isEmpty()) {
                    addNewCardIntoDeck(card = newCard)
                    finishAddingState()
                    audioPlayer.preparePronunciation(word = "")
                    eventMessage.tryEmitAsPositive(resId = Res.string.card_has_been_added)
                } else {
                    val deckNamesAsString = decksWithSameForeignWord.joinToString(", ") { it.name }

                    eventMessage.tryEmitAsNegative(
                        resId = Res.string.foreign_word_already_exists,
                        args = arrayOf(foreignWord, deckNamesAsString),
                    )
                }
            }.onExceptionWithCrashlyticsReport(crashlytics = crashlytics) { _, throwable ->
                // logE("Failed to add card\n${throwable.stackTraceToString()}")
                eventMessage.tryEmitAsNegative(resId = Res.string.exception_adding_card)
            }
        }
    }

    private fun handleAddingStateBySelectedWord(word: String?) {
        val checkedWord = word?.trim()?.lowercase() ?: ""

        foreignWordFieldValueState.value = TextFieldValue(text = checkedWord)
        letterInfosState.value = checkedWord.toRowInfos()
    }

    private fun finishAddingState() {
        sendAction(
            action = ICardManagementAction.UpdateDataOnForeignWordChanged(
                wordFieldValue = TextFieldValue()
            )
        )
        cardManagementState.value = CardManagementState.Finished
    }
}
