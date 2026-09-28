package com.kuts.klaf.vocabularySource

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import com.kuts.domain.entities.CefrLevel
import com.kuts.domain.entities.Deck
import com.kuts.domain.entities.VocabularySource
import com.kuts.domain.entities.VocabularySourceItem
import com.kuts.domain.entities.VocabularySourceItemCategory
import com.kuts.domain.entities.VocabularySourceItemConfidence
import com.kuts.domain.entities.VocabularySourceItemOccurrence
import com.kuts.domain.entities.VocabularySourceItemPartOfSpeech
import com.kuts.domain.entities.VocabularySourceItemStatus
import com.kuts.klaf.common.BaseMainViewModel
import com.kuts.klaf.common.CustomCheckBox
import com.kuts.klaf.common.ROUNDED_ELEMENT_SIZE
import com.kuts.klaf.common.RoundButton
import com.kuts.klaf.navigation.CollectFlowWithLifecycle
import com.kuts.klaf.presentation.resources.Res
import com.kuts.klaf.presentation.resources.ic_baseline_volume_up_24
import com.kuts.klaf.presentation.resources.ic_close_24
import com.kuts.klaf.presentation.resources.ic_collapse_vertical_24
import com.kuts.klaf.presentation.resources.ic_confidence_high_24
import com.kuts.klaf.presentation.resources.ic_confidence_low_24
import com.kuts.klaf.presentation.resources.ic_confidence_medium_24
import com.kuts.klaf.presentation.resources.ic_copy_24
import com.kuts.klaf.presentation.resources.ic_edit_24
import com.kuts.klaf.presentation.resources.ic_expand_vertical_24
import com.kuts.klaf.presentation.resources.ic_save_24
import com.kuts.klaf.presentation.resources.ic_target_24
import com.kuts.klaf.presentation.resources.label_foreign_word
import com.kuts.klaf.presentation.resources.label_native_word
import com.kuts.klaf.presentation.resources.vocabulary_source_add_to_deck_action
import com.kuts.klaf.presentation.resources.vocabulary_source_analyze_action
import com.kuts.klaf.presentation.resources.vocabulary_source_analyzing_action
import com.kuts.klaf.presentation.resources.vocabulary_source_cancel_action
import com.kuts.klaf.presentation.resources.vocabulary_source_character_count
import com.kuts.klaf.presentation.resources.vocabulary_source_clean_text_label
import com.kuts.klaf.presentation.resources.vocabulary_source_clear_selection_action
import com.kuts.klaf.presentation.resources.vocabulary_source_collapse_text_action
import com.kuts.klaf.presentation.resources.vocabulary_source_description_label
import com.kuts.klaf.presentation.resources.vocabulary_source_edit_action
import com.kuts.klaf.presentation.resources.vocabulary_source_edit_item_title
import com.kuts.klaf.presentation.resources.vocabulary_source_expand_text_action
import com.kuts.klaf.presentation.resources.vocabulary_source_ignore_action
import com.kuts.klaf.presentation.resources.vocabulary_source_items_empty
import com.kuts.klaf.presentation.resources.vocabulary_source_locate_occurrences_action
import com.kuts.klaf.presentation.resources.vocabulary_source_new_deck_action
import com.kuts.klaf.presentation.resources.vocabulary_source_new_deck_name_label
import com.kuts.klaf.presentation.resources.vocabulary_source_no_decks_available
import com.kuts.klaf.presentation.resources.vocabulary_source_original_text_mode
import com.kuts.klaf.presentation.resources.vocabulary_source_raw_text_label
import com.kuts.klaf.presentation.resources.vocabulary_source_restore_action
import com.kuts.klaf.presentation.resources.vocabulary_source_save_action
import com.kuts.klaf.presentation.resources.vocabulary_source_select_all_action
import com.kuts.klaf.presentation.resources.vocabulary_source_audio_clear_action
import com.kuts.klaf.presentation.resources.vocabulary_source_audio_label
import com.kuts.klaf.presentation.resources.vocabulary_source_audio_replace_action
import com.kuts.klaf.presentation.resources.vocabulary_source_audio_select_action
import com.kuts.klaf.presentation.resources.vocabulary_source_copy_url_action
import com.kuts.klaf.presentation.resources.vocabulary_source_speak_word_action
import com.kuts.klaf.presentation.resources.vocabulary_source_selected_items
import com.kuts.klaf.presentation.resources.vocabulary_source_stale_items_warning
import com.kuts.klaf.presentation.resources.vocabulary_source_target_deck_title
import com.kuts.klaf.presentation.resources.vocabulary_source_title_label
import com.kuts.klaf.presentation.resources.vocabulary_source_transcribe_action
import com.kuts.klaf.presentation.resources.vocabulary_source_transcribing_action
import com.kuts.klaf.presentation.resources.vocabulary_source_transcription_failed
import com.kuts.klaf.presentation.resources.vocabulary_source_uploading_action
import com.kuts.klaf.presentation.resources.vocabulary_source_url_label
import com.kuts.klaf.theme.DarkThemePreviewBackground
import com.kuts.klaf.theme.LightThemePreviewBackground
import com.kuts.klaf.theme.MainTheme
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private const val COLLAPSED_TRANSCRIPT_TEXT_LINES = 10
private const val TRANSCRIPT_TEXT_MODE_ANIMATION_DURATION_MS = 260
private val TranscriptTextModeSwitchWidth = 260.dp
private val TranscriptTextModeSwitchHeight = 44.dp
private val FloatingSaveButtonContentBottomPadding = 124.dp
private val FloatingSaveButtonEdgePadding = 48.dp
private val TargetDeckChooserListMaxHeight = 280.dp

private enum class VocabularySourceTextMode {
    ORIGINAL,
    CLEAN,
}

private data class VocabularySourceTextHighlightRange(
    val startOffset: Int,
    val endOffset: Int,
)

private data class VocabularySourceTextLocateRequest(
    val id: Int,
    val startOffset: Int,
)

private class VocabularySourceHighlightTransformation(
    private val ranges: List<VocabularySourceTextHighlightRange>,
    private val highlightColor: Color,
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        if (ranges.isEmpty() || text.isEmpty()) {
            return TransformedText(text = text, offsetMapping = OffsetMapping.Identity)
        }

        return TransformedText(
            text = text.highlighted(ranges = ranges, highlightColor = highlightColor),
            offsetMapping = OffsetMapping.Identity,
        )
    }
}

private fun AnnotatedString.highlighted(
    ranges: List<VocabularySourceTextHighlightRange>,
    highlightColor: Color,
): AnnotatedString {
    return buildAnnotatedString {
        append(this@highlighted)
        ranges.forEach { range ->
            val startOffset = range.startOffset.coerceIn(minimumValue = 0, maximumValue = length)
            val endOffset = range.endOffset.coerceIn(minimumValue = startOffset, maximumValue = length)
            if (startOffset == endOffset) return@forEach

            addStyle(
                style = SpanStyle(background = highlightColor),
                start = startOffset,
                end = endOffset,
            )
        }
    }
}

private fun List<VocabularySourceItemOccurrence>.toHighlightRanges(
    text: String,
): List<VocabularySourceTextHighlightRange> {
    if (text.isBlank()) return emptyList()

    return mapNotNull { occurrence ->
        val startOffset = occurrence.startOffset.coerceIn(minimumValue = 0, maximumValue = text.length)
        val endOffset = occurrence.endOffset.coerceIn(minimumValue = startOffset, maximumValue = text.length)
        if (startOffset == endOffset) {
            null
        } else {
            VocabularySourceTextHighlightRange(startOffset = startOffset, endOffset = endOffset)
        }
    }
        .sortedBy(VocabularySourceTextHighlightRange::startOffset)
        .mergeOverlappingRanges()
}

