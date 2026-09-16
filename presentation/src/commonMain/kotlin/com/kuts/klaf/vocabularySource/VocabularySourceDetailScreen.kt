package com.kuts.klaf.vocabularySource

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
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
import com.kuts.klaf.common.RoundButton
import com.kuts.klaf.navigation.CollectFlowWithLifecycle
import com.kuts.klaf.presentation.resources.Res
import com.kuts.klaf.presentation.resources.ic_app_labale
import com.kuts.klaf.presentation.resources.ic_collapse_vertical_24
import com.kuts.klaf.presentation.resources.ic_confidence_high_24
import com.kuts.klaf.presentation.resources.ic_confidence_low_24
import com.kuts.klaf.presentation.resources.ic_confidence_medium_24
import com.kuts.klaf.presentation.resources.ic_expand_vertical_24
import com.kuts.klaf.presentation.resources.ic_save_24
import com.kuts.klaf.presentation.resources.label_foreign_word
import com.kuts.klaf.presentation.resources.label_native_word
import com.kuts.klaf.presentation.resources.vocabulary_source_analyze_action
import com.kuts.klaf.presentation.resources.vocabulary_source_add_to_deck_action
import com.kuts.klaf.presentation.resources.vocabulary_source_cancel_action
import com.kuts.klaf.presentation.resources.vocabulary_source_clean_text_label
import com.kuts.klaf.presentation.resources.vocabulary_source_clear_selection_action
import com.kuts.klaf.presentation.resources.vocabulary_source_collapse_text_action
import com.kuts.klaf.presentation.resources.vocabulary_source_description_label
import com.kuts.klaf.presentation.resources.vocabulary_source_edit_action
import com.kuts.klaf.presentation.resources.vocabulary_source_edit_item_title
import com.kuts.klaf.presentation.resources.vocabulary_source_expand_text_action
import com.kuts.klaf.presentation.resources.vocabulary_source_ignore_action
import com.kuts.klaf.presentation.resources.vocabulary_source_items_empty
import com.kuts.klaf.presentation.resources.vocabulary_source_new_deck_action
import com.kuts.klaf.presentation.resources.vocabulary_source_new_deck_name_label
import com.kuts.klaf.presentation.resources.vocabulary_source_no_decks_available
import com.kuts.klaf.presentation.resources.vocabulary_source_original_text_mode
import com.kuts.klaf.presentation.resources.vocabulary_source_raw_text_label
import com.kuts.klaf.presentation.resources.vocabulary_source_restore_action
import com.kuts.klaf.presentation.resources.vocabulary_source_save_action
import com.kuts.klaf.presentation.resources.vocabulary_source_select_all_action
import com.kuts.klaf.presentation.resources.vocabulary_source_selected_items
import com.kuts.klaf.presentation.resources.vocabulary_source_stale_items_warning
import com.kuts.klaf.presentation.resources.vocabulary_source_target_deck_title
import com.kuts.klaf.presentation.resources.vocabulary_source_title_label
import com.kuts.klaf.theme.MainTheme
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private const val COLLAPSED_TRANSCRIPT_TEXT_LINES = 10
private const val TRANSCRIPT_TEXT_MODE_ANIMATION_DURATION_MS = 260
private val TranscriptTextModeSwitchWidth = 260.dp
private val TranscriptTextModeSwitchHeight = 44.dp
private val DisabledTextModeSwitchBackground = Color(0xFFE4E8EA)
private val DisabledTextModeSwitchContent = Color(0xFF8A9499)
private val VocabularySourceItemCardBackgroundAlpha = 0.14F
private val VocabularySourceItemBadgeBackgroundAlpha = 0.28F
private val CardLinkUnlinkedDotColor = Color(0xFFE53935)
private val CardLinkInterimDotColor = Color(0xFFFFA000)
private val CardLinkNormalDeckDotColor = Color(0xFF43A047)
private val SaveConfirmedButtonColor = Color(0xFF43A047)

private enum class VocabularySourceTextMode {
    ORIGINAL,
    CLEAN,
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

    CollectFlowWithLifecycle(flow = viewModel.eventMessage, onEach = sharedViewModel::notify)

