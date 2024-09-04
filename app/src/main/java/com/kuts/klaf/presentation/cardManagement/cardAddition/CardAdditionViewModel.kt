package com.kuts.klaf.presentation.cardManagement.cardAddition

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.viewModelScope
import com.kuts.domain.common.CoroutineStateHolder.Companion.launchWithState
import com.kuts.domain.common.CoroutineStateHolder.Companion.onExceptionWithCrashlyticsReport
import com.kuts.domain.entities.Card
import com.kuts.domain.ipa.toRowInfos
import com.kuts.domain.repositories.CrashlyticsRepository
import com.kuts.domain.useCases.AddNewCardIntoDeckUseCase
import com.kuts.domain.useCases.FetchDeckByIdUseCase
import com.kuts.domain.useCases.FetchWordAutocompleteUseCase
import com.kuts.domain.useCases.FetchWordInfoUseCase
import com.kuts.klaf.R
import com.kuts.klaf.data.networking.CardAudioPlayer
import com.kuts.klaf.presentation.cardManagement.common.CardManagementEvent
import com.kuts.klaf.presentation.cardManagement.common.CardManagementState
import com.kuts.klaf.presentation.cardManagement.common.CardManagementViewModel
import com.kuts.klaf.presentation.common.tryEmitAsNegative
import com.kuts.klaf.presentation.common.tryEmitAsPositive
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers

class CardAdditionViewModel @AssistedInject constructor(
    @Assisted deckId: Int,
    @Assisted smartSelectedWord: String?,
    private val addNewCardIntoDeck: AddNewCardIntoDeckUseCase,
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

    init {
        handleAddingStateBySelectedWord(word = smartSelectedWord)
    }

    override fun onCardManagementConfirmed() {
        val deckId = deck.replayCache.first()?.id ?: return
        val nativeWord = cardManagementState.value.nativeWordFieldValue.text
        val foreignWord = cardManagementState.value.foreignWordFieldValue.text
        val ipaHoldersState = cardManagementState.value.ipaHolders

        if (nativeWord.isEmpty() || foreignWord.isEmpty()) {
            eventMessage.tryEmitAsNegative(resId = R.string.native_and_foreign_words_must_be_filled)
        } else {
            val newCard = Card(
                deckId = deckId,
                nativeWord = nativeWord,
                foreignWord = foreignWord,
                ipa = ipaHoldersState.map { ipaHolder -> ipaHolder.copy(ipa = ipaHolder.ipa.trim()) }
            )

            viewModelScope.launchWithState(Dispatchers.IO) {
                addNewCardIntoDeck(card = newCard)
                finishAddingState()
                audioPlayer.preparePronunciation(word = "")
                eventMessage.tryEmitAsPositive(resId = R.string.card_has_been_added)
            }.onExceptionWithCrashlyticsReport(crashlytics = crashlytics) { _, _ ->
                eventMessage.tryEmitAsNegative(resId = R.string.exception_adding_card)
            }
        }
    }

    private fun handleAddingStateBySelectedWord(word: String?) {
        val checkedWord = word ?: ""

        foreignWordFieldValueState.value = TextFieldValue(text = checkedWord)
        letterInfosState.value = word?.toRowInfos() ?: emptyList()
    }

    private fun finishAddingState() {
        sendEvent(
            event = CardManagementEvent.UpdateDataOnForeignWordChanged(
                wordFieldValue = TextFieldValue()
            )
        )
        cardManagementState.value = CardManagementState.Finished
    }
}