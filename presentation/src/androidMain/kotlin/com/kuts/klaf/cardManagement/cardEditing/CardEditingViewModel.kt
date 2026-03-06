package com.kuts.klaf.cardManagement.cardEditing

import androidx.annotation.StringRes
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
import com.kuts.klaf.cardManagement.common.CardManagementState
import com.kuts.klaf.cardManagement.common.CardManagementViewModel
import com.kuts.klaf.cardManagement.common.toDomainEntity
import com.kuts.klaf.cardManagement.common.toTextFieldValueIpaHolder
import com.kuts.klaf.common.tryEmitAsNegative
import com.kuts.klaf.common.tryEmitAsPositive
import com.kuts.klaf.presentation.R
import com.lib.lokdroid.core.logD
import com.lib.lokdroid.core.logE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update

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
            val insightsForSaving = resolveInsightsForSaving(
                originalCard = originalCard,
                foreignWord = foreignWord,
                isForeignWordChanged = isForeignWordChanged,
            )
            val updatedCard = originalCard.copy(
                deckId = deckId,
                nativeWord = nativeWord,
                foreignWord = foreignWord,
                ipa = trimmedTextFieldValueIpaHoldersState.map { it.toDomainEntity() },
                wordMeaningInsights = insightsForSaving,
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

    fun showInsightsSheet() {
        if (_insightsUiState.value.isExpandable) {
            _insightsUiState.update { state -> state.copy(isSheetVisible = true) }
        }
    }

    fun hideInsightsSheet() {
        _insightsUiState.update { state -> state.copy(isSheetVisible = false) }
    }

    fun requestRefreshedInsights() {
        val foreignWord = cardManagementState.value.foreignWordFieldValue.text.trim()

        if (foreignWord.isEmpty()) return
        if (!_insightsUiState.value.canRequestRefreshedInsights) return

        if (!foreignWord.isValidWordFormat()) {
            setRefreshedInsightsError(errorMessageResId = R.string.word_insights_invalid_word_format)
            return
        }

        _insightsUiState.update { state ->
            state.copy(
                refreshedMeanings = emptyList(),
                refreshedErrorMessageResId = null,
                isRefreshing = true,
            )
        }

        viewModelScope.launchWithState(Dispatchers.IO) {
            val refreshedInsights = sanitizeInsights(insights = fetchWordMeaningInsights(word = foreignWord))
            val refreshedMeanings = refreshedInsights.meanings

            _insightsUiState.update { state ->
                state.copy(
                    refreshedMeanings = refreshedMeanings,
                    refreshedErrorMessageResId = null,
                    isRefreshing = false,
                )
            }
        }.onExceptionWithCrashlyticsReport(crashlytics = crashlytics) { _, throwable ->
            logE("Failed to refresh insights\n${throwable.stackTraceToString()}")
            setRefreshedInsightsError(errorMessageResId = R.string.word_insights_request_failed)
        }
    }

    fun applyRefreshedInsights() {
        val refreshedMeanings = _insightsUiState.value.refreshedMeanings

        if (refreshedMeanings.isEmpty()) {
            setRefreshedInsightsError(errorMessageResId = R.string.word_insights_no_refreshed_data)
            return
        }

        val currentForeignWord = cardManagementState.value.foreignWordFieldValue.text.trim()
        val candidateInsights = sanitizeInsights(
            insights = WordMeaningInsights(
                word = currentForeignWord.ifBlank { _insightsUiState.value.word },
                meanings = refreshedMeanings,
            ),
        )

        _insightsUiState.update { state ->
            state.copy(
                word = candidateInsights.word.ifBlank { state.word },
                meanings = candidateInsights.meanings,
                status = if (candidateInsights.meanings.isNotEmpty()) {
                    CardEditingInsightsStatus.Success
                } else {
                    CardEditingInsightsStatus.Idle
                },
                refreshedMeanings = emptyList(),
                refreshedErrorMessageResId = null,
                isRefreshing = false,
                isApplyingRefreshed = false,
                isSheetVisible = state.isSheetVisible && candidateInsights.meanings.isNotEmpty(),
            )
        }
    }

    private fun fetchCardAndConfigureStates(cardId: Int) {
        setInsightsLoading()

        viewModelScope.launchWithState {
            fetchCard(cardId = cardId)
                .catchWithCrashlyticsReport(crashlytics = crashlytics) { throwable ->
                    logE("Failed to fetch card flow for editing\n${throwable.stackTraceToString()}")
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
            logE("Failed to fetch card for editing\n${throwable.stackTraceToString()}")
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
                    errorMessageResId = R.string.word_insights_invalid_word_format,
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
            logE("Failed to auto-load Gemini insights\n${throwable.stackTraceToString()}")
            setInsightsError(word = foreignWord, errorMessageResId = R.string.word_insights_request_failed)
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
        }.onExceptionWithCrashlyticsReport(crashlytics = crashlytics) { _, throwable ->
            logE("Failed to update card\n${throwable.stackTraceToString()}")
            eventMessage.tryEmitAsNegative(resId = R.string.problem_with_updating_card)
        }
    }

    private fun resolveInsightsForSaving(
        originalCard: Card,
        foreignWord: String,
        isForeignWordChanged: Boolean,
    ): WordMeaningInsights {
        val uiState = _insightsUiState.value
        val candidateInsights = sanitizeInsights(
            insights = WordMeaningInsights(
                word = uiState.word.ifBlank { foreignWord.trim() },
                meanings = uiState.meanings,
            ),
        )
        val isCandidateForCurrentWord =
            candidateInsights.hasData() && candidateInsights.word.toWordKey() == foreignWord.toWordKey()

        if (isForeignWordChanged && !isCandidateForCurrentWord) {
            return WordMeaningInsights.EMPTY
        }
        if (isCandidateForCurrentWord) {
            return candidateInsights
        }
        return originalCard.wordMeaningInsights
    }

    private suspend fun performUpdatingCard(updatedCard: Card) {
        updateCard.invoke(newCard = updatedCard)
        eventMessage.tryEmitAsPositive(resId = R.string.card_has_been_changed)
        cardManagementState.value = CardManagementState.Finished
    }

    private fun updateInsightsUiState(insights: WordMeaningInsights) {
        val sanitizedInsights = sanitizeInsights(insights = insights)
        val hasSanitizedMeanings = sanitizedInsights.meanings.isNotEmpty()

        _insightsUiState.update { state ->
            state.copy(
                word = sanitizedInsights.word.trim(),
                meanings = sanitizedInsights.meanings,
                status = if (hasSanitizedMeanings) {
                    CardEditingInsightsStatus.Success
                } else {
                    CardEditingInsightsStatus.Idle
                },
                errorMessageResId = null,
                refreshedMeanings = emptyList(),
                refreshedErrorMessageResId = null,
                isRefreshing = false,
                isApplyingRefreshed = false,
                isSheetVisible = state.isSheetVisible && hasSanitizedMeanings,
            )
        }
    }

    private fun sanitizeInsights(insights: WordMeaningInsights): WordMeaningInsights {
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

        return insights.copy(
            word = insights.word.trim(),
            meanings = sanitizedMeanings,
        )
    }

    private fun setInsightsLoading(word: String = "") {
        _insightsUiState.update { state ->
            state.copy(
                word = word.trim().ifEmpty { state.word },
                status = CardEditingInsightsStatus.Loading,
                errorMessageResId = null,
                refreshedMeanings = emptyList(),
                refreshedErrorMessageResId = null,
                isRefreshing = false,
                isApplyingRefreshed = false,
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
                errorMessageResId = null,
                refreshedMeanings = emptyList(),
                refreshedErrorMessageResId = null,
                isRefreshing = false,
                isApplyingRefreshed = false,
                isSheetVisible = false,
            )
        }
    }

    private fun setInsightsError(
        word: String,
        @StringRes errorMessageResId: Int? = null,
    ) {
        _insightsUiState.update { state ->
            state.copy(
                word = word.trim().ifEmpty { state.word },
                meanings = emptyList(),
                status = CardEditingInsightsStatus.Error,
                errorMessageResId = errorMessageResId,
                refreshedMeanings = emptyList(),
                refreshedErrorMessageResId = null,
                isRefreshing = false,
                isApplyingRefreshed = false,
                isSheetVisible = false,
            )
        }
    }

    private fun setRefreshedInsightsError(
        @StringRes errorMessageResId: Int? = null,
    ) {
        _insightsUiState.update { state ->
            state.copy(
                refreshedMeanings = emptyList(),
                refreshedErrorMessageResId = errorMessageResId,
                isRefreshing = false,
                isApplyingRefreshed = false,
            )
        }
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