private fun List<VocabularySourceTextHighlightRange>.mergeOverlappingRanges(): List<VocabularySourceTextHighlightRange> {
    if (size < 2) return this

    return fold(mutableListOf()) { mergedRanges, range ->
        val previousRange = mergedRanges.lastOrNull()
        if (previousRange != null && range.startOffset <= previousRange.endOffset) {
            mergedRanges[mergedRanges.lastIndex] = previousRange.copy(
                endOffset = maxOf(previousRange.endOffset, range.endOffset),
            )
        } else {
            mergedRanges += range
        }
        mergedRanges
    }
}

private fun TextRange.coerceIn(text: String): TextRange {
    if (text.isEmpty()) return TextRange(index = 0)

    return TextRange(
        start = start.coerceIn(minimumValue = 0, maximumValue = text.length),
        end = end.coerceIn(minimumValue = 0, maximumValue = text.length),
    )
}

private fun formatFileSize(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024.0) return "${(kb * 10).toLong() / 10.0} KB"
    val mb = kb / 1024.0
    return "${(mb * 10).toLong() / 10.0} MB"
}

private fun VocabularySourceItemCategory.toDisplayLabel(): String {
    return when (this) {
        VocabularySourceItemCategory.NEW -> "NEW"
        VocabularySourceItemCategory.POSSIBLE_NEW_MEANING -> "NEW MEANING"
        VocabularySourceItemCategory.IGNORED_WORD_NEW_MEANING -> "IGNORED · NEW MEANING"
    }
}

@Composable
private fun VocabularySourceItemCategory.toBadgeContainerColor(): Color {
    return when (this) {
        VocabularySourceItemCategory.NEW -> MainTheme.colors.vocabularySourceScreen.newBadge.container
        VocabularySourceItemCategory.POSSIBLE_NEW_MEANING -> {
            MainTheme.colors.vocabularySourceScreen.newMeaningBadge.container
        }
        VocabularySourceItemCategory.IGNORED_WORD_NEW_MEANING -> {
            MainTheme.colors.vocabularySourceScreen.ignoredNewMeaningBadge.container
        }
    }
}

@Composable
private fun VocabularySourceItemCategory.toBadgeContentColor(): Color {
    return when (this) {
        VocabularySourceItemCategory.NEW -> MainTheme.colors.vocabularySourceScreen.newBadge.content
        VocabularySourceItemCategory.POSSIBLE_NEW_MEANING -> {
            MainTheme.colors.vocabularySourceScreen.newMeaningBadge.content
        }
        VocabularySourceItemCategory.IGNORED_WORD_NEW_MEANING -> {
            MainTheme.colors.vocabularySourceScreen.ignoredNewMeaningBadge.content
        }
    }
}

private fun VocabularySourceItemPartOfSpeech.toDisplayLabel(): String {
    return when (this) {
        VocabularySourceItemPartOfSpeech.UNKNOWN -> "UNKNOWN"
        VocabularySourceItemPartOfSpeech.NOUN -> "NOUN"
        VocabularySourceItemPartOfSpeech.VERB -> "VERB"
        VocabularySourceItemPartOfSpeech.ADJECTIVE -> "ADJECTIVE"
        VocabularySourceItemPartOfSpeech.ADVERB -> "ADVERB"
        VocabularySourceItemPartOfSpeech.PHRASAL_VERB -> "PHRASAL"
        VocabularySourceItemPartOfSpeech.PHRASE -> "PHRASE"
        VocabularySourceItemPartOfSpeech.IDIOM -> "IDIOM"
        VocabularySourceItemPartOfSpeech.OTHER -> "OTHER"
    }
}

@Composable
private fun CefrLevel.toVocabularySourceCefrBadgeColor(): Color =
    MainTheme.colors.vocabularySourceScreen.cefrBadgeContainers[ordinal]

private fun vocabularySourceDetailPreviewState(): VocabularySourceDetailState {
    return VocabularySourceDetailState(
        source = VocabularySource(
            id = 1,
            title = "Severance S01E01",
            description = "Transcript from YouTube captions.",
            rawText = "00:00:01,000 --> 00:00:03,000\nTurn it on before the meeting.",
            cleanText = "Turn it on before the meeting.",
            createdAt = 1_718_000_000_000L,
            updatedAt = 1_718_200_000_000L,
            lastAnalyzedAt = 1_718_220_000_000L,
        ),
        title = "Severance S01E01",
        description = "Transcript from YouTube captions.",
        rawText = "00:00:01,000 --> 00:00:03,000\nTurn it on before the meeting.",
        cleanText = "Turn it on before the meeting.",
        draftItems = vocabularySourcePreviewItems(),
        decks = vocabularySourcePreviewDecks(),
        selectedItemIndexes = setOf(0),
    )
}

private fun vocabularySourcePreviewItems(): List<VocabularySourceItem> {
    return listOf(
        VocabularySourceItem(
            sourceId = 1,
            foreignWord = "turn on",
            transcription = "tɜːrn ɑːn",
            nativeWord = "включать",
            originalText = "turn it on",
            partOfSpeech = VocabularySourceItemPartOfSpeech.PHRASAL_VERB,
            cefrLevel = CefrLevel.A2,
            confidence = VocabularySourceItemConfidence.HIGH,
            category = VocabularySourceItemCategory.POSSIBLE_NEW_MEANING,
            status = VocabularySourceItemStatus.PENDING,
            sourceExample = "Turn it on before the meeting.",
            explanation = "Here it means to activate a device or system.",
            knownMeaningsSnapshot = "поворачивать",
            occurrences = listOf(
                VocabularySourceItemOccurrence(
                    timestamp = "00:00:01",
                    startOffset = 0,
                    endOffset = 10,
                    sentence = "Turn it on before the meeting.",
                )
            ),
            firstOccurrenceOrder = 0,
            createdAt = 1_718_220_000_000L,
            updatedAt = 1_718_220_000_000L,
            id = 1,
        ),
        VocabularySourceItem(
            sourceId = 1,
            foreignWord = "meeting",
            transcription = "ˈmiːtɪŋ",
            nativeWord = "встреча",
            originalText = "meeting",
            partOfSpeech = VocabularySourceItemPartOfSpeech.NOUN,
            cefrLevel = CefrLevel.A1,
            confidence = VocabularySourceItemConfidence.MEDIUM,
            category = VocabularySourceItemCategory.NEW,
            status = VocabularySourceItemStatus.PENDING,
            sourceExample = "Turn it on before the meeting.",
            explanation = "A planned business discussion.",
            firstOccurrenceOrder = 20,
            createdAt = 1_718_220_000_000L,
            updatedAt = 1_718_220_000_000L,
            id = 2,
        ),
        VocabularySourceItem(
            sourceId = 1,
            foreignWord = "figure out",
            transcription = "ˈfɪɡjər aʊt",
            nativeWord = "понять, разобраться",
            originalText = "figured it out",
            partOfSpeech = VocabularySourceItemPartOfSpeech.PHRASAL_VERB,
            cefrLevel = CefrLevel.B1,
            confidence = VocabularySourceItemConfidence.HIGH,
            category = VocabularySourceItemCategory.POSSIBLE_NEW_MEANING,
            status = VocabularySourceItemStatus.ADDED,
            sourceExample = "I finally figured it out after watching the whole scene again.",
            explanation = "The phrase means to understand something after thinking about it.",
            knownMeaningsSnapshot = "вычислять; считать",
            alreadyExists = true,
            createdCardId = 10,
            targetDeckId = 1,
            firstOccurrenceOrder = 36,
            createdAt = 1_718_220_000_000L,
            updatedAt = 1_718_220_000_000L,
            id = 3,
        ),
        VocabularySourceItem(
            sourceId = 1,
            foreignWord = "routine",
            transcription = "ruːˈtiːn",
            nativeWord = "привычный порядок, рутина",
            originalText = "routine",
            partOfSpeech = VocabularySourceItemPartOfSpeech.NOUN,
            cefrLevel = CefrLevel.B2,
            confidence = VocabularySourceItemConfidence.LOW,
            category = VocabularySourceItemCategory.NEW,
            status = VocabularySourceItemStatus.ADDED,
            sourceExample = "The character follows the same routine every morning before going to work.",
            explanation = "The word describes a repeated set of actions that happens regularly.",
            createdCardId = 11,
            targetDeckId = Deck.INTERIM_DECK_ID,
            firstOccurrenceOrder = 52,
            createdAt = 1_718_220_000_000L,
            updatedAt = 1_718_220_000_000L,
            id = 4,
        ),
    ).let { items ->
        listOf(items[0], items[1], items[3], items[2])
    }
}

