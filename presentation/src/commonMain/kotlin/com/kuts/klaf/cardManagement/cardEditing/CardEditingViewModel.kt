package com.kuts.klaf.cardManagement.cardEditing

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.viewModelScope
import com.kuts.domain.common.CoroutineStateHolder.Companion.launchWithState
import com.kuts.domain.common.CoroutineStateHolder.Companion.onExceptionWithCrashlyticsReport
import com.kuts.domain.common.ICoroutineContextProvider
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
import com.kuts.klaf.cardManagement.common.ICambridgeWordDataProvider
import com.kuts.klaf.cardManagement.common.toDomainEntity
import com.kuts.klaf.cardManagement.common.toTextFieldValueIpaHolder
import com.kuts.klaf.common.tryEmitAsNegative
import com.kuts.klaf.common.tryEmitAsPositive
import com.kuts.klaf.presentation.resources.*
import com.lib.lokdroid.core.logD
import com.lib.lokdroid.core.logE
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import org.jetbrains.compose.resources.StringResource

class CardEditingViewModel(
    private val deckId: Int,
    cardId: Int,
    private val fetchCard: FetchCardUseCase,
    private val updateCard: UpdateCardUseCase,
    private val fetchWordMeaningInsights: FetchWordMeaningInsightsUseCase,
    checkIfWordExists: CheckIfCardExistsUseCase,
    audioPlayer: IAudioPlayerManager,
    cambridgeWordDataProvider: ICambridgeWordDataProvider,
    fetchWordAutocomplete: FetchWordAutocompleteUseCase,
    fetchWordInfo: FetchWordInfoUseCase,
    crashlytics: ICrashlyticsRepository,
    fetchDeckById: FetchDeckByIdUseCase,
    coroutineContextProvider: ICoroutineContextProvider,
) : CardManagementViewModel(
    deckId = deckId,
    audioPlayer = audioPlayer,
    cambridgeWordDataProvider = cambridgeWordDataProvider,
    fetchWordAutocomplete = fetchWordAutocomplete,
    fetchWordInfo = fetchWordInfo,
    crashlytics = crashlytics,
    fetchDeckById = fetchDeckById,
    checkIfWordExists = checkIfWordExists,
    coroutineContextProvider = coroutineContextProvider,
) {

    private val originalCardState = MutableStateFlow<Card?>(value = null)
    private val _insightsUiState = MutableStateFlow(CardEditingInsightsUiState())
    val insightsUiState = _insightsUiState.asStateFlow()

    init {
        logD("card editing view model created, cardId=$cardId, deckId=$deckId")
        fetchCardAndConfigureStates(cardId = cardId)
    }

    override suspend fun onForeignWordChanged(word: String) {
        val originalForeignWord = originalCardState.value?.foreignWord

        // logD("onForeignWordChanged() called. foreignWord -> $word, originalForeignWord -> $originalForeignWord")

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
            eventMessage.tryEmitAsNegative(resId = Res.string.native_and_foreign_words_must_be_filled)
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
                    eventMessage.tryEmitAsNegative(resId = Res.string.native_and_foreign_words_must_be_filled)
                }

                updatedCard == originalCard -> {
                    eventMessage.tryEmitAsNegative(resId = Res.string.card_has_not_been_changed)
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

        if (foreignWord.isEmpty()) {
            logD("refresh skipped: foreign word is blank")
            return
        }
        if (!_insightsUiState.value.canRequestRefreshedInsights) {
            logD(
                "refresh skipped: request is not allowed right now, " +
                    "status=${_insightsUiState.value.status}"
            )
            return
        }

        if (!foreignWord.isValidWordFormat()) {
            logD("refresh rejected: invalid word format, word=${foreignWord.asLogWord()}")
            setRefreshedInsightsError(errorMessageResId = Res.string.word_insights_invalid_word_format)
            return
        }

        logD("refresh started for word=${foreignWord.asLogWord()}")

        _insightsUiState.update { state ->
            state.copy(
                refreshedMeanings = emptyList(),
                refreshedErrorMessageResId = null,
                isRefreshing = true,
            )
        }

        viewModelScope.launchWithState(coroutineContextProvider.io) {
            val refreshedInsights = sanitizeInsights(insights = fetchWordMeaningInsights(word = foreignWord))
            val refreshedMeanings = refreshedInsights.meanings
            logD(
                "refresh completed for word=${foreignWord.asLogWord()}, meanings=${refreshedMeanings.size}"
            )

            _insightsUiState.update { state ->
                state.copy(
                    refreshedMeanings = refreshedMeanings,
                    refreshedErrorMessageResId = null,
                    isRefreshing = false,
                )
            }
        }.onExceptionWithCrashlyticsReport(crashlytics = crashlytics) { _, throwable ->
            logE(
                "refresh failed for word=${foreignWord.asLogWord()}\n${throwable.stackTraceToString()}"
            )
            setRefreshedInsightsError(errorMessageResId = Res.string.word_insights_request_failed)
        }
    }

    fun applyRefreshedInsights() {
        val refreshedMeanings = _insightsUiState.value.refreshedMeanings

        if (refreshedMeanings.isEmpty()) {
            logD("apply refresh skipped: there are no refreshed meanings")
            setRefreshedInsightsError(errorMessageResId = Res.string.word_insights_no_refreshed_data)
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

        logD(
            "refreshed insights applied for word=${candidateInsights.word.asLogWord()}, " +
                "meanings=${candidateInsights.meanings.size}"
        )
    }

    private fun fetchCardAndConfigureStates(cardId: Int) {
        setInsightsLoading()
        logD("card editing init: fetching cardId=$cardId, deckId=$deckId")

        viewModelScope.launchWithState {
            fetchCard(cardId = cardId)
                .catchWithCrashlyticsReport(crashlytics = crashlytics) { throwable ->
                    logE(
                        "failed to observe card flow for cardId=$cardId\n${throwable.stackTraceToString()}"
                    )
                    eventMessage.tryEmitAsNegative(resId = Res.string.problem_with_fetching_card)
                }.firstOrNull()
                ?.let { card: Card? ->
                    originalCardState.value = card

                    if (card != null) {
                        logD(
                            "card loaded: cardId=${card.id}, word=${card.foreignWord.asLogWord()}, " +
                                "storedInsights=${card.wordMeaningInsights.meanings.size}"
                        )
                        if (card.hasValidInsightsForCurrentWord()) {
                            logD(
                                "using stored insights for cardId=${card.id}, " +
                                    "word=${card.foreignWord.asLogWord()}"
                            )
                            updateInsightsUiState(insights = card.wordMeaningInsights)
                        } else {
                            logD(
                                "stored insights missing or stale for cardId=${card.id}, " +
                                    "word=${card.foreignWord.asLogWord()}"
                            )
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
                        requestAndStoreWordInsightsIfMissing(card = card)
                    } else {
                        logD("card fetch completed with null result for cardId=$cardId")
                        setInsightsIdle()
                    }
                } ?: run {
                    logD("card flow produced no first value for cardId=$cardId")
                    setInsightsIdle()
                }
        }.onExceptionWithCrashlyticsReport(crashlytics = crashlytics) { _, throwable ->
            logE(
                "failed to fetch card for editing, cardId=$cardId\n${throwable.stackTraceToString()}"
            )
            setInsightsIdle()
            eventMessage.tryEmitAsNegative(resId = Res.string.problem_with_fetching_card)
        }
    }

    private fun requestAndStoreWordInsightsIfMissing(card: Card) {
        if (card.hasValidInsightsForCurrentWord()) {
            logD(
                "auto-load skipped: cardId=${card.id} already has valid insights for " +
                    "word=${card.foreignWord.asLogWord()}"
            )
            return
        }
        val foreignWord = card.foreignWord.trim()
        if (foreignWord.isEmpty()) {
            logD("auto-load skipped: cardId=${card.id} has blank foreign word")
            setInsightsIdle()
            return
        }

        logD(
            "auto-load started for cardId=${card.id}, word=${foreignWord.asLogWord()}"
        )
        setInsightsLoading(word = foreignWord)

        viewModelScope.launchWithState(coroutineContextProvider.io) {
            if (!foreignWord.isValidWordFormat()) {
                logD(
                    "auto-load rejected: invalid word format, cardId=${card.id}, " +
                        "word=${foreignWord.asLogWord()}"
                )
                setInsightsError(
                    word = foreignWord,
                    errorMessageResId = Res.string.word_insights_invalid_word_format,
                )
                return@launchWithState
            }

            val insights = fetchWordMeaningInsights(word = foreignWord)
            logD(
                "auto-load fetched insights for cardId=${card.id}, " +
                    "word=${foreignWord.asLogWord()}, meanings=${insights.meanings.size}"
            )
            val latestCard = fetchCard(cardId = card.id).firstOrNull() ?: return@launchWithState

            if (latestCard.hasValidInsightsForCurrentWord()) {
                logD(
                    "auto-load cancelled: latest card already contains valid insights, " +
                        "cardId=${card.id}, word=${latestCard.foreignWord.asLogWord()}"
                )
                originalCardState.value = latestCard
                updateInsightsUiState(insights = latestCard.wordMeaningInsights)
                return@launchWithState
            }
            if (latestCard.foreignWord != foreignWord) {
                logD(
                    "auto-save skipped: foreign word changed, cardId=${card.id}, " +
                        "initial=${foreignWord.asLogWord()}, latest=${latestCard.foreignWord.asLogWord()}"
                )
                setInsightsIdle(word = latestCard.foreignWord)
                return@launchWithState
            }

            val updatedCard = latestCard.copy(wordMeaningInsights = insights)
            updateCard.invoke(newCard = updatedCard)
            originalCardState.value = updatedCard
            updateInsightsUiState(insights = updatedCard.wordMeaningInsights)

            logD(
                "auto-save completed for cardId=${card.id}, " +
                    "word=${foreignWord.asLogWord()}, meanings=${updatedCard.wordMeaningInsights.meanings.size}"
            )
        }.onExceptionWithCrashlyticsReport(crashlytics = crashlytics) { _, throwable ->
            logE(
                "auto-load failed for cardId=${card.id}, word=${foreignWord.asLogWord()}\n" +
                    throwable.stackTraceToString()
            )
            setInsightsError(word = foreignWord, errorMessageResId = Res.string.word_insights_request_failed)
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
                        resId = Res.string.foreign_word_already_exists,
                        args = arrayOf(foreignWord, deckNamesAsString),
                    )
                }
            }
        }.onExceptionWithCrashlyticsReport(crashlytics = crashlytics) { _, throwable ->
            // logE("Failed to update card\n${throwable.stackTraceToString()}")
            eventMessage.tryEmitAsNegative(resId = Res.string.problem_with_updating_card)
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
        eventMessage.tryEmitAsPositive(resId = Res.string.card_has_been_changed)
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
        errorMessageResId: StringResource? = null,
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
        errorMessageResId: StringResource? = null,
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

    private fun String.asLogWord(): String {
        return trim().ifEmpty { "<blank>" }
    }
}
