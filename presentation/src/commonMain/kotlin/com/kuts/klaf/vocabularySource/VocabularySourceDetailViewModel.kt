package com.kuts.klaf.vocabularySource

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuts.domain.common.getCurrentDateAsLong
import com.kuts.domain.entities.Deck
import com.kuts.domain.entities.VocabularySource
import com.kuts.domain.entities.VocabularySourceItem
import com.kuts.domain.entities.VocabularySourceItemStatus
import com.kuts.domain.managers.IVocabularySourceAnalysisBackgroundManager
import com.kuts.domain.managers.ITextToSpeechManager
import com.kuts.domain.managers.VocabularySourceAnalysisHandle
import com.kuts.domain.managers.VocabularySourceAnalysisOutcome
import com.kuts.domain.managers.VocabularySourceTranscriptionCoordinator
import com.kuts.domain.managers.VocabularySourceTranscriptionOperation
import com.kuts.domain.repositories.VocabularySourceTranscriptionProgress
import com.kuts.domain.repositories.VocabularySourceTranscriptionUpdate
import com.kuts.domain.useCases.AddVocabularySourceItemsToDeckUseCase
import com.kuts.domain.useCases.AnalyzeVocabularySourceTextUseCase
import com.kuts.domain.useCases.FetchAllCardsUseCase
import com.kuts.domain.useCases.FetchDeckSourceUseCase
import com.kuts.domain.useCases.FetchIgnoredVocabularyWordsUseCase
import com.kuts.domain.useCases.ObserveVocabularySourceByIdUseCase
import com.kuts.domain.useCases.ObserveVocabularySourceItemsUseCase
import com.kuts.domain.useCases.SaveVocabularySourceChangesUseCase
import com.kuts.domain.vocabularySource.TranscriptTextCleaner
import com.kuts.domain.vocabularySource.TranscriptTextValidationResult
import com.kuts.domain.vocabularySource.TranscriptTimestampFormatter
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
import com.kuts.klaf.presentation.resources.vocabulary_source_transcription_failed
import com.kuts.klaf.presentation.resources.vocabulary_source_transcription_finished
import com.lib.lokdroid.core.logD
import com.lib.lokdroid.core.logE
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SelectedAudioFile(
    val fileName: String,
    val byteSize: Long,
    val mimeType: String,
    val audioSource: suspend (writeChunk: suspend (ByteArray) -> Unit) -> Unit,
)

sealed interface TranscriptionUiState {
    data object Idle : TranscriptionUiState
    data class Uploading(val uploadedBytes: Long, val totalBytes: Long) : TranscriptionUiState
    data class Transcribing(val completedChunks: Int, val totalChunks: Int) : TranscriptionUiState
    data class Failed(val message: String) : TranscriptionUiState
}

data class VocabularySourceDetailState(
    val source: VocabularySource? = null,
    val title: String = "",
    val description: String = "",
    val url: String = "",
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
    val isSaving: Boolean = false,
    val hasStaleItems: Boolean = false,
    val hasNewAnalysisDraft: Boolean = false,
    val selectedAudioFile: SelectedAudioFile? = null,
    val transcriptionState: TranscriptionUiState = TranscriptionUiState.Idle,
) {

    val isTranscribing: Boolean
        get() = transcriptionState is TranscriptionUiState.Uploading ||
            transcriptionState is TranscriptionUiState.Transcribing

    val reviewItems: List<VocabularySourceItem>
        get() = if (hasNewAnalysisDraft || draftItems.isNotEmpty()) draftItems else persistedItems

    val displayedItems: List<VocabularySourceItem>
        get() = reviewItems.filter { item -> item.status != VocabularySourceItemStatus.ADDED }

    val itemHolders: List<SelectableVocabularySourceItemHolder>
        get() = reviewItems.mapIndexedNotNull { index, item ->
            if (item.status == VocabularySourceItemStatus.ADDED) return@mapIndexedNotNull null

            SelectableVocabularySourceItemHolder(
                item = item,
                sourceIndex = index,
                isSelected = index in selectedItemIndexes,
            )
        }

    val selectedItemCount: Int
        get() = itemHolders.count { holder -> holder.isSelected }

    val selectedItemsForAdding: List<VocabularySourceItem>
        get() = selectedItemIndexes
            .sorted()
            .mapNotNull { index -> reviewItems.getOrNull(index) }
            .filter { item -> item.status == VocabularySourceItemStatus.PENDING && !item.alreadyExists }

    val isAllItemsSelected: Boolean
        get() {
            val displayedIndexes = itemHolders.map { holder -> holder.sourceIndex }.toSet()
            return displayedIndexes.isNotEmpty() && displayedIndexes.all { index -> index in selectedItemIndexes }
        }

    val hasSavableContent: Boolean
        get() = title.isNotBlank() ||
            description.isNotBlank() ||
            url.isNotBlank() ||
            rawText.isNotBlank() ||
            cleanText.isNotBlank() ||
            reviewItems.isNotEmpty()
}