    Surface {
        VocabularySourceDetailContent(
            state = viewModel.state.collectAsState().value,
            onBack = { navController.popBackStack() },
            onTitleChanged = viewModel::onTitleChanged,
            onDescriptionChanged = viewModel::onDescriptionChanged,
            onRawTextChanged = viewModel::onRawTextChanged,
            onSave = viewModel::save,
            onAnalyze = viewModel::analyze,
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
        )
    }
}

@Composable
private fun VocabularySourceDetailContent(
    state: VocabularySourceDetailState,
    onBack: () -> Unit,
    onTitleChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onRawTextChanged: (String) -> Unit,
    onSave: () -> Unit,
    onAnalyze: () -> Unit,
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
) {
    var textMode by rememberSaveable { mutableStateOf(VocabularySourceTextMode.ORIGINAL) }
    var isTranscriptExpanded by rememberSaveable { mutableStateOf(false) }

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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            TextButton(onClick = onBack) {
                Text(text = "Back")
            }
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
            VocabularySourceTranscriptTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                rawText = state.rawText,
                cleanText = state.cleanText,
                textMode = textMode,
                isExpanded = isTranscriptExpanded,
                onTextModeChanged = { textMode = it },
                onExpandedChanged = { isTranscriptExpanded = it },
                onRawTextChanged = onRawTextChanged,
            )
            if (state.hasStaleItems) {
                Text(text = stringResource(resource = Res.string.vocabulary_source_stale_items_warning))
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RoundButton(
                    background = if (state.isSaveConfirmed) {
                        SaveConfirmedButtonColor
                    } else {
                        MainTheme.colors.material.primary
                    },
                    iconRes = Res.drawable.ic_save_24,
                    onClick = onSave,
                    contentDescription = stringResource(resource = Res.string.vocabulary_source_save_action),
                    elevation = 4.dp,
                )
                Button(
                    enabled = state.cleanText.isNotBlank() && !state.isAnalyzing,
                    onClick = onAnalyze,
                ) {
                    if (state.isAnalyzing) {
                        CircularProgressIndicator()
                    } else {
                        Text(text = stringResource(resource = Res.string.vocabulary_source_analyze_action))
                    }
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
                    onAllItemsSelectionChanged = onAllItemsSelectionChanged,
                    onIgnoreSelectedItems = onIgnoreSelectedItems,
                    onRestoreSelectedItems = onRestoreSelectedItems,
                    onAddSelectedToDeckClick = onAddSelectedToDeckClick,
                )
            }
            itemsIndexed(
                items = state.itemHolders,
                key = { _, holder ->
                    "${holder.item.id}-${holder.item.foreignWord}-${holder.item.firstOccurrenceOrder}"
                },
            ) { index, holder ->
                VocabularySourceResultItem(
                    holder = holder,
                    onSelectionChanged = { onItemSelectionChanged(index) },
                    onEditClick = { onStartItemEditing(index) },
                )
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
    onTextModeChanged: (VocabularySourceTextMode) -> Unit,
    onExpandedChanged: (Boolean) -> Unit,
    onRawTextChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isOriginalMode = textMode == VocabularySourceTextMode.ORIGINAL
    val isOriginalEnabled = rawText.isNotBlank()
    val isCleanEnabled = cleanText.isNotBlank()
    val focusManager = LocalFocusManager.current
    val changeTextMode: (VocabularySourceTextMode) -> Unit = { targetMode ->
        if (targetMode != textMode) {
            focusManager.clearFocus()
            onTextModeChanged(targetMode)
        }
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
        VocabularySourceTextModeSwitch(
            textMode = textMode,
            isOriginalEnabled = isOriginalEnabled,
            isCleanEnabled = isCleanEnabled,
            onTextModeChanged = changeTextMode,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        ) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = if (isOriginalMode) rawText else cleanText,
                onValueChange = { text ->
                    if (isOriginalMode) {
                        onRawTextChanged(text)
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
                minLines = COLLAPSED_TRANSCRIPT_TEXT_LINES,
                maxLines = if (isExpanded) Int.MAX_VALUE else COLLAPSED_TRANSCRIPT_TEXT_LINES,
            )
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp)
                    .size(40.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(percent = 50),
                color = MainTheme.colors.material.surface.copy(alpha = 0.92F),
                contentColor = MainTheme.colors.material.onBackground,
            ) {
                IconButton(
                    modifier = Modifier.size(40.dp),
                    onClick = { onExpandedChanged(!isExpanded) },
                ) {
                    Icon(
                        modifier = Modifier.size(22.dp),
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
                    DisabledTextModeSwitchBackground
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
                        DisabledTextModeSwitchContent
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
                !enabled && isSelected -> MainTheme.colors.material.onPrimary.copy(alpha = 0.68F)
                !enabled -> DisabledTextModeSwitchContent
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
    onAllItemsSelectionChanged: () -> Unit,
    onIgnoreSelectedItems: () -> Unit,
    onRestoreSelectedItems: () -> Unit,
    onAddSelectedToDeckClick: () -> Unit,
) {
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
            TextButton(onClick = onAllItemsSelectionChanged) {
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
                enabled = selectedItemCount > 0,
                onClick = onIgnoreSelectedItems,
            ) {
                Text(text = stringResource(resource = Res.string.vocabulary_source_ignore_action))
            }
            OutlinedButton(
                enabled = selectedItemCount > 0,
                onClick = onRestoreSelectedItems,
            ) {
                Text(text = stringResource(resource = Res.string.vocabulary_source_restore_action))
            }
            Button(
                enabled = selectedItemCount > 0,
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
    onSelectionChanged: () -> Unit,
    onEditClick: () -> Unit,
) {
    val item = holder.item

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (item.status == VocabularySourceItemStatus.IGNORED) 0.55F else 1F),
        colors = CardDefaults.cardColors(
            containerColor = MainTheme.colors.material.surfaceVariant.copy(
                alpha = VocabularySourceItemCardBackgroundAlpha,
            ),
            contentColor = MainTheme.colors.material.onBackground,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            CustomCheckBox(
                modifier = Modifier.padding(top = 2.dp, end = 10.dp),
                checked = holder.isSelected,
                onCheckedChange = { onSelectionChanged() },
            )
            VocabularySourceItemBody(
                modifier = Modifier.weight(1F),
                item = item,
                onEditClick = onEditClick,
            )
        }
    }
}

@Composable
private fun VocabularySourceItemBody(
    item: VocabularySourceItem,
    onEditClick: () -> Unit,
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        modifier = Modifier.weight(weight = 1F, fill = false),
                        text = item.foreignWord,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    item.cefrLevel?.let { cefrLevel ->
                        VocabularySourceCefrBadge(level = cefrLevel)
                    }
                    VocabularySourcePartOfSpeechBadge(partOfSpeech = item.partOfSpeech)
                    VocabularySourceConfidenceIndicator(confidence = item.confidence)
                }
                Text(
                    text = item.nativeWord,
                    color = MainTheme.colors.material.onBackground,
                    fontSize = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                VocabularySourceCardLinkIndicator(item = item)
                TextButton(onClick = onEditClick) {
                    Text(text = stringResource(resource = Res.string.vocabulary_source_edit_action))
                }
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
            )
        }
        if (item.explanation.isNotBlank()) {
            VocabularySourceItemLabeledText(
                label = "Explanation",
                value = item.explanation,
                maxLines = 4,
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
private fun VocabularySourceItemBadgeRows(item: VocabularySourceItem) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            VocabularySourceItemBadge(
                modifier = Modifier.weight(1F),
                text = item.category.toDisplayLabel(),
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
private fun VocabularySourceCardLinkIndicator(item: VocabularySourceItem) {
    val targetDeckId = item.targetDeckId
    val dotColor = when {
        item.createdCardId == null || targetDeckId == null -> CardLinkUnlinkedDotColor
        targetDeckId == Deck.INTERIM_DECK_ID -> CardLinkInterimDotColor
        else -> CardLinkNormalDeckDotColor
    }
    val dotShape = androidx.compose.foundation.shape.RoundedCornerShape(percent = 50)
    val isIgnored = item.status == VocabularySourceItemStatus.IGNORED

    Box(
        modifier = Modifier.size(30.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            modifier = Modifier.size(23.dp),
            painter = painterResource(resource = Res.drawable.ic_app_labale),
            contentDescription = null,
            tint = MainTheme.colors.material.onBackground.copy(
                alpha = if (isIgnored) 0.28F else 0.72F,
            ),
        )
        if (!isIgnored) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(12.dp)
                    .clip(shape = dotShape)
                    .background(color = dotColor)
                    .border(
                        width = 1.dp,
                        color = MainTheme.colors.material.surface.copy(alpha = 0.96F),
                        shape = dotShape,
                    ),
            )
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
        tint = MainTheme.colors.material.onBackground.copy(alpha = 0.72F),
    )
}

@Composable
private fun VocabularySourcePartOfSpeechBadge(partOfSpeech: VocabularySourceItemPartOfSpeech) {
    Box(
        modifier = Modifier
            .background(
                color = MainTheme.colors.material.surfaceVariant.copy(
                    alpha = VocabularySourceItemBadgeBackgroundAlpha,
                ),
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
    val bottomSheetColors = MainTheme.colors.wordInsightsBottomSheet

    Box(
        modifier = Modifier
            .background(
                color = level.toVocabularySourceCefrBadgeColor().copy(alpha = 0.88F),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = level.name,
            color = bottomSheetColors.levelBadgeContent,
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
) {
    Surface(
        modifier = modifier.height(28.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
        color = MainTheme.colors.material.surfaceVariant.copy(
            alpha = VocabularySourceItemBadgeBackgroundAlpha,
        ),
        contentColor = MainTheme.colors.material.onBackground,
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
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            color = MainTheme.colors.material.onBackground.copy(alpha = 0.7F),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = value,
            color = MainTheme.colors.material.onBackground,
            fontSize = 14.sp,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun VocabularySourceItemCategory.toDisplayLabel(): String {
    return when (this) {
        VocabularySourceItemCategory.NEW -> "NEW"
        VocabularySourceItemCategory.POSSIBLE_NEW_MEANING -> "NEW MEANING"
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
private fun CefrLevel.toVocabularySourceCefrBadgeColor(): Color {
    val bottomSheetColors = MainTheme.colors.wordInsightsBottomSheet

    return when (this) {
        CefrLevel.A1 -> bottomSheetColors.levelA1BadgeBackground
        CefrLevel.A2 -> bottomSheetColors.levelA2BadgeBackground
        CefrLevel.B1 -> bottomSheetColors.levelB1BadgeBackground
        CefrLevel.B2 -> bottomSheetColors.levelB2BadgeBackground
        CefrLevel.C1 -> bottomSheetColors.levelC1BadgeBackground
        CefrLevel.C2 -> bottomSheetColors.levelC2BadgeBackground
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
                    decks.forEach { deck ->
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

@Preview(name = "Light", showBackground = true, backgroundColor = 0xFFF4F0E8)
@Composable
private fun VocabularySourceDetailContentPreview() {
    VocabularySourceDetailContentPreviewContent(darkTheme = false)
}

@Preview(name = "Dark", showBackground = true, backgroundColor = 0xFF1B1B1F)
@Composable
private fun VocabularySourceDetailContentDarkPreview() {
    VocabularySourceDetailContentPreviewContent(darkTheme = true)
}

@Preview(name = "Items Light", showBackground = true, backgroundColor = 0xFFF4F0E8)
@Composable
private fun VocabularySourceItemsPreview() {
    VocabularySourceItemsPreviewContent(darkTheme = false)
}

@Preview(name = "Items Dark", showBackground = true, backgroundColor = 0xFF1B1B1F)
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
                onBack = {},
                onTitleChanged = {},
                onDescriptionChanged = {},
                onRawTextChanged = {},
                onSave = {},
                onAnalyze = {},
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
                            isSelected = index == 0,
                        ),
                        onSelectionChanged = {},
                        onEditClick = {},
                    )
                }
            }
        }
    }
}

@Preview(name = "Edit Item Dialog", showBackground = true, backgroundColor = 0xFFF4F0E8)
@Composable
private fun VocabularySourceItemEditDialogPreview() {
    MainTheme(darkTheme = false) {
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

@Preview(name = "Deck Chooser Dialog", showBackground = true, backgroundColor = 0xFFF4F0E8)
@Composable
private fun VocabularySourceDeckChooserDialogPreview() {
    MainTheme(darkTheme = false) {
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