private fun vocabularySourcePreviewDecks(): List<Deck> {
    return listOf(
        Deck(
            id = 1,
            name = "Series Vocabulary",
            creationDate = 1_718_000_000_000L,
            cardQuantity = 42,
        ),
        Deck(
            id = 2,
            name = "Phrasal Verbs",
            creationDate = 1_718_100_000_000L,
            cardQuantity = 18,
        ),
    )
}

@Composable
internal fun VocabularySourceDetailScreen(
    navController: NavHostController,
    backStackEntry: NavBackStackEntry,
    sharedViewModel: BaseMainViewModel,
    sourceId: Int,
) {
    val viewModel: VocabularySourceDetailViewModel = koinViewModel(
        viewModelStoreOwner = backStackEntry,
        parameters = { parametersOf(sourceId) },
    )
    val audioPicker = rememberAudioFilePickerLauncher(
        onAudioFileSelected = viewModel::onAudioFileSelected,
    )

    CollectFlowWithLifecycle(flow = viewModel.eventMessage, onEach = sharedViewModel::notify)

    Surface {
        VocabularySourceDetailContent(
            state = viewModel.state.collectAsState().value,
            onTitleChanged = viewModel::onTitleChanged,
            onDescriptionChanged = viewModel::onDescriptionChanged,
            onUrlChanged = viewModel::onUrlChanged,
            onSelectAudioClick = audioPicker::launch,
            onClearAudioFile = viewModel::onClearAudioFile,
            onTranscribeAudio = viewModel::onTranscribeAudio,
            onCancelTranscription = viewModel::cancelTranscription,
            onRawTextChanged = viewModel::onRawTextChanged,
            onSave = viewModel::save,
            onAnalyze = viewModel::analyze,
            onCancelAnalysis = viewModel::cancelAnalysis,
            onItemSelectionChanged = viewModel::changeItemSelectionState,
            onAllItemsSelectionChanged = viewModel::changeAllItemSelectionState,
            onIgnoreSelectedItems = viewModel::ignoreSelectedItems,
            onRestoreSelectedItems = viewModel::restoreSelectedItems,
            onStartItemEditing = viewModel::startItemEditing,
            onEditingForeignWordChanged = viewModel::onEditingForeignWordChanged,
            onEditingNativeWordChanged = viewModel::onEditingNativeWordChanged,
            onApplyItemEditing = viewModel::applyItemEditing,
            onCancelItemEditing = viewModel::cancelItemEditing,
            onAddSelectedToDeckClick = viewModel::showDeckChooser,
            onDeckSelected = viewModel::addSelectedItemsToDeck,
            onCancelDeckChoosing = viewModel::hideDeckChooser,
            onNewDeckClick = viewModel::showNewDeckCreation,
            onNewDeckNameChanged = viewModel::onNewDeckNameChanged,
            onNewDeckConfirm = viewModel::addSelectedItemsToNewDeck,
            onNewDeckCancel = viewModel::hideNewDeckCreation,
            onSpeakItemClick = viewModel::speakItemForeignWord,
            onSpeakTextClick = viewModel::speak,
        )
    }
}

