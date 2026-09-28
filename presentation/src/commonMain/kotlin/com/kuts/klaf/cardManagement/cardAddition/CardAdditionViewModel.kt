package com.kuts.klaf.cardManagement.cardAddition

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.viewModelScope
import com.kuts.domain.common.CoroutineStateHolder.Companion.launchWithState
import com.kuts.domain.common.CoroutineStateHolder.Companion.onExceptionWithCrashlyticsReport
import com.kuts.domain.common.ICoroutineContextProvider
import com.kuts.domain.common.ReviewedDeckCardAdditionException
import com.kuts.domain.entities.Card
import com.kuts.domain.ipa.toRowInfos
import com.kuts.domain.managers.IAudioPlayerManager
import com.kuts.domain.managers.IMnemonicGenerationBackgroundManager
import com.kuts.domain.managers.ISpeechRecognitionManager
import com.kuts.domain.managers.MnemonicGenerationSource
import com.kuts.domain.repositories.ICrashlyticsRepository
import com.kuts.domain.repositories.IMnemonicImageAssetRepository
import com.kuts.domain.useCases.AddNewCardIntoDeckUseCase
import com.kuts.domain.useCases.CheckIfCardExistsUseCase
import com.kuts.domain.useCases.FetchDeckByIdUseCase
import com.kuts.domain.useCases.FetchMnemonicAssociationUseCase
import com.kuts.domain.useCases.FetchMnemonicImageUseCase
import com.kuts.domain.useCases.FetchWordAutocompleteUseCase
import com.kuts.domain.useCases.FetchWordInfoUseCase
import com.kuts.domain.useCases.ObserveKlafServerConnectionStateUseCase
import com.kuts.klaf.presentation.resources.*
import com.kuts.klaf.cardManagement.common.ICardManagementAction
import com.kuts.klaf.cardManagement.common.ICambridgeWordDataProvider
import com.kuts.klaf.cardManagement.common.CardManagementState
import com.kuts.klaf.cardManagement.common.CardManagementViewModel
import com.kuts.klaf.cardManagement.common.toTrimmedDomainEntities
import com.kuts.klaf.common.permissions.IMicrophonePermissionManager
import com.kuts.klaf.common.tryEmitAsNegative
import com.kuts.klaf.common.tryEmitAsPositive
import org.jetbrains.compose.resources.StringResource

internal fun Throwable.additionFailureMessage(): StringResource = when (this) {
    is ReviewedDeckCardAdditionException -> Res.string.card_addition_reviewed_deck
    else -> Res.string.exception_adding_card
}

class CardAdditionViewModel(
    deckId: Int,
    smartSelectedWord: String?,
    private val addNewCardIntoDeck: AddNewCardIntoDeckUseCase,
    checkIfWordExists: CheckIfCardExistsUseCase,
    audioPlayer: IAudioPlayerManager,
    cambridgeWordDataProvider: ICambridgeWordDataProvider,
    fetchMnemonicAssociation: FetchMnemonicAssociationUseCase,
    fetchMnemonicImage: FetchMnemonicImageUseCase,
    mnemonicImageAssetRepository: IMnemonicImageAssetRepository,
    mnemonicGenerationBackgroundManager: IMnemonicGenerationBackgroundManager,
    fetchWordAutocomplete: FetchWordAutocompleteUseCase,
    fetchWordInfo: FetchWordInfoUseCase,
    speechRecognitionManager: ISpeechRecognitionManager,
    microphonePermissionManager: IMicrophonePermissionManager,
    observeKlafServerConnectionState: ObserveKlafServerConnectionStateUseCase,
    crashlytics: ICrashlyticsRepository,
    fetchDeckById: FetchDeckByIdUseCase,
    coroutineContextProvider: ICoroutineContextProvider,
) : CardManagementViewModel(
    deckId = deckId,
    audioPlayer = audioPlayer,
    cambridgeWordDataProvider = cambridgeWordDataProvider,
    fetchMnemonicAssociation = fetchMnemonicAssociation,
    fetchMnemonicImage = fetchMnemonicImage,
    mnemonicImageAssetRepository = mnemonicImageAssetRepository,
    mnemonicGenerationBackgroundManager = mnemonicGenerationBackgroundManager,
    mnemonicGenerationSource = MnemonicGenerationSource.CardCreation(
        deckId = deckId,
    ),
    fetchWordAutocomplete = fetchWordAutocomplete,
    fetchWordInfo = fetchWordInfo,
    speechRecognitionManager = speechRecognitionManager,
    microphonePermissionManager = microphonePermissionManager,
    observeKlafServerConnectionState = observeKlafServerConnectionState,
    crashlytics = crashlytics,
    fetchDeckById = fetchDeckById,
    checkIfWordExists = checkIfWordExists,
    coroutineContextProvider = coroutineContextProvider,
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
            viewModelScope.launchWithState(coroutineContextProvider.io) {
                val ipaHolders = textFieldIpaHoldersState.toTrimmedDomainEntities()
                val decksWithSameForeignWord = checkIfWordExists.invoke(foreignWord = foreignWord)

                if (decksWithSameForeignWord.isEmpty()) {
                    val preparedMnemonic = materializeCurrentMnemonicForSaving(
                        foreignWord = foreignWord,
                    )
                    val newCard = Card(
                        deckId = deckId,
                        nativeWord = nativeWord,
                        foreignWord = foreignWord,
                        ipa = ipaHolders,
                        mnemonic = preparedMnemonic.mnemonic,
                    )

                    try {
                        addNewCardIntoDeck(card = newCard)
                        finalizeMnemonicSaveSuccess(
                            retainedSavedAssetId = preparedMnemonic.retainedSavedAssetId,
                            previousSavedAssetId = null,
                        )
                        finishAddingState()
                        audioPlayer.preparePronunciation(word = "")
                        eventMessage.tryEmitAsPositive(resId = Res.string.card_has_been_added)
                    } catch (error: Throwable) {
                        rollbackPreparedMnemonicSave(preparedSave = preparedMnemonic)
                        throw error
                    }
                } else {
                    val deckNamesAsString = decksWithSameForeignWord.joinToString(", ") { it.name }

                    eventMessage.tryEmitAsNegative(
                        resId = Res.string.foreign_word_already_exists,
                        args = arrayOf(foreignWord, deckNamesAsString),
                    )
                }
            }.onExceptionWithCrashlyticsReport(crashlytics = crashlytics) { _, throwable ->
                // logE("Failed to add card\n${throwable.stackTraceToString()}")
                eventMessage.tryEmitAsNegative(resId = throwable.additionFailureMessage())
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
