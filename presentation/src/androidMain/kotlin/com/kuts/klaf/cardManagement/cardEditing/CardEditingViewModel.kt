package com.kuts.klaf.cardManagement.cardEditing

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.viewModelScope
import com.cambridge.dictionary.client.CambridgeClient
import com.kuts.domain.common.CoroutineStateHolder.Companion.launchWithState
import com.kuts.domain.common.CoroutineStateHolder.Companion.onExceptionWithCrashlyticsReport
import com.kuts.domain.common.catchWithCrashlyticsReport
import com.kuts.domain.entities.Card
import com.kuts.domain.entities.WordMeaningInsights
import com.kuts.domain.ipa.toLetterInfos
import com.kuts.domain.managers.IAudioPlayerManager
import com.kuts.domain.repositories.ICrashlyticsRepository
import com.kuts.domain.useCases.CheckIfCardExistsUseCase
import com.kuts.domain.useCases.FetchCardUseCase
import com.kuts.domain.useCases.FetchDeckByIdUseCase
import com.kuts.domain.useCases.FetchWordAutocompleteUseCase
import com.kuts.domain.useCases.FetchWordInfoUseCase
import com.kuts.domain.useCases.FetchWordMeaningInsightsUseCase
import com.kuts.domain.useCases.UpdateCardUseCase
import com.kuts.klaf.presentation.R
import com.kuts.klaf.cardManagement.common.CardManagementState
import com.kuts.klaf.cardManagement.common.CardManagementViewModel
import com.kuts.klaf.cardManagement.common.toDomainEntity
import com.kuts.klaf.cardManagement.common.toTextFieldValueIpaHolder
import com.kuts.klaf.common.tryEmitAsNegative
import com.kuts.klaf.common.tryEmitAsPositive
import com.lib.lokdroid.core.logD
import com.lib.lokdroid.core.logW
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class CardEditingViewModel(
    private val deckId: Int,
    cardId: Int,
    private val fetchCard: FetchCardUseCase,
    private val updateCard: UpdateCardUseCase,
    private val fetchWordMeaningInsights: FetchWordMeaningInsightsUseCase,
    checkIfWordExists: CheckIfCardExistsUseCase,
    audioPlayer: IAudioPlayerManager,
    cambridgeClient: CambridgeClient,
    fetchWordAutocomplete: FetchWordAutocompleteUseCase,
    fetchWordInfo: FetchWordInfoUseCase,
    crashlytics: ICrashlyticsRepository,
    fetchDeckById: FetchDeckByIdUseCase,
) : CardManagementViewModel(
    deckId = deckId,
    audioPlayer = audioPlayer,
    cambridgeClient = cambridgeClient,
    fetchWordAutocomplete = fetchWordAutocomplete,
    fetchWordInfo = fetchWordInfo,
    crashlytics = crashlytics,
    fetchDeckById = fetchDeckById,
    checkIfWordExists = checkIfWordExists,
) {
    companion object {

        private const val DEFAULT_INSIGHTS_ERROR_MESSAGE = "Word insights request failed."
        private const val INVALID_WORD_FORMAT_ERROR_MESSAGE =
            "The word format is invalid. Please enter a real English word."
    }

    private val json = Json {
        prettyPrint = true
        explicitNulls = false
    }

    private val originalCardState = MutableStateFlow<Card?>(value = null)
    private val _insightsUiState = MutableStateFlow(CardEditingInsightsUiState())
    val insightsUiState = _insightsUiState.asStateFlow()

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
            val isForeignWordChanged = foreignWord != originalCard.foreignWord
            val updatedCard = originalCard.copy(
                deckId = deckId,
                nativeWord = nativeWord,
                foreignWord = foreignWord,
                ipa = trimmedTextFieldValueIpaHoldersState.map { it.toDomainEntity() },
                wordMeaningInsights = if (isForeignWordChanged) {
                    WordMeaningInsights.EMPTY
                } else {
                    originalCard.wordMeaningInsights
                },
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

    override fun onGeminiInsightsClicked() {
        val foreignWord = cardManagementState.value.foreignWordFieldValue.text.trim()

        if (foreignWord.isEmpty()) {
            eventMessage.tryEmitAsNegative(resId = R.string.native_and_foreign_words_must_be_filled)
            return
        }

        viewModelScope.launchWithState(Dispatchers.IO) {
            val insights = fetchWordMeaningInsights(word = foreignWord)
            logD(
                "Gemini insights for \"$foreignWord\":\n" +
                        json.encodeToString(insights)
            )
        }.onExceptionWithCrashlyticsReport(crashlytics = crashlytics) { _, throwable ->
            logW(throwable.stackTraceToString())
            eventMessage.tryEmitAsNegative(resId = R.string.word_info_retrieving_common_warning_message)
        }
    }

    fun showInsightsSheet() {
        if (_insightsUiState.value.isExpandable) {
            _insightsUiState.update { state -> state.copy(isSheetVisible = true) }
        }
    }

    fun hideInsightsSheet() {
        _insightsUiState.update { state -> state.copy(isSheetVisible = false) }
    }

    private fun fetchCardAndConfigureStates(cardId: Int) {
        setInsightsLoading()

        viewModelScope.launchWithState {
            fetchCard(cardId = cardId)
                .catchWithCrashlyticsReport(crashlytics = crashlytics) {
                    eventMessage.tryEmitAsNegative(resId = R.string.problem_with_fetching_card)
                }.firstOrNull()
                ?.let { card: Card? ->
                    originalCardState.value = card

                    if (card != null) {
                        if (card.hasValidInsightsForCurrentWord()) {
                            updateInsightsUiState(insights = card.wordMeaningInsights)
                        } else {
                            setInsightsIdle(word = card.foreignWord)
                        }
                        audioPlayer.preparePronunciation(word = card.foreignWord)
                        foreignWordFieldValueState.value =
                            TextFieldValue(text = card.foreignWord)
                        letterInfosState.value = card.toLetterInfos()
                        textFieldValueIpaHoldersState.value = card.ipa.map {
                            it.toTextFieldValueIpaHolder()
                        }
                        nativeWordFieldValueState.value = TextFieldValue(text = card.nativeWord)
                        requestAndStoreGeminiInsightsIfMissing(card = card)
                    } else {
                        setInsightsIdle()
                    }
                } ?: setInsightsIdle()
        }.onExceptionWithCrashlyticsReport(crashlytics = crashlytics) { _, throwable ->
            logW(throwable.stackTraceToString())
            setInsightsIdle()
            eventMessage.tryEmitAsNegative(resId = R.string.problem_with_fetching_card)
        }
    }

    private fun requestAndStoreGeminiInsightsIfMissing(card: Card) {
        if (card.hasValidInsightsForCurrentWord()) return
        val foreignWord = card.foreignWord.trim()
        if (foreignWord.isEmpty()) {
            setInsightsIdle()
            return
        }
        setInsightsLoading(word = foreignWord)

        viewModelScope.launchWithState(Dispatchers.IO) {
            if (!foreignWord.isValidWordFormat()) {
                setInsightsError(
                    word = foreignWord,
                    errorMessage = INVALID_WORD_FORMAT_ERROR_MESSAGE,
                )
                return@launchWithState
            }

            val insights = fetchWordMeaningInsights(word = foreignWord)
            val latestCard = fetchCard(cardId = card.id).firstOrNull() ?: return@launchWithState

            if (latestCard.hasValidInsightsForCurrentWord()) {
                originalCardState.value = latestCard
                updateInsightsUiState(insights = latestCard.wordMeaningInsights)
                return@launchWithState
            }
            if (latestCard.foreignWord != foreignWord) {
                logD(
                    "Gemini insights skipping auto-save because foreign word changed. " +
                        "initial=$foreignWord, latest=${latestCard.foreignWord}"
                )
                setInsightsIdle(word = latestCard.foreignWord)
                return@launchWithState
            }

            val updatedCard = latestCard.copy(wordMeaningInsights = insights)
            updateCard.invoke(newCard = updatedCard)
            originalCardState.value = updatedCard
            updateInsightsUiState(insights = updatedCard.wordMeaningInsights)

            logD("Gemini insights were auto-saved for cardId=${card.id}, foreignWord=$foreignWord")
        }.onExceptionWithCrashlyticsReport(crashlytics = crashlytics) { _, throwable ->
            logW(throwable.stackTraceToString())
            setInsightsError(word = foreignWord, throwable = throwable)
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

    private fun updateInsightsUiState(insights: WordMeaningInsights) {
        val sanitizedMeanings = insights.meanings
            .sortedBy { meaning -> meaning.frequencyRank }
            .map { meaning ->
                meaning.copy(
                    translation = meaning.translation.trim(),
                    context = meaning.context.trim(),
                    examples = meaning.examples
                        .map { example -> example.trim() }
                        .filter { example -> example.isNotEmpty() }
                        .distinct(),
                )
            }
            .filter { meaning -> meaning.translation.isNotEmpty() }
        val hasSanitizedMeanings = sanitizedMeanings.isNotEmpty()
        val sanitizedWord = insights.word.trim()

        _insightsUiState.update { state ->
            state.copy(
                word = sanitizedWord,
                meanings = sanitizedMeanings,
                status = if (hasSanitizedMeanings) {
                    CardEditingInsightsStatus.Success
                } else {
                    CardEditingInsightsStatus.Idle
                },
                errorMessage = "",
                isSheetVisible = state.isSheetVisible && hasSanitizedMeanings,
            )
        }
    }

    private fun setInsightsLoading(word: String = "") {
        _insightsUiState.update { state ->
            state.copy(
                word = word.trim().ifEmpty { state.word },
                status = CardEditingInsightsStatus.Loading,
                errorMessage = "",
                isSheetVisible = false,
            )
        }
    }

    private fun setInsightsIdle(word: String = "") {
        _insightsUiState.update { state ->
            state.copy(
                word = word.trim().ifEmpty { state.word },
                meanings = emptyList(),
                status = CardEditingInsightsStatus.Idle,
                errorMessage = "",
                isSheetVisible = false,
            )
        }
    }

    private fun setInsightsError(word: String, throwable: Throwable) {
        setInsightsError(
            word = word,
            errorMessage = throwable.toInsightsErrorMessage(),
        )
    }

    private fun setInsightsError(word: String, errorMessage: String) {
        _insightsUiState.update { state ->
            state.copy(
                word = word.trim().ifEmpty { state.word },
                meanings = emptyList(),
                status = CardEditingInsightsStatus.Error,
                errorMessage = errorMessage,
                isSheetVisible = false,
            )
        }
    }

    private fun Throwable.toInsightsErrorMessage(): String {
        val parsedMessage = message
            .orEmpty()
            .lineSequence()
            .firstOrNull()
            .orEmpty()
            .trim()

        return parsedMessage.ifEmpty { DEFAULT_INSIGHTS_ERROR_MESSAGE }
    }

    private fun Card.hasValidInsightsForCurrentWord(): Boolean {
        return wordMeaningInsights.isValidForWord(word = foreignWord)
    }

    private fun WordMeaningInsights.isValidForWord(word: String): Boolean {
        if (!hasData()) return false
        return this.word.toWordKey() == word.toWordKey()
    }

    private fun String.toWordKey(): String {
        return trim().lowercase()
    }

    private fun String.isValidWordFormat(): Boolean {
        val trimmedWord = trim()
        if (trimmedWord.isEmpty()) return false

        val hasOnlyAllowedCharacters = trimmedWord.all { symbol ->
            symbol.isLetter() || symbol == '\'' || symbol == '-'
        }
        if (!hasOnlyAllowedCharacters) return false

        val lettersOnly = trimmedWord.filter { symbol -> symbol.isLetter() }.lowercase()
        if (lettersOnly.isEmpty()) return false
        if (lettersOnly.length >= 4 && lettersOnly.toSet().size == 1) return false

        return true
    }
}