@Composable
private fun VocabularySourceDetailContent(
    state: VocabularySourceDetailState,
    onTitleChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onUrlChanged: (String) -> Unit,
    onSelectAudioClick: () -> Unit,
    onClearAudioFile: () -> Unit,
    onTranscribeAudio: () -> Unit,
    onCancelTranscription: () -> Unit,
    onRawTextChanged: (String) -> Unit,
    onSave: () -> Unit,
    onAnalyze: () -> Unit,
    onCancelAnalysis: () -> Unit,
    onItemSelectionChanged: (Int) -> Unit,
    onAllItemsSelectionChanged: () -> Unit,
    onIgnoreSelectedItems: () -> Unit,
    onRestoreSelectedItems: () -> Unit,
    onStartItemEditing: (Int) -> Unit,
    onEditingForeignWordChanged: (String) -> Unit,
    onEditingNativeWordChanged: (String) -> Unit,
    onApplyItemEditing: () -> Unit,
    onCancelItemEditing: () -> Unit,
    onAddSelectedToDeckClick: () -> Unit,
    onDeckSelected: (Deck) -> Unit,
    onCancelDeckChoosing: () -> Unit,
    onNewDeckClick: () -> Unit,
    onNewDeckNameChanged: (String) -> Unit,
    onNewDeckConfirm: () -> Unit,
    onNewDeckCancel: () -> Unit,
    onSpeakItemClick: (Int) -> Unit,
    onSpeakTextClick: (String) -> Unit,
) {
    var textMode by rememberSaveable { mutableStateOf(VocabularySourceTextMode.ORIGINAL) }
    var isTranscriptExpanded by rememberSaveable { mutableStateOf(false) }
    var highlightedTextRanges by remember { mutableStateOf(emptyList<VocabularySourceTextHighlightRange>()) }
    var clearTranscriptSelectionRequestId by remember { mutableStateOf(0) }
    var locateRequestId by remember { mutableStateOf(0) }
    var locateRequest by remember { mutableStateOf<VocabularySourceTextLocateRequest?>(null) }
    val clipboardManager = LocalClipboardManager.current
    val lazyListState = rememberLazyListState()
    val canSave = state.hasUnsavedChanges && state.hasSavableContent && !state.isSaving
    val isReviewLocked = state.isAnalyzing && state.itemHolders.isNotEmpty()
    val disabledButtonContainerColor = MainTheme.colors.vocabularySourceScreen.disabledButtonContainer
    val disabledButtonContentColor = MainTheme.colors.vocabularySourceScreen.disabledButtonContent

    LaunchedEffect(state.cleanText) {
        highlightedTextRanges = highlightedTextRanges.filter { range ->
            range.startOffset >= 0 &&
                range.endOffset > range.startOffset &&
                range.endOffset <= state.cleanText.length
        }
    }

    state.itemEditState?.let { editState ->
        VocabularySourceItemEditDialog(
            editState = editState,
            onForeignWordChanged = onEditingForeignWordChanged,
            onNativeWordChanged = onEditingNativeWordChanged,
            onConfirm = onApplyItemEditing,
            onCancel = onCancelItemEditing,
        )
    }
    if (state.isDeckChooserVisible) {
        VocabularySourceDeckChooserDialog(
            decks = state.decks,
            isNewDeckCreationVisible = state.isNewDeckCreationVisible,
            newDeckName = state.newDeckName,
            isAddingToDeck = state.isAddingToDeck,
            onDeckSelected = onDeckSelected,
            onNewDeckClick = onNewDeckClick,
            onNewDeckNameChanged = onNewDeckNameChanged,
            onNewDeckConfirm = onNewDeckConfirm,
            onNewDeckCancel = onNewDeckCancel,
            onCancel = onCancelDeckChoosing,
        )
    }

    LaunchedEffect(locateRequest?.id) {
        if (locateRequest != null) {
            lazyListState.animateScrollToItem(index = 0)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            contentPadding = PaddingValues(bottom = FloatingSaveButtonContentBottomPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = state.title,
                    onValueChange = onTitleChanged,
                    label = {
                        Text(text = stringResource(resource = Res.string.vocabulary_source_title_label))
                    },
                    singleLine = true,
                )
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    value = state.description,
                    onValueChange = onDescriptionChanged,
                    label = {
                        Text(text = stringResource(resource = Res.string.vocabulary_source_description_label))
                    },
                    minLines = 2,
                )
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    value = state.url,
                    onValueChange = onUrlChanged,
                    label = {
                        Text(text = stringResource(resource = Res.string.vocabulary_source_url_label))
                    },
                    singleLine = true,
                    trailingIcon = if (state.url.isNotBlank()) {
                        {
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(text = state.url))
                                },
                            ) {
                                Icon(
                                    painter = painterResource(resource = Res.drawable.ic_copy_24),
                                    contentDescription = stringResource(resource = Res.string.vocabulary_source_copy_url_action),
                                )
                            }
                        }
                    } else null,
                )
                VocabularySourceAudioSection(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    selectedAudioFile = state.selectedAudioFile,
                    transcriptionState = state.transcriptionState,
                    onSelectAudioClick = onSelectAudioClick,
                    onClearAudioClick = onClearAudioFile,
                    onTranscribeClick = onTranscribeAudio,
                    onCancelTranscriptionClick = onCancelTranscription,
                )
                VocabularySourceTranscriptTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    rawText = state.rawText,
                    cleanText = state.cleanText,
                    textMode = textMode,
                    isExpanded = isTranscriptExpanded,
                    highlightRanges = highlightedTextRanges,
                    locateRequest = locateRequest,
                    clearSelectionRequestId = clearTranscriptSelectionRequestId,
                    onTextModeChanged = { textMode = it },
                    onExpandedChanged = { isTranscriptExpanded = it },
                    onRawTextChanged = onRawTextChanged,
                )
                if (state.hasStaleItems) {
                    Text(text = stringResource(resource = Res.string.vocabulary_source_stale_items_warning))
                }
                Row(
                    modifier = Modifier.padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        enabled = state.cleanText.isNotBlank(),
                        onClick = {
                            if (!state.isAnalyzing) {
                                highlightedTextRanges = emptyList()
                                locateRequest = null
                                clearTranscriptSelectionRequestId += 1
                                onAnalyze()
                            }
                        },
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (state.isAnalyzing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MainTheme.colors.material.onPrimary,
                                    strokeWidth = 2.dp,
                                )
                            }
                            Text(
                                text = stringResource(
                                    resource = if (state.isAnalyzing) {
                                        Res.string.vocabulary_source_analyzing_action
                                    } else {
                                        Res.string.vocabulary_source_analyze_action
                                    },
                                ),
                            )
                        }
                    }
                    if (state.isAnalyzing) {
                        RoundButton(
                            background = MainTheme.colors.common.negativeDialogButton,
                            iconRes = Res.drawable.ic_close_24,
                            onClick = onCancelAnalysis,
                            size = 40.dp,
                            contentDescription = stringResource(resource = Res.string.vocabulary_source_cancel_action),
                        )
                    }
                }
            }

            if (state.displayedItems.isEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(text = stringResource(resource = Res.string.vocabulary_source_items_empty))
                }
            } else {
                item {
                    VocabularySourceItemReviewToolbar(
                        selectedItemCount = state.selectedItemCount,
                        isAllSelected = state.isAllItemsSelected,
                        hasUnsavedChanges = state.hasUnsavedChanges,
                        isLocked = isReviewLocked,
                        onAllItemsSelectionChanged = onAllItemsSelectionChanged,
                        onIgnoreSelectedItems = onIgnoreSelectedItems,
                        onRestoreSelectedItems = onRestoreSelectedItems,
                        onAddSelectedToDeckClick = onAddSelectedToDeckClick,
                    )
                }
                itemsIndexed(
                    items = state.itemHolders,
                    key = { _, holder ->
                        "${holder.item.id}-${holder.sourceIndex}"
                    },
                ) { _, holder ->
                    VocabularySourceResultItem(
                        holder = holder,
                        isLocked = isReviewLocked,
                        onSelectionChanged = { onItemSelectionChanged(holder.sourceIndex) },
                        onEditClick = { onStartItemEditing(holder.sourceIndex) },
                        onLocateClick = {
                            val highlightRanges = holder.item.occurrences.toHighlightRanges(text = state.cleanText)
                            if (highlightRanges.isNotEmpty()) {
                                highlightedTextRanges = highlightRanges
                                textMode = VocabularySourceTextMode.CLEAN
                                locateRequestId += 1
                                locateRequest = VocabularySourceTextLocateRequest(
                                    id = locateRequestId,
                                    startOffset = highlightRanges.first().startOffset,
                                )
                            }
                        },
                        onSpeakClick = { onSpeakItemClick(holder.sourceIndex) },
                        onSpeakTextClick = onSpeakTextClick,
                    )
                }
            }
        }
        if (canSave || state.isSaveConfirmed) {
            VocabularySourceFloatingSaveButton(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = FloatingSaveButtonEdgePadding,
                        bottom = FloatingSaveButtonEdgePadding,
                    ),
                canSave = canSave,
                isSaveConfirmed = state.isSaveConfirmed,
                containerColor = if (canSave) {
                    MainTheme.colors.material.primary
                } else {
                    disabledButtonContainerColor
                },
                contentColor = if (canSave) {
                    MainTheme.colors.material.onPrimary
                } else {
                    disabledButtonContentColor
                },
                onSave = onSave,
            )
        }
    }
}

@Composable
private fun VocabularySourceFloatingSaveButton(
    canSave: Boolean,
    isSaveConfirmed: Boolean,
    containerColor: Color,
    contentColor: Color,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        if (canSave) {
            RoundButton(
                background = containerColor,
                iconRes = Res.drawable.ic_save_24,
                enabled = true,
                onClick = onSave,
                contentDescription = stringResource(resource = Res.string.vocabulary_source_save_action),
                elevation = 4.dp,
                contentColor = contentColor,
            )
        } else {
            Card(
                modifier = Modifier.size(ROUNDED_ELEMENT_SIZE.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MainTheme.colors.vocabularySourceScreen.savedButtonContainer,
                    contentColor = MainTheme.colors.vocabularySourceScreen.disabledTextModeSelectedContent,
                ),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        modifier = Modifier
                            .size(ROUNDED_ELEMENT_SIZE.dp)
                            .padding(8.dp),
                        painter = painterResource(resource = Res.drawable.ic_save_24),
                        contentDescription = stringResource(resource = Res.string.vocabulary_source_save_action),
                    )
                }
            }
        }
        if (isSaveConfirmed && !canSave) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 6.dp, bottom = 6.dp)
                    .size(12.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(percent = 50))
                    .background(color = MainTheme.colors.vocabularySourceScreen.savedDot)
                    .border(
                        width = 1.dp,
                        color = MainTheme.colors.material.surface,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(percent = 50),
                    ),
            )
        }
    }
}