data class SelectableVocabularySourceItemHolder(
    val item: VocabularySourceItem,
    val sourceIndex: Int,
    val isSelected: Boolean = false,
)

data class VocabularySourceItemEditState(
    val index: Int,
    val foreignWord: String,
    val nativeWord: String,
)

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

class VocabularySourceDetailViewModel(
    private val sourceId: Int,
    observeVocabularySourceById: ObserveVocabularySourceByIdUseCase,
    observeVocabularySourceItems: ObserveVocabularySourceItemsUseCase,
    private val saveVocabularySourceChanges: SaveVocabularySourceChangesUseCase,
    private val analyzeVocabularySourceText: AnalyzeVocabularySourceTextUseCase,
    private val transcriptionCoordinator: VocabularySourceTranscriptionCoordinator,
    private val fetchAllCards: FetchAllCardsUseCase,
    private val fetchIgnoredVocabularyWords: FetchIgnoredVocabularyWordsUseCase,
    fetchDeckSource: FetchDeckSourceUseCase,
    private val addVocabularySourceItemsToDeck: AddVocabularySourceItemsToDeckUseCase,
    private val vocabularySourceAnalysisBackgroundManager: IVocabularySourceAnalysisBackgroundManager,
    private val textToSpeechManager: ITextToSpeechManager,
) : ViewModel(), IEventMessageSource {

    override val eventMessage = MutableSharedFlow<EventMessage>(extraBufferCapacity = 1)

    private val cleaner = TranscriptTextCleaner()
    private val categorizer = VocabularySourceItemCategorizer()
    private val editableState = MutableStateFlow(VocabularySourceDetailState())
    private var vocabularySourceAnalysisJob: Job? = null
    private var appliedTranscriptionId: Long? = null
    val state: StateFlow<VocabularySourceDetailState> = editableState.asStateFlow()

    init {
        viewModelScope.launch {
            transcriptionCoordinator.observe(sourceId).collect(::applyTranscriptionOperation)
        }
        viewModelScope.launch {
            combine(
                observeVocabularySourceById(sourceId),
                observeVocabularySourceItems(sourceId),
                fetchDeckSource(),
            ) { source, items, decks -> Triple(source, items, decks) }.collect { (source, items, decks) ->
                editableState.update { current ->
                    val updated = current.copy(source = source, persistedItems = items, decks = decks)
                    if (source != null && !current.hasUnsavedChanges && !current.isSaving) {
                        updated.copy(title = source.title, description = source.description, url = source.url,
                            rawText = source.rawText, cleanText = source.cleanText)
                    } else {
                        updated
                    }
                }
            }
        }
    }

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

    fun onUrlChanged(url: String) {
        editableState.value = state.value.copy(
            url = url,
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
        if (currentState.isSaving) return
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

        editableState.update { it.copy(isSaving = true) }
        val savedTranscriptionId = appliedTranscriptionId
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
                val updatedSource = source.copy(
                    title = title,
                    description = currentState.description.trim(),
                    url = currentState.url.trim(),
                    rawText = currentState.rawText,
                    cleanText = currentState.cleanText,
                    updatedAt = currentTime,
                    lastAnalyzedAt = currentState.hasNewAnalysisDraft
                        .takeIf { it }
                        ?.let { currentTime }
                        ?: source.lastAnalyzedAt,
                )
                saveVocabularySourceChanges(
                    source = updatedSource,
                    draftItems = currentState.draftItems,
                    shouldReplaceSourceItems = currentState.hasNewAnalysisDraft ||
                        currentState.draftItems.isNotEmpty(),
                )
                updatedSource
            }.onSuccess { savedSource ->
                logD("Vocabulary source save finished: sourceId=$sourceId")
                editableState.update { current ->
                    if (current.hasSameEditableContent(currentState)) {
                        current.copy(source = savedSource, title = savedSource.title,
                            description = savedSource.description, url = savedSource.url,
                            draftItems = emptyList(), selectedItemIndexes = emptySet(),
                            hasUnsavedChanges = false, isSaveConfirmed = true, isSaving = false,
                            hasStaleItems = currentState.hasStaleItems, hasNewAnalysisDraft = false)
                    } else {
                        current.copy(isSaving = false)
                    }
                }
                if (state.value.isSaveConfirmed && savedTranscriptionId != null) {
                    transcriptionCoordinator.clearCompleted(sourceId, savedTranscriptionId)
                }
                eventMessage.tryEmit(
                    EventMessage(
                        resId = Res.string.vocabulary_source_saved,
                        type = EventMessage.Type.Positive,
                    )
                )
            }.onFailure { throwable ->
                editableState.update { it.copy(isSaving = false) }
                if (throwable is CancellationException) throw throwable
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

    private fun VocabularySourceDetailState.hasSameEditableContent(other: VocabularySourceDetailState): Boolean {
        return title == other.title && description == other.description && url == other.url &&
            rawText == other.rawText && cleanText == other.cleanText && draftItems == other.draftItems &&
            hasNewAnalysisDraft == other.hasNewAnalysisDraft
    }

    fun analyze() {
        if (vocabularySourceAnalysisJob?.isActive == true) return

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

        var serverNotificationSent = false
        var operationFailure: Throwable? = null
        val analysisJob = viewModelScope.launch {
            runCatching {
                logD(
                    "Vocabulary source analysis started: " +
                        "sourceId=$sourceId, cleanTextLength=${cleanText.length}"
                )
                val analysisResult = analyzeVocabularySourceText(
                    cleanText = cleanText,
                    sourceId = sourceId,
                    sourceTitle = currentState.analysisNotificationTitle(),
                )
                val analysis = analysisResult.analysis
                serverNotificationSent = analysisResult.serverNotificationSent
                val cards = fetchAllCards()
                val ignoredWords = fetchIgnoredVocabularyWords()
                val currentTime = getCurrentDateAsLong()

                categorizer.categorize(
                    sourceId = sourceId,
                    language = analysis.language,
                    analysisItems = analysis.items,
                    cards = cards,
                    ignoredWords = ignoredWords,
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
                    hasStaleItems = state.value.cleanText.trim() != cleanText,
                    hasNewAnalysisDraft = true,
                )
                eventMessage.tryEmit(
                    EventMessage(
                        resId = Res.string.vocabulary_source_analysis_finished,
                        type = EventMessage.Type.Positive,
                    )
                )
            }.onFailure { throwable ->
                operationFailure = throwable
                if (throwable is CancellationException) {
                    logD("Vocabulary source analysis cancelled: sourceId=$sourceId")
                    editableState.value = state.value.copy(isAnalyzing = false)
                    throw throwable
                }
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

        vocabularySourceAnalysisJob = analysisJob
        analysisJob.invokeOnCompletion { failure ->
            finishVocabularySourceAnalysis(
                handle = analysisHandle,
                failure = failure ?: operationFailure,
                serverNotificationSent = serverNotificationSent,
            )
            if (vocabularySourceAnalysisJob == analysisJob) {
                vocabularySourceAnalysisJob = null
            }
        }
    }

    fun cancelAnalysis() {
        vocabularySourceAnalysisJob?.cancel()
        editableState.value = state.value.copy(isAnalyzing = false)
    }

    fun onAudioFileSelected(
        fileName: String,
        byteSize: Long,
        mimeType: String,
        audioSource: suspend (writeChunk: suspend (ByteArray) -> Unit) -> Unit,
    ) {
        cancelTranscription()
        if (byteSize <= 0L) {
            editableState.update {
                it.copy(selectedAudioFile = null,
                    transcriptionState = TranscriptionUiState.Failed("Audio file size is unavailable or the file is empty"))
            }
            return
        }
        editableState.update { current ->
            current.copy(
                selectedAudioFile = SelectedAudioFile(
                    fileName = fileName,
                    byteSize = byteSize,
                    mimeType = mimeType,
                    audioSource = audioSource,
                ),
                transcriptionState = TranscriptionUiState.Idle,
            )
        }
    }

    fun onClearAudioFile() {
        cancelTranscription()
        editableState.update { current ->
            current.copy(
                selectedAudioFile = null,
                transcriptionState = TranscriptionUiState.Idle,
            )
        }
    }

    fun onTranscribeAudio() {
        val current = state.value
        if (current.isTranscribing) return
        val audio = current.selectedAudioFile ?: return
        transcriptionCoordinator.start(
            sourceId = sourceId,
            sourceTitle = current.analysisNotificationTitle(),
            fileName = audio.fileName,
            audioFormat = audio.mimeType,
            byteSize = audio.byteSize,
            audioSource = audio.audioSource,
        )
    }

    fun cancelTranscription() {
        transcriptionCoordinator.cancel(sourceId)
    }

    private fun applyTranscriptionOperation(operation: VocabularySourceTranscriptionOperation?) {
        if (operation == null) return
        if (operation.isActive) {
            val progress = (operation.update as? VocabularySourceTranscriptionUpdate.Progress)?.progress ?: return
            val uiState = when (progress) {
                is VocabularySourceTranscriptionProgress.Uploading ->
                    TranscriptionUiState.Uploading(progress.uploadedBytes, progress.totalBytes)
                is VocabularySourceTranscriptionProgress.Transcribing ->
                    TranscriptionUiState.Transcribing(progress.completedChunks, progress.totalChunks)
            }
            editableState.update { it.copy(transcriptionState = uiState) }
            return
        }
        if (appliedTranscriptionId == operation.id) return
        appliedTranscriptionId = operation.id
        val failure = operation.failure
        if (failure is CancellationException) {
            editableState.update { it.copy(transcriptionState = TranscriptionUiState.Idle) }
            return
        }
        if (failure != null) {
            editableState.update {
                it.copy(transcriptionState = TranscriptionUiState.Failed(failure.message ?: "Audio transcription failed"))
            }
            eventMessage.tryEmit(EventMessage(Res.string.vocabulary_source_transcription_failed, type = EventMessage.Type.Negative))
            return
        }
        val result = (operation.update as? VocabularySourceTranscriptionUpdate.Success)?.result ?: return
        val formatted = TranscriptTimestampFormatter.format(result.transcript, result.segments)
        val validation = cleaner.validateRawText(formatted)
        if (validation is TranscriptTextValidationResult.TooLong) {
            editableState.update {
                it.copy(transcriptionState = TranscriptionUiState.Failed("Transcript exceeds the ${validation.maxLength}-character limit"))
            }
            eventMessage.tryEmit(EventMessage(Res.string.vocabulary_source_text_too_long, validation.maxLength,
                type = EventMessage.Type.Negative))
            return
        }
        onRawTextChanged(formatted)
        editableState.update { it.copy(transcriptionState = TranscriptionUiState.Idle) }
        eventMessage.tryEmit(EventMessage(Res.string.vocabulary_source_transcription_finished, type = EventMessage.Type.Positive))
    }

    fun changeItemSelectionState(index: Int) {
        val currentState = state.value
        if (index !in currentState.reviewItems.indices) return

        val updatedSelection = currentState.selectedItemIndexes
            .toMutableSet()
            .apply {
                if (index in this) remove(index) else add(index)
            }

        editableState.value = currentState.copy(selectedItemIndexes = updatedSelection)
    }

    fun changeAllItemSelectionState() {
        val currentState = state.value
        val displayedIndexes = currentState.itemHolders.map { holder -> holder.sourceIndex }.toSet()
        val updatedSelection = if (currentState.isAllItemsSelected) {
            currentState.selectedItemIndexes - displayedIndexes
        } else {
            currentState.selectedItemIndexes + displayedIndexes
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
        val item = currentState.reviewItems.getOrNull(index) ?: return

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
        val updatedItems = currentState.reviewItems.mapIndexed { index, item ->
            if (index == editState.index) {
                item.copy(
                    foreignWord = foreignWord,
                    nativeWord = nativeWord,
                    isEdited = true,
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
        val updatedItems = currentState.reviewItems.mapIndexed { index, item ->
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

    fun speakItemForeignWord(index: Int) {
        val item = state.value.reviewItems.getOrNull(index) ?: return
        speak(text = item.foreignWord)
    }

    fun speak(text: String) {
        textToSpeechManager.speak(text = text)
    }

    private fun finishVocabularySourceAnalysis(
        handle: VocabularySourceAnalysisHandle,
        failure: Throwable?,
        serverNotificationSent: Boolean,
    ) {
        val outcome = when {
            failure == null -> VocabularySourceAnalysisOutcome.Succeeded
            failure is CancellationException -> VocabularySourceAnalysisOutcome.Cancelled
            else -> VocabularySourceAnalysisOutcome.Failed
        }
        runCatching {
            handle.finish(
                outcome = outcome,
                serverNotificationSent = serverNotificationSent,
            )
        }.onFailure { backgroundManagerFailure ->
            logE(
                "Vocabulary source analysis background finish failed: sourceId=$sourceId\n" +
                    backgroundManagerFailure.stackTraceToString()
            )
        }
    }


    override fun onCleared() {
        textToSpeechManager.shutdown()
        super.onCleared()
    }
}
