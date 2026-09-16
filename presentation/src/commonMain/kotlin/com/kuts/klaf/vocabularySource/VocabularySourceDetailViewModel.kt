package com.kuts.klaf.vocabularySource

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuts.domain.common.getCurrentDateAsLong
import com.kuts.domain.entities.Deck
import com.kuts.domain.entities.VocabularySource
import com.kuts.domain.entities.VocabularySourceItem
import com.kuts.domain.entities.VocabularySourceItemStatus
import com.kuts.domain.managers.IVocabularySourceAnalysisBackgroundManager
import com.kuts.domain.managers.VocabularySourceAnalysisHandle
import com.kuts.domain.managers.VocabularySourceAnalysisOutcome
import com.kuts.domain.useCases.AddVocabularySourceItemsToDeckUseCase
import com.kuts.domain.useCases.AnalyzeVocabularySourceTextUseCase
import com.kuts.domain.useCases.FetchAllCardsUseCase
import com.kuts.domain.useCases.FetchDeckSourceUseCase
import com.kuts.domain.useCases.ObserveVocabularySourceByIdUseCase
import com.kuts.domain.useCases.ObserveVocabularySourceItemsUseCase
import com.kuts.domain.useCases.ReplaceVocabularySourceDraftItemsUseCase
import com.kuts.domain.useCases.SaveVocabularySourceItemsUseCase
import com.kuts.domain.useCases.UpdateVocabularySourceUseCase
import com.kuts.domain.vocabularySource.TranscriptTextCleaner
import com.kuts.domain.vocabularySource.TranscriptTextValidationResult
import com.kuts.domain.vocabularySource.VocabularySourceItemCategorizer
import com.kuts.klaf.common.EventMessage
import com.kuts.klaf.common.IEventMessageSource
import com.kuts.klaf.presentation.resources.Res
import com.kuts.klaf.presentation.resources.problem_with_saving_vocabulary_source
import com.kuts.klaf.presentation.resources.vocabulary_source_analysis_failed
import com.kuts.klaf.presentation.resources.vocabulary_source_analysis_finished
import com.kuts.klaf.presentation.resources.vocabulary_source_items_added
import com.kuts.klaf.presentation.resources.vocabulary_source_no_items_to_add
import com.kuts.klaf.presentation.resources.vocabulary_source_saved
import com.kuts.klaf.presentation.resources.vocabulary_source_save_before_adding
import com.kuts.klaf.presentation.resources.vocabulary_source_text_too_long
import com.kuts.klaf.presentation.resources.vocabulary_source_title_empty
import com.lib.lokdroid.core.logD
import com.lib.lokdroid.core.logE
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VocabularySourceDetailState(
    val source: VocabularySource? = null,
    val title: String = "",
    val description: String = "",
    val rawText: String = "",
    val cleanText: String = "",
    val persistedItems: List<VocabularySourceItem> = emptyList(),
    val draftItems: List<VocabularySourceItem> = emptyList(),
    val decks: List<Deck> = emptyList(),
    val selectedItemIndexes: Set<Int> = emptySet(),
    val itemEditState: VocabularySourceItemEditState? = null,
    val isDeckChooserVisible: Boolean = false,
    val isNewDeckCreationVisible: Boolean = false,
    val newDeckName: String = "",
    val isAddingToDeck: Boolean = false,
    val isAnalyzing: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
    val isSaveConfirmed: Boolean = false,
    val hasStaleItems: Boolean = false,
    val hasNewAnalysisDraft: Boolean = false,
) {

    val displayedItems: List<VocabularySourceItem>
        get() = draftItems.ifEmpty { persistedItems }

    val itemHolders: List<SelectableVocabularySourceItemHolder>
        get() = displayedItems.mapIndexed { index, item ->
            SelectableVocabularySourceItemHolder(
                item = item,
                isSelected = index in selectedItemIndexes,
            )
        }

    val selectedItemCount: Int
        get() = selectedItemIndexes.size

    val selectedItemsForAdding: List<VocabularySourceItem>
        get() = selectedItemIndexes
            .sorted()
            .mapNotNull { index -> displayedItems.getOrNull(index) }
            .filter { item -> item.status == VocabularySourceItemStatus.PENDING && !item.alreadyExists }

    val isAllItemsSelected: Boolean
        get() = displayedItems.isNotEmpty() && displayedItems.size == selectedItemIndexes.size
}

data class SelectableVocabularySourceItemHolder(
    val item: VocabularySourceItem,
    val isSelected: Boolean = false,
)

data class VocabularySourceItemEditState(
    val index: Int,
    val foreignWord: String,
    val nativeWord: String,
)