@Composable
private fun VocabularySourceAudioSection(
    modifier: Modifier = Modifier,
    selectedAudioFile: SelectedAudioFile?,
    transcriptionState: TranscriptionUiState,
    onSelectAudioClick: () -> Unit,
    onClearAudioClick: () -> Unit,
    onTranscribeClick: () -> Unit,
    onCancelTranscriptionClick: () -> Unit,
) {
    val isTranscribing = transcriptionState is TranscriptionUiState.Uploading ||
        transcriptionState is TranscriptionUiState.Transcribing

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MainTheme.colors.vocabularySourceScreen.audioContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(resource = Res.string.vocabulary_source_audio_label),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (selectedAudioFile == null) {
                    OutlinedButton(
                        onClick = onSelectAudioClick,
                        enabled = !isTranscribing,
                    ) {
                        Text(text = stringResource(resource = Res.string.vocabulary_source_audio_select_action))
                    }
                }
            }

            if (selectedAudioFile != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedAudioFile.fileName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = formatFileSize(selectedAudioFile.byteSize),
                            style = MaterialTheme.typography.bodySmall,
                            color = MainTheme.colors.material.onSurfaceVariant,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = onSelectAudioClick,
                            enabled = !isTranscribing,
                        ) {
                            Text(text = stringResource(resource = Res.string.vocabulary_source_audio_replace_action))
                        }
                        TextButton(
                            onClick = onClearAudioClick,
                            enabled = !isTranscribing,
                        ) {
                            Text(
                                text = stringResource(resource = Res.string.vocabulary_source_audio_clear_action),
                                color = MainTheme.colors.material.error,
                            )
                        }
                    }
                }

            }

            if (selectedAudioFile != null || isTranscribing || transcriptionState is TranscriptionUiState.Failed) {
                when (transcriptionState) {
                    is TranscriptionUiState.Idle -> {
                        Button(
                            onClick = onTranscribeClick,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(text = stringResource(resource = Res.string.vocabulary_source_transcribe_action))
                        }
                    }
                    is TranscriptionUiState.Uploading -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = "${stringResource(resource = Res.string.vocabulary_source_uploading_action)}: " +
                                        "${formatFileSize(transcriptionState.uploadedBytes)} / ${formatFileSize(transcriptionState.totalBytes)}",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                TextButton(onClick = onCancelTranscriptionClick) {
                                    Text(text = stringResource(resource = Res.string.vocabulary_source_cancel_action))
                                }
                            }
                            val progressFraction = if (transcriptionState.totalBytes > 0L) {
                                (transcriptionState.uploadedBytes.toFloat() / transcriptionState.totalBytes.toFloat()).coerceIn(0f, 1f)
                            } else 0f
                            LinearProgressIndicator(
                                progress = { progressFraction },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    is TranscriptionUiState.Transcribing -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                val chunkText = if (transcriptionState.totalChunks > 0) {
                                    "${stringResource(resource = Res.string.vocabulary_source_transcribing_action)}: " +
                                        "${transcriptionState.completedChunks} / ${transcriptionState.totalChunks}"
                                } else {
                                    stringResource(resource = Res.string.vocabulary_source_transcribing_action)
                                }
                                Text(
                                    text = chunkText,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                TextButton(onClick = onCancelTranscriptionClick) {
                                    Text(text = stringResource(resource = Res.string.vocabulary_source_cancel_action))
                                }
                            }
                            if (transcriptionState.totalChunks > 0) {
                                val chunkFraction = (transcriptionState.completedChunks.toFloat() / transcriptionState.totalChunks.toFloat()).coerceIn(0f, 1f)
                                LinearProgressIndicator(
                                    progress = { chunkFraction },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            } else {
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                    is TranscriptionUiState.Failed -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = transcriptionState.message.ifBlank {
                                    stringResource(resource = Res.string.vocabulary_source_transcription_failed)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MainTheme.colors.material.error,
                            )
                            Button(
                                onClick = onTranscribeClick,
                                modifier = Modifier.fillMaxWidth(),
                                enabled = selectedAudioFile != null,
                            ) {
                                Text(text = stringResource(resource = Res.string.vocabulary_source_transcribe_action))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VocabularySourceTranscriptTextField(
    rawText: String,
    cleanText: String,
    textMode: VocabularySourceTextMode,
    isExpanded: Boolean,
    highlightRanges: List<VocabularySourceTextHighlightRange>,
    locateRequest: VocabularySourceTextLocateRequest?,
    clearSelectionRequestId: Int,
    onTextModeChanged: (VocabularySourceTextMode) -> Unit,
    onExpandedChanged: (Boolean) -> Unit,
    onRawTextChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isOriginalMode = textMode == VocabularySourceTextMode.ORIGINAL
    val isOriginalEnabled = rawText.isNotBlank()
    val isCleanEnabled = cleanText.isNotBlank()
    val hasTextContent = isOriginalEnabled || isCleanEnabled
    val visibleText = if (isOriginalMode) rawText else cleanText
    val focusManager = LocalFocusManager.current
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    var textFieldValue by remember { mutableStateOf(TextFieldValue(text = rawText)) }
    val visualTransformation = if (!isOriginalMode && highlightRanges.isNotEmpty()) {
        VocabularySourceHighlightTransformation(
            ranges = highlightRanges,
            highlightColor = MainTheme.colors.vocabularySourceScreen.occurrenceHighlight,
        )
    } else {
        VisualTransformation.None
    }
    val changeTextMode: (VocabularySourceTextMode) -> Unit = { targetMode ->
        if (targetMode != textMode) {
            focusManager.clearFocus()
            onTextModeChanged(targetMode)
        }
    }

    LaunchedEffect(rawText, cleanText, textMode, locateRequest) {
        val targetSelection = locateRequest
            ?.takeIf { !isOriginalMode && visibleText.isNotBlank() }
            ?.let { request ->
                TextRange(index = request.startOffset.coerceIn(minimumValue = 0, maximumValue = visibleText.length))
            }
            ?: textFieldValue.selection.coerceIn(text = visibleText)

        if (textFieldValue.text != visibleText || textFieldValue.selection != targetSelection) {
            textFieldValue = TextFieldValue(text = visibleText, selection = targetSelection)
        }
    }

    LaunchedEffect(clearSelectionRequestId) {
        if (clearSelectionRequestId > 0) {
            textFieldValue = textFieldValue.copy(selection = TextRange(index = 0))
        }
    }

    LaunchedEffect(locateRequest, isOriginalMode, cleanText) {
        val request = locateRequest ?: return@LaunchedEffect
        if (isOriginalMode || cleanText.isBlank()) return@LaunchedEffect

        bringIntoViewRequester.bringIntoView()
    }

    LaunchedEffect(rawText, cleanText, textMode) {
        when {
            textMode == VocabularySourceTextMode.CLEAN && !isCleanEnabled -> {
                changeTextMode(VocabularySourceTextMode.ORIGINAL)
            }

            textMode == VocabularySourceTextMode.ORIGINAL && !isOriginalEnabled && isCleanEnabled -> {
                changeTextMode(VocabularySourceTextMode.CLEAN)
            }
        }
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VocabularySourceTextModeSwitch(
                textMode = textMode,
                isOriginalEnabled = isOriginalEnabled,
                isCleanEnabled = isCleanEnabled,
                onTextModeChanged = changeTextMode,
            )
            Spacer(modifier = Modifier.weight(1F))
            if (hasTextContent) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(percent = 50),
                    color = MainTheme.colors.vocabularySourceScreen.disabledTextModeSelectedBackground,
                    contentColor = MainTheme.colors.vocabularySourceScreen.disabledTextModeSelectedContent,
                ) {
                    IconButton(
                        modifier = Modifier.size(40.dp),
                        onClick = { onExpandedChanged(!isExpanded) },
                    ) {
                        Icon(
                            modifier = Modifier.size(21.dp),
                            painter = painterResource(
                                resource = if (isExpanded) {
                                    Res.drawable.ic_collapse_vertical_24
                                } else {
                                    Res.drawable.ic_expand_vertical_24
                                },
                            ),
                            contentDescription = stringResource(
                                resource = if (isExpanded) {
                                    Res.string.vocabulary_source_collapse_text_action
                                } else {
                                    Res.string.vocabulary_source_expand_text_action
                                },
                            ),
                        )
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(bringIntoViewRequester),
                value = textFieldValue,
                onValueChange = { updatedTextFieldValue ->
                    textFieldValue = updatedTextFieldValue
                    if (isOriginalMode) {
                        onRawTextChanged(updatedTextFieldValue.text)
                    }
                },
                label = {
                    Text(
                        text = stringResource(
                            resource = if (isOriginalMode) {
                                Res.string.vocabulary_source_raw_text_label
                            } else {
                                Res.string.vocabulary_source_clean_text_label
                            },
                        ),
                    )
                },
                readOnly = !isOriginalMode,
                visualTransformation = visualTransformation,
                minLines = COLLAPSED_TRANSCRIPT_TEXT_LINES,
                maxLines = if (isExpanded) Int.MAX_VALUE else COLLAPSED_TRANSCRIPT_TEXT_LINES,
            )
            Text(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(y = (-7).dp)
                    .padding(end = 12.dp)
                    .background(color = MainTheme.colors.material.background)
                    .padding(horizontal = 4.dp),
                text = stringResource(
                    resource = Res.string.vocabulary_source_character_count,
                    visibleText.length,
                ),
                color = MainTheme.colors.vocabularySourceScreen.secondaryContent,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun VocabularySourceTextModeSwitch(
    textMode: VocabularySourceTextMode,
    isOriginalEnabled: Boolean,
    isCleanEnabled: Boolean,
    onTextModeChanged: (VocabularySourceTextMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isOriginalMode = textMode == VocabularySourceTextMode.ORIGINAL
    val isAnyModeEnabled = isOriginalEnabled || isCleanEnabled
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(percent = 50)

    BoxWithConstraints(
        modifier = modifier
            .width(TranscriptTextModeSwitchWidth)
            .height(TranscriptTextModeSwitchHeight)
            .clip(shape = shape)
            .background(
                color = if (isAnyModeEnabled) {
                    MainTheme.colors.material.surfaceVariant
                } else {
                    MainTheme.colors.vocabularySourceScreen.disabledTextModeBackground
                },
            ),
    ) {
        val selectedOffset by animateDpAsState(
            targetValue = if (isOriginalMode) 0.dp else maxWidth / 2,
            animationSpec = tween(durationMillis = TRANSCRIPT_TEXT_MODE_ANIMATION_DURATION_MS),
            label = "VocabularySourceTextModeOffset",
        )

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(maxWidth / 2)
                .offset(x = selectedOffset)
                .padding(3.dp)
                .clip(shape = shape)
                .background(
                    color = if (isAnyModeEnabled) {
                        MainTheme.colors.material.primary
                    } else {
                        MainTheme.colors.vocabularySourceScreen.disabledTextModeSelectedBackground
                    },
                ),
        )

        Row(modifier = Modifier.fillMaxSize()) {
            TextModeSwitchItem(
                modifier = Modifier
                    .weight(1F)
                    .fillMaxHeight(),
                text = stringResource(resource = Res.string.vocabulary_source_original_text_mode),
                isSelected = isOriginalMode,
                enabled = isOriginalEnabled,
                onClick = { onTextModeChanged(VocabularySourceTextMode.ORIGINAL) },
            )
            TextModeSwitchItem(
                modifier = Modifier
                    .weight(1F)
                    .fillMaxHeight(),
                text = stringResource(resource = Res.string.vocabulary_source_clean_text_label),
                isSelected = !isOriginalMode,
                enabled = isCleanEnabled,
                onClick = { onTextModeChanged(VocabularySourceTextMode.CLEAN) },
            )
        }
    }
}

@Composable
private fun TextModeSwitchItem(
    text: String,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = when {
                !enabled && isSelected -> MainTheme.colors.vocabularySourceScreen.disabledTextModeSelectedContent
                !enabled -> MainTheme.colors.vocabularySourceScreen.disabledTextModeContent
                isSelected -> MainTheme.colors.material.onPrimary
                else -> MainTheme.colors.material.onBackground
            },
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun VocabularySourceItemReviewToolbar(
    selectedItemCount: Int,
    isAllSelected: Boolean,
    hasUnsavedChanges: Boolean,
    isLocked: Boolean,
    onAllItemsSelectionChanged: () -> Unit,
    onIgnoreSelectedItems: () -> Unit,
    onRestoreSelectedItems: () -> Unit,
    onAddSelectedToDeckClick: () -> Unit,
) {
    val hasSelectedItems = selectedItemCount > 0
    val addToDeckColors = if (hasSelectedItems && hasUnsavedChanges) {
        ButtonDefaults.buttonColors(
            containerColor = MainTheme.colors.vocabularySourceScreen.disabledButtonContainer,
            contentColor = MainTheme.colors.vocabularySourceScreen.disabledButtonContent,
        )
    } else {
        ButtonDefaults.buttonColors()
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(
                    resource = Res.string.vocabulary_source_selected_items,
                    selectedItemCount,
                ),
            )
            TextButton(
                enabled = !isLocked,
                onClick = onAllItemsSelectionChanged,
            ) {
                Text(
                    text = stringResource(
                        resource = if (isAllSelected) {
                            Res.string.vocabulary_source_clear_selection_action
                        } else {
                            Res.string.vocabulary_source_select_all_action
                        },
                    ),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                enabled = selectedItemCount > 0 && !isLocked,
                onClick = onIgnoreSelectedItems,
            ) {
                Text(text = stringResource(resource = Res.string.vocabulary_source_ignore_action))
            }
            OutlinedButton(
                enabled = selectedItemCount > 0 && !isLocked,
                onClick = onRestoreSelectedItems,
            ) {
                Text(text = stringResource(resource = Res.string.vocabulary_source_restore_action))
            }
            Button(
                enabled = hasSelectedItems && !isLocked,
                colors = addToDeckColors,
                onClick = onAddSelectedToDeckClick,
            ) {
                Text(text = stringResource(resource = Res.string.vocabulary_source_add_to_deck_action))
            }
        }
    }
}

@Composable
private fun VocabularySourceResultItem(
    holder: SelectableVocabularySourceItemHolder,
    isLocked: Boolean,
    onSelectionChanged: () -> Unit,
    onEditClick: () -> Unit,
    onLocateClick: () -> Unit,
    onSpeakClick: () -> Unit,
    onSpeakTextClick: (String) -> Unit,
) {
    val item = holder.item
    val isVisuallyDisabled = isLocked || item.status == VocabularySourceItemStatus.IGNORED
    val isLocateEnabled = !isLocked && item.occurrences.isNotEmpty()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isVisuallyDisabled) 0.55F else 1F),
        colors = CardDefaults.cardColors(
            containerColor = MainTheme.colors.vocabularySourceScreen.itemContainer,
            contentColor = MainTheme.colors.material.onBackground,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.padding(top = 2.dp, end = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                CustomCheckBox(
                    modifier = Modifier,
                    checked = holder.isSelected,
                    enabled = !isLocked,
                    onCheckedChange = { onSelectionChanged() },
                )
                IconButton(
                    modifier = Modifier.size(40.dp),
                    enabled = !isLocked,
                    onClick = onEditClick,
                ) {
                    VocabularySourceEditIcon(isEdited = item.isEdited)
                }
                IconButton(
                    modifier = Modifier.size(40.dp),
                    enabled = isLocateEnabled,
                    onClick = onLocateClick,
                ) {
                    Icon(
                        modifier = Modifier.size(21.dp),
                        painter = painterResource(resource = Res.drawable.ic_target_24),
                        contentDescription = stringResource(
                            resource = Res.string.vocabulary_source_locate_occurrences_action,
                        ),
                        tint = if (isLocateEnabled) {
                            MainTheme.colors.vocabularySourceScreen.icon
                        } else {
                            MainTheme.colors.vocabularySourceScreen.disabledIcon
                        },
                    )
                }
            }
            VocabularySourceItemBody(
                modifier = Modifier.weight(1F),
                item = item,
                isLocked = isLocked,
                onSpeakClick = onSpeakClick,
                onSpeakTextClick = onSpeakTextClick,
            )
        }
    }
}

@Composable
private fun VocabularySourceEditIcon(isEdited: Boolean) {
    Box(
        modifier = Modifier.size(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            modifier = Modifier.size(20.dp),
            painter = painterResource(resource = Res.drawable.ic_edit_24),
            contentDescription = stringResource(resource = Res.string.vocabulary_source_edit_action),
            tint = MainTheme.colors.vocabularySourceScreen.icon,
        )
        if (isEdited) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 1.dp, bottom = 1.dp)
                    .size(8.dp)
                    .clip(shape = androidx.compose.foundation.shape.RoundedCornerShape(percent = 50))
                    .background(color = MainTheme.colors.vocabularySourceScreen.editedDot)
                    .border(
                        width = 1.dp,
                        color = MainTheme.colors.vocabularySourceScreen.markerBorder,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(percent = 50),
                    ),
            )
        }
    }
}

@Composable
private fun VocabularySourceItemBody(
    item: VocabularySourceItem,
    isLocked: Boolean,
    onSpeakClick: () -> Unit,
    onSpeakTextClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1F)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    VocabularySourceItemTitle(
                        modifier = Modifier.weight(1F),
                        foreignWord = item.foreignWord,
                        transcription = item.transcription,
                        isLocked = isLocked,
                        onSpeakClick = onSpeakClick,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        item.cefrLevel?.let { cefrLevel ->
                            VocabularySourceCefrBadge(level = cefrLevel)
                        }
                        VocabularySourcePartOfSpeechBadge(partOfSpeech = item.partOfSpeech)
                        VocabularySourceConfidenceIndicator(confidence = item.confidence)
                    }
                }
                Text(
                    text = item.nativeWord,
                    color = MainTheme.colors.material.onBackground,
                    fontSize = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        VocabularySourceItemBadgeRows(item = item)

        if (item.originalText.isNotBlank() && item.originalText != item.foreignWord) {
            VocabularySourceItemLabeledText(
                label = "Used as",
                value = item.originalText,
                maxLines = 2,
            )
        }
        if (item.sourceExample.isNotBlank()) {
            VocabularySourceItemLabeledText(
                label = "Example",
                value = item.sourceExample,
                maxLines = 3,
                isLocked = isLocked,
                onSpeakClick = { onSpeakTextClick(item.sourceExample) },
            )
        }
        if (item.explanation.isNotBlank()) {
            VocabularySourceItemLabeledText(
                label = "Explanation",
                value = item.explanation,
                maxLines = 4,
                isLocked = isLocked,
                onSpeakClick = { onSpeakTextClick(item.explanation) },
            )
        }
        if (item.knownMeaningsSnapshot.isNotBlank()) {
            VocabularySourceItemLabeledText(
                label = "Known",
                value = item.knownMeaningsSnapshot,
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun VocabularySourceItemTitle(
    foreignWord: String,
    transcription: String,
    isLocked: Boolean,
    onSpeakClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = foreignWord,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (transcription.isNotBlank()) {
                Text(
                    text = "[",
                    color = MainTheme.colors.material.primary,
                    fontSize = 14.sp,
                )
                Text(
                    text = transcription,
                    color = MainTheme.colors.vocabularySourceScreen.secondaryContent,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "]",
                    color = MainTheme.colors.material.primary,
                    fontSize = 14.sp,
                )
            }
            IconButton(
                modifier = Modifier.size(32.dp),
                enabled = !isLocked,
                onClick = onSpeakClick,
            ) {
                Icon(
                    modifier = Modifier.size(19.dp),
                    painter = painterResource(resource = Res.drawable.ic_baseline_volume_up_24),
                    contentDescription = stringResource(
                        resource = Res.string.vocabulary_source_speak_word_action,
                    ),
                    tint = MainTheme.colors.vocabularySourceScreen.icon,
                )
            }
        }
    }
}

@Composable
private fun VocabularySourceItemBadgeRows(item: VocabularySourceItem) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            VocabularySourceItemBadge(
                modifier = Modifier.weight(1F),
                text = item.category.toDisplayLabel(),
                containerColor = item.category.toBadgeContainerColor(),
                contentColor = item.category.toBadgeContentColor(),
            )
        }
        if (item.alreadyExists) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                VocabularySourceItemBadge(
                    text = "KNOWN",
                )
            }
        }
    }
}

@Composable
private fun VocabularySourceConfidenceIndicator(confidence: VocabularySourceItemConfidence) {
    val iconResource = when (confidence) {
        VocabularySourceItemConfidence.LOW -> Res.drawable.ic_confidence_low_24
        VocabularySourceItemConfidence.MEDIUM -> Res.drawable.ic_confidence_medium_24
        VocabularySourceItemConfidence.HIGH -> Res.drawable.ic_confidence_high_24
    }

    Icon(
        modifier = Modifier.size(18.dp),
        painter = painterResource(resource = iconResource),
        contentDescription = null,
        tint = MainTheme.colors.vocabularySourceScreen.icon,
    )
}

@Composable
private fun VocabularySourcePartOfSpeechBadge(partOfSpeech: VocabularySourceItemPartOfSpeech) {
    Box(
        modifier = Modifier
            .background(
                color = MainTheme.colors.vocabularySourceScreen.badgeContainer,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = partOfSpeech.toDisplayLabel(),
            color = MainTheme.colors.material.onBackground,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun VocabularySourceCefrBadge(level: CefrLevel) {
    Box(
        modifier = Modifier
            .background(
                color = level.toVocabularySourceCefrBadgeColor(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = level.name,
            color = MainTheme.colors.vocabularySourceScreen.cefrBadgeContent,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun VocabularySourceItemBadge(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MainTheme.colors.vocabularySourceScreen.badgeContainer,
    contentColor: Color = MainTheme.colors.material.onBackground,
) {
    Surface(
        modifier = modifier.height(28.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
        color = containerColor,
        contentColor = contentColor,
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun VocabularySourceItemLabeledText(
    label: String,
    value: String,
    maxLines: Int,
    isLocked: Boolean = false,
    onSpeakClick: (() -> Unit)? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            color = MainTheme.colors.vocabularySourceScreen.exampleContent,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                modifier = Modifier.weight(1F),
                text = value,
                color = MainTheme.colors.material.onBackground,
                fontSize = 14.sp,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis,
            )
            onSpeakClick?.let { notNullOnSpeakClick ->
                IconButton(
                    modifier = Modifier.size(30.dp),
                    enabled = !isLocked,
                    onClick = notNullOnSpeakClick,
                ) {
                    Icon(
                        modifier = Modifier.size(18.dp),
                        painter = painterResource(resource = Res.drawable.ic_baseline_volume_up_24),
                        contentDescription = stringResource(
                            resource = Res.string.vocabulary_source_speak_word_action,
                        ),
                        tint = MainTheme.colors.vocabularySourceScreen.icon,
                    )
                }
            }
        }
    }
}

@Composable
private fun VocabularySourceItemEditDialog(
    editState: VocabularySourceItemEditState,
    onForeignWordChanged: (String) -> Unit,
    onNativeWordChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(text = stringResource(resource = Res.string.vocabulary_source_edit_item_title))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = editState.foreignWord,
                    onValueChange = onForeignWordChanged,
                    label = {
                        Text(text = stringResource(resource = Res.string.label_foreign_word))
                    },
                    singleLine = true,
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = editState.nativeWord,
                    onValueChange = onNativeWordChanged,
                    label = {
                        Text(text = stringResource(resource = Res.string.label_native_word))
                    },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = editState.foreignWord.isNotBlank() && editState.nativeWord.isNotBlank(),
                onClick = onConfirm,
            ) {
                Text(text = stringResource(resource = Res.string.vocabulary_source_save_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(text = stringResource(resource = Res.string.vocabulary_source_cancel_action))
            }
        },
    )
}

@Composable
private fun VocabularySourceDeckChooserDialog(
    decks: List<Deck>,
    isNewDeckCreationVisible: Boolean,
    newDeckName: String,
    isAddingToDeck: Boolean,
    onDeckSelected: (Deck) -> Unit,
    onNewDeckClick: () -> Unit,
    onNewDeckNameChanged: (String) -> Unit,
    onNewDeckConfirm: () -> Unit,
    onNewDeckCancel: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(text = stringResource(resource = Res.string.vocabulary_source_target_deck_title))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isNewDeckCreationVisible) {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = newDeckName,
                        enabled = !isAddingToDeck,
                        onValueChange = onNewDeckNameChanged,
                        label = {
                            Text(text = stringResource(resource = Res.string.vocabulary_source_new_deck_name_label))
                        },
                        singleLine = true,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            enabled = newDeckName.isNotBlank() && !isAddingToDeck,
                            onClick = onNewDeckConfirm,
                        ) {
                            Text(text = stringResource(resource = Res.string.vocabulary_source_add_to_deck_action))
                        }
                        TextButton(
                            enabled = !isAddingToDeck,
                            onClick = onNewDeckCancel,
                        ) {
                            Text(text = stringResource(resource = Res.string.vocabulary_source_cancel_action))
                        }
                    }
                } else {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isAddingToDeck,
                        onClick = onNewDeckClick,
                    ) {
                        Text(text = stringResource(resource = Res.string.vocabulary_source_new_deck_action))
                    }
                }

                if (decks.isEmpty()) {
                    Text(text = stringResource(resource = Res.string.vocabulary_source_no_decks_available))
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = TargetDeckChooserListMaxHeight),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(
                            items = decks,
                            key = Deck::id,
                        ) { deck ->
                            OutlinedButton(
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isAddingToDeck,
                                onClick = { onDeckSelected(deck) },
                            ) {
                                Text(
                                    text = deck.name,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                enabled = !isAddingToDeck,
                onClick = onCancel,
            ) {
                Text(text = stringResource(resource = Res.string.vocabulary_source_cancel_action))
            }
        },
    )
}

@Preview(name = "Light", showBackground = true, backgroundColor = LightThemePreviewBackground)
@Composable
private fun VocabularySourceDetailContentPreview() {
    VocabularySourceDetailContentPreviewContent(darkTheme = false)
}

@Preview(name = "Dark", showBackground = true, backgroundColor = DarkThemePreviewBackground)
@Composable
private fun VocabularySourceDetailContentDarkPreview() {
    VocabularySourceDetailContentPreviewContent(darkTheme = true)
}

@Preview(name = "Items Light", showBackground = true, backgroundColor = LightThemePreviewBackground)
@Composable
private fun VocabularySourceItemsPreview() {
    VocabularySourceItemsPreviewContent(darkTheme = false)
}

@Preview(name = "Items Dark", showBackground = true, backgroundColor = DarkThemePreviewBackground)
@Composable
private fun VocabularySourceItemsDarkPreview() {
    VocabularySourceItemsPreviewContent(darkTheme = true)
}

@Composable
private fun VocabularySourceDetailContentPreviewContent(darkTheme: Boolean) {
    MainTheme(darkTheme = darkTheme) {
        Surface {
            VocabularySourceDetailContent(
                state = vocabularySourceDetailPreviewState(),
                onTitleChanged = {},
                onDescriptionChanged = {},
                onUrlChanged = {},
                onSelectAudioClick = {},
                onClearAudioFile = {},
                onTranscribeAudio = {},
                onCancelTranscription = {},
                onRawTextChanged = {},
                onSave = {},
                onAnalyze = {},
                onCancelAnalysis = {},
                onItemSelectionChanged = {},
                onAllItemsSelectionChanged = {},
                onIgnoreSelectedItems = {},
                onRestoreSelectedItems = {},
                onStartItemEditing = {},
                onEditingForeignWordChanged = {},
                onEditingNativeWordChanged = {},
                onApplyItemEditing = {},
                onCancelItemEditing = {},
                onAddSelectedToDeckClick = {},
                onDeckSelected = {},
                onCancelDeckChoosing = {},
                onNewDeckClick = {},
                onNewDeckNameChanged = {},
                onNewDeckConfirm = {},
                onNewDeckCancel = {},
                onSpeakItemClick = {},
                onSpeakTextClick = {},
            )
        }
    }
}

@Composable
private fun VocabularySourceItemsPreviewContent(darkTheme: Boolean) {
    MainTheme(darkTheme = darkTheme) {
        Surface {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                vocabularySourcePreviewItems().forEachIndexed { index, item ->
                    VocabularySourceResultItem(
                        holder = SelectableVocabularySourceItemHolder(
                            item = item,
                            sourceIndex = index,
                            isSelected = index == 0,
                        ),
                        isLocked = false,
                        onSelectionChanged = {},
                        onEditClick = {},
                        onLocateClick = {},
                        onSpeakClick = {},
                        onSpeakTextClick = {},
                    )
                }
            }
        }
    }
}

@Preview(name = "Edit Item Dialog Light", showBackground = true, backgroundColor = LightThemePreviewBackground)
@Composable
private fun VocabularySourceItemEditDialogPreview() {
    VocabularySourceItemEditDialogPreviewContent(darkTheme = false)
}

@Preview(name = "Edit Item Dialog Dark", showBackground = true, backgroundColor = DarkThemePreviewBackground)
@Composable
private fun VocabularySourceItemEditDialogDarkPreview() {
    VocabularySourceItemEditDialogPreviewContent(darkTheme = true)
}

@Composable
private fun VocabularySourceItemEditDialogPreviewContent(darkTheme: Boolean) {
    MainTheme(darkTheme = darkTheme) {
        VocabularySourceItemEditDialog(
            editState = VocabularySourceItemEditState(
                index = 0,
                foreignWord = "turn on",
                nativeWord = "включать",
            ),
            onForeignWordChanged = {},
            onNativeWordChanged = {},
            onConfirm = {},
            onCancel = {},
        )
    }
}

@Preview(name = "Deck Chooser Dialog Light", showBackground = true, backgroundColor = LightThemePreviewBackground)
@Composable
private fun VocabularySourceDeckChooserDialogPreview() {
    VocabularySourceDeckChooserDialogPreviewContent(darkTheme = false)
}

@Preview(name = "Deck Chooser Dialog Dark", showBackground = true, backgroundColor = DarkThemePreviewBackground)
@Composable
private fun VocabularySourceDeckChooserDialogDarkPreview() {
    VocabularySourceDeckChooserDialogPreviewContent(darkTheme = true)
}

@Composable
private fun VocabularySourceDeckChooserDialogPreviewContent(darkTheme: Boolean) {
    MainTheme(darkTheme = darkTheme) {
        VocabularySourceDeckChooserDialog(
            decks = vocabularySourcePreviewDecks(),
            isNewDeckCreationVisible = true,
            newDeckName = "Severance",
            isAddingToDeck = false,
            onDeckSelected = {},
            onNewDeckClick = {},
            onNewDeckNameChanged = {},
            onNewDeckConfirm = {},
            onNewDeckCancel = {},
            onCancel = {},
        )
    }
}