class VocabularySourceDetailViewModel(
    private val sourceId: Int,
    observeVocabularySourceById: ObserveVocabularySourceByIdUseCase,
    observeVocabularySourceItems: ObserveVocabularySourceItemsUseCase,
    private val updateVocabularySource: UpdateVocabularySourceUseCase,
    private val saveVocabularySourceItems: SaveVocabularySourceItemsUseCase,
    private val replaceVocabularySourceDraftItems: ReplaceVocabularySourceDraftItemsUseCase,
    private val analyzeVocabularySourceText: AnalyzeVocabularySourceTextUseCase,
    private val fetchAllCards: FetchAllCardsUseCase,
    fetchDeckSource: FetchDeckSourceUseCase,
    private val addVocabularySourceItemsToDeck: AddVocabularySourceItemsToDeckUseCase,
    private val vocabularySourceAnalysisBackgroundManager: IVocabularySourceAnalysisBackgroundManager,
) : ViewModel(), IEventMessageSource {

    override val eventMessage = MutableSharedFlow<EventMessage>(extraBufferCapacity = 1)

    private val cleaner = TranscriptTextCleaner()
    private val categorizer = VocabularySourceItemCategorizer()
    private val editableState = MutableStateFlow(VocabularySourceDetailState())
    private val decks = fetchDeckSource()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList(),
        )

    val state: StateFlow<VocabularySourceDetailState> = combine(
        editableState,
        observeVocabularySourceById(sourceId = sourceId).filterNotNull(),
        observeVocabularySourceItems(sourceId = sourceId),
        decks,
    ) { editableState, source, items, decks ->
        val shouldApplySource = editableState.source?.id != source.id && !editableState.hasUnsavedChanges

        if (shouldApplySource) {
            editableState.copy(
                source = source,
                title = source.title,
                description = source.description,
                rawText = source.rawText,
                cleanText = source.cleanText,
                persistedItems = items,
                decks = decks,
            )
        } else {
            editableState.copy(
                source = source,
                persistedItems = items,
                decks = decks,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = VocabularySourceDetailState(),
    )

    fun onTitleChanged(title: String) {
        editableState.value = state.value.copy(
            title = title,
            hasUnsavedChanges = true,
            isSaveConfirmed = false,
        )
    }

    fun onDescriptionChanged(description: String) {
        editableState.value = state.value.copy(
            description = description,
            hasUnsavedChanges = true,
            isSaveConfirmed = false,
        )
    }

    fun onRawTextChanged(rawText: String) {
        when (val validation = cleaner.validateRawText(rawText = rawText)) {
            TranscriptTextValidationResult.Valid -> {
                val currentState = state.value
                editableState.value = currentState.copy(
                    rawText = rawText,
                    cleanText = cleaner.clean(rawText = rawText),
                    selectedItemIndexes = emptySet(),
                    hasUnsavedChanges = true,
                    isSaveConfirmed = false,
                    hasStaleItems = currentState.displayedItems.isNotEmpty(),
                )
            }

            is TranscriptTextValidationResult.TooLong -> {
                logD(
                    "Vocabulary source raw text rejected: " +
                        "sourceId=$sourceId, length=${rawText.length}, maxLength=${validation.maxLength}"
                )
                eventMessage.tryEmit(
                    EventMessage(
                        resId = Res.string.vocabulary_source_text_too_long,
                        validation.maxLength,
                        type = EventMessage.Type.Negative,
                    )
                )
            }
        }
    }

    fun save() {
        val currentState = state.value
        val source = currentState.source ?: return
        val title = currentState.title.trim()

        if (title.isBlank()) {
            eventMessage.tryEmit(
                EventMessage(
                    resId = Res.string.vocabulary_source_title_empty,
                    type = EventMessage.Type.Negative,
                )
            )
            return
        }

        viewModelScope.launch {
            runCatching {
                logD(
                    "Vocabulary source save started: " +
                        "sourceId=$sourceId, rawTextLength=${currentState.rawText.length}, " +
                        "cleanTextLength=${currentState.cleanText.length}, " +
                        "draftItems=${currentState.draftItems.size}, " +
                        "hasNewAnalysisDraft=${currentState.hasNewAnalysisDraft}"
                )
                val currentTime = getCurrentDateAsLong()
                updateVocabularySource(
                    source.copy(
                        title = title,
                        description = currentState.description.trim(),
                        rawText = currentState.rawText,
                        cleanText = currentState.cleanText,
                        updatedAt = currentTime,
                        lastAnalyzedAt = currentState.draftItems
                            .takeIf { it.isNotEmpty() }
                            ?.let { currentTime }
                            ?: source.lastAnalyzedAt,
                    )
                )

                if (currentState.draftItems.isNotEmpty()) {
                    if (currentState.hasNewAnalysisDraft) {
                        replaceVocabularySourceDraftItems(
                            sourceId = sourceId,
                            items = currentState.draftItems,
                        )
                    } else {
                        saveVocabularySourceItems(items = currentState.draftItems)
                    }
                }
            }.onSuccess {
                logD("Vocabulary source save finished: sourceId=$sourceId")
                editableState.value = state.value.copy(
                    draftItems = emptyList(),
                    selectedItemIndexes = emptySet(),
                    hasUnsavedChanges = false,
                    isSaveConfirmed = true,
                    hasStaleItems = false,
                    hasNewAnalysisDraft = false,
                )
                eventMessage.tryEmit(
                    EventMessage(
                        resId = Res.string.vocabulary_source_saved,
                        type = EventMessage.Type.Positive,
                    )
                )
            }.onFailure { throwable ->
                logE(
                    "Vocabulary source save failed: sourceId=$sourceId\n" +
                        throwable.stackTraceToString()
                )
                eventMessage.tryEmit(
                    EventMessage(
                        resId = Res.string.problem_with_saving_vocabulary_source,
                        type = EventMessage.Type.Negative,
                    )
                )
            }
        }
    }

    fun analyze() {
        val currentState = state.value
        val cleanText = currentState.cleanText.trim()
        if (cleanText.isBlank()) return

        editableState.value = currentState.copy(isAnalyzing = true)

        val analysisHandle = try {
            vocabularySourceAnalysisBackgroundManager.startAnalysis(
                sourceId = sourceId,
                sourceTitle = currentState.analysisNotificationTitle(),
            )
        } catch (throwable: Throwable) {
            logE(
                "Vocabulary source analysis background start failed: sourceId=$sourceId\n" +
                    throwable.stackTraceToString()
            )
            editableState.value = currentState.copy(isAnalyzing = false)
            eventMessage.tryEmit(
                EventMessage(
                    resId = Res.string.vocabulary_source_analysis_failed,
                    type = EventMessage.Type.Negative,
                )
            )
            return
        }

        val analysisJob = viewModelScope.launch {
            runCatching {
                logD(
                    "Vocabulary source analysis started: " +
                        "sourceId=$sourceId, cleanTextLength=${cleanText.length}"
                )
                val analysis = analyzeVocabularySourceText(cleanText = cleanText)
                val cards = fetchAllCards()
                val currentTime = getCurrentDateAsLong()

                categorizer.categorize(
                    sourceId = sourceId,
                    analysisItems = analysis.items,
                    cards = cards,
                    createdAt = currentTime,
                ).also { categorizedItems ->
                    logD(
                        "Vocabulary source analysis categorized: " +
                            "sourceId=$sourceId, aiItems=${analysis.items.size}, " +
                            "cards=${cards.size}, categorizedItems=${categorizedItems.size}, " +
                            "categories=${categorizedItems.categoryLogSummary()}, " +
                            "statuses=${categorizedItems.statusLogSummary()}"
                    )
                }
            }.onSuccess { draftItems ->
                editableState.value = state.value.copy(
                    draftItems = draftItems,
                    selectedItemIndexes = emptySet(),
                    isAnalyzing = false,
                    hasUnsavedChanges = true,
                    isSaveConfirmed = false,
                    hasStaleItems = false,
                    hasNewAnalysisDraft = true,
                )
                eventMessage.tryEmit(
                    EventMessage(
                        resId = Res.string.vocabulary_source_analysis_finished,
                        type = EventMessage.Type.Positive,
                    )
                )
            }.onFailure { throwable ->
                logE(
                    "Vocabulary source analysis failed: sourceId=$sourceId\n" +
                        throwable.stackTraceToString()
                )
                editableState.value = state.value.copy(isAnalyzing = false)
                eventMessage.tryEmit(
                    EventMessage(
                        resId = Res.string.vocabulary_source_analysis_failed,
                        type = EventMessage.Type.Negative,
                    )
                )
            }
        }

        analysisJob.invokeOnCompletion { failure ->
            finishVocabularySourceAnalysis(
                handle = analysisHandle,
                failure = failure,
            )
        }
    }

    fun changeItemSelectionState(index: Int) {
        val currentState = state.value
        if (index !in currentState.displayedItems.indices) return

        val updatedSelection = currentState.selectedItemIndexes
            .toMutableSet()
            .apply {
                if (index in this) remove(index) else add(index)
            }

        editableState.value = currentState.copy(selectedItemIndexes = updatedSelection)
    }

    fun changeAllItemSelectionState() {
        val currentState = state.value
        val updatedSelection = if (currentState.isAllItemsSelected) {
            emptySet()
        } else {
            currentState.displayedItems.indices.toSet()
        }

        editableState.value = currentState.copy(selectedItemIndexes = updatedSelection)
    }

    fun ignoreSelectedItems() {
        updateSelectedItems { item ->
            if (item.status == VocabularySourceItemStatus.PENDING) {
                item.copy(status = VocabularySourceItemStatus.IGNORED)
            } else {
                item
            }
        }
    }

    fun restoreSelectedItems() {
        updateSelectedItems { item ->
            if (item.status == VocabularySourceItemStatus.IGNORED) {
                item.copy(status = VocabularySourceItemStatus.PENDING)
            } else {
                item
            }
        }
    }

    fun startItemEditing(index: Int) {
        val currentState = state.value
        val item = currentState.displayedItems.getOrNull(index) ?: return

        editableState.value = currentState.copy(
            itemEditState = VocabularySourceItemEditState(
                index = index,
                foreignWord = item.foreignWord,
                nativeWord = item.nativeWord,
            ),
        )
    }

    fun onEditingForeignWordChanged(foreignWord: String) {
        val currentState = state.value
        val editState = currentState.itemEditState ?: return

        editableState.value = currentState.copy(
            itemEditState = editState.copy(foreignWord = foreignWord),
        )
    }

    fun onEditingNativeWordChanged(nativeWord: String) {
        val currentState = state.value
        val editState = currentState.itemEditState ?: return

        editableState.value = currentState.copy(
            itemEditState = editState.copy(nativeWord = nativeWord),
        )
    }

    fun applyItemEditing() {
        val currentState = state.value
        val editState = currentState.itemEditState ?: return
        val foreignWord = editState.foreignWord.trim()
        val nativeWord = editState.nativeWord.trim()

        if (foreignWord.isBlank() || nativeWord.isBlank()) return

        val currentTime = getCurrentDateAsLong()
        val updatedItems = currentState.displayedItems.mapIndexed { index, item ->
            if (index == editState.index) {
                item.copy(
                    foreignWord = foreignWord,
                    nativeWord = nativeWord,
                    updatedAt = currentTime,
                )
            } else {
                item
            }
        }

        editableState.value = currentState.copy(
            draftItems = updatedItems,
            itemEditState = null,
            hasUnsavedChanges = true,
            isSaveConfirmed = false,
        )
    }

    fun cancelItemEditing() {
        editableState.value = state.value.copy(itemEditState = null)
    }

    fun showDeckChooser() {
        val currentState = state.value

        when {
            currentState.hasUnsavedChanges -> {
                eventMessage.tryEmit(
                    EventMessage(
                        resId = Res.string.vocabulary_source_save_before_adding,
                        type = EventMessage.Type.Negative,
                    )
                )
            }

            currentState.selectedItemsForAdding.isEmpty() -> {
                eventMessage.tryEmit(
                    EventMessage(
                        resId = Res.string.vocabulary_source_no_items_to_add,
                        type = EventMessage.Type.Negative,
                    )
                )
            }

            else -> editableState.value = currentState.copy(isDeckChooserVisible = true)
        }
    }

    fun hideDeckChooser() {
        editableState.value = state.value.copy(
            isDeckChooserVisible = false,
            isNewDeckCreationVisible = false,
            newDeckName = "",
        )
    }

    fun showNewDeckCreation() {
        editableState.value = state.value.copy(isNewDeckCreationVisible = true)
    }

    fun hideNewDeckCreation() {
        editableState.value = state.value.copy(
            isNewDeckCreationVisible = false,
            newDeckName = "",
        )
    }

    fun onNewDeckNameChanged(deckName: String) {
        editableState.value = state.value.copy(
            newDeckName = deckName.take(Deck.MAX_NAME_LENGTH),
        )
    }

    fun addSelectedItemsToDeck(deck: Deck) {
        val currentState = state.value
        val items = currentState.selectedItemsForAdding
        if (items.isEmpty()) return

        editableState.value = currentState.copy(isAddingToDeck = true)

        viewModelScope.launch {
            runCatching {
                logD(
                    "Vocabulary source add-to-deck started: " +
                        "sourceId=$sourceId, deckId=${deck.id}, items=${items.size}"
                )
                addVocabularySourceItemsToDeck(deck = deck, items = items)
            }.onSuccess { addedCount ->
                logD(
                    "Vocabulary source add-to-deck finished: " +
                        "sourceId=$sourceId, deckId=${deck.id}, addedCount=$addedCount"
                )
                editableState.value = state.value.copy(
                    selectedItemIndexes = emptySet(),
                    isDeckChooserVisible = false,
                    isAddingToDeck = false,
                )
                eventMessage.tryEmit(
                    EventMessage(
                        resId = Res.string.vocabulary_source_items_added,
                        addedCount,
                        type = EventMessage.Type.Positive,
                    )
                )
            }.onFailure { throwable ->
                logE(
                    "Vocabulary source add-to-deck failed: " +
                        "sourceId=$sourceId, deckId=${deck.id}, items=${items.size}\n" +
                        throwable.stackTraceToString()
                )
                editableState.value = state.value.copy(isAddingToDeck = false)
                eventMessage.tryEmit(
                    EventMessage(
                        resId = Res.string.problem_with_saving_vocabulary_source,
                        type = EventMessage.Type.Negative,
                    )
                )
            }
        }
    }

    fun addSelectedItemsToNewDeck() {
        val currentState = state.value
        val items = currentState.selectedItemsForAdding
        val deckName = currentState.newDeckName.trim()

        if (items.isEmpty() || deckName.isBlank()) return

        editableState.value = currentState.copy(isAddingToDeck = true)

        viewModelScope.launch {
            runCatching {
                logD(
                    "Vocabulary source add-to-new-deck started: " +
                        "sourceId=$sourceId, deckNameLength=${deckName.length}, items=${items.size}"
                )
                addVocabularySourceItemsToDeck(deckName = deckName, items = items)
            }.onSuccess { addedCount ->
                logD(
                    "Vocabulary source add-to-new-deck finished: " +
                        "sourceId=$sourceId, addedCount=$addedCount"
                )
                editableState.value = state.value.copy(
                    selectedItemIndexes = emptySet(),
                    isDeckChooserVisible = false,
                    isNewDeckCreationVisible = false,
                    newDeckName = "",
                    isAddingToDeck = false,
                )
                eventMessage.tryEmit(
                    EventMessage(
                        resId = Res.string.vocabulary_source_items_added,
                        addedCount,
                        type = EventMessage.Type.Positive,
                    )
                )
            }.onFailure { throwable ->
                logE(
                    "Vocabulary source add-to-new-deck failed: " +
                        "sourceId=$sourceId, deckNameLength=${deckName.length}, items=${items.size}\n" +
                        throwable.stackTraceToString()
                )
                editableState.value = state.value.copy(isAddingToDeck = false)
                eventMessage.tryEmit(
                    EventMessage(
                        resId = Res.string.problem_with_saving_vocabulary_source,
                        type = EventMessage.Type.Negative,
                    )
                )
            }
        }
    }

    private fun updateSelectedItems(
        update: (VocabularySourceItem) -> VocabularySourceItem,
    ) {
        val currentState = state.value
        if (currentState.selectedItemIndexes.isEmpty()) return

        val currentTime = getCurrentDateAsLong()
        val updatedItems = currentState.displayedItems.mapIndexed { index, item ->
            if (index in currentState.selectedItemIndexes) {
                update(item).copy(updatedAt = currentTime)
            } else {
                item
            }
        }

        editableState.value = currentState.copy(
            draftItems = updatedItems,
            selectedItemIndexes = emptySet(),
            hasUnsavedChanges = true,
            isSaveConfirmed = false,
        )
    }

    private fun finishVocabularySourceAnalysis(
        handle: VocabularySourceAnalysisHandle,
        failure: Throwable?,
    ) {
        val outcome = when {
            failure == null -> VocabularySourceAnalysisOutcome.Succeeded
            failure is CancellationException -> VocabularySourceAnalysisOutcome.Cancelled
            else -> VocabularySourceAnalysisOutcome.Failed
        }
        runCatching {
            handle.finish(outcome = outcome)
        }.onFailure { backgroundManagerFailure ->
            logE(
                "Vocabulary source analysis background finish failed: sourceId=$sourceId\n" +
                    backgroundManagerFailure.stackTraceToString()
            )
        }
    }
}

private fun VocabularySourceDetailState.analysisNotificationTitle(): String =
    title.trim()
        .ifBlank { source?.title?.trim().orEmpty() }
        .ifBlank { "Untitled source" }

private fun List<VocabularySourceItem>.categoryLogSummary(): String {
    return groupingBy { item -> item.category }
        .eachCount()
        .toString()
}

private fun List<VocabularySourceItem>.statusLogSummary(): String {
    return groupingBy { item -> item.status }
        .eachCount()
        .toString()
}
