package com.kuts.klaf.vocabularySource

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import com.kuts.domain.entities.VocabularySource
import com.kuts.klaf.common.BaseMainViewModel
import com.kuts.klaf.common.RoundButton
import com.kuts.klaf.navigation.AppDestination
import com.kuts.klaf.navigation.CollectFlowWithLifecycle
import com.kuts.klaf.presentation.resources.Res
import com.kuts.klaf.presentation.resources.ic_add_24
import com.kuts.klaf.presentation.resources.vocabulary_source_cancel_action
import com.kuts.klaf.presentation.resources.vocabulary_source_delete_action
import com.kuts.klaf.presentation.resources.vocabulary_source_delete_question
import com.kuts.klaf.presentation.resources.vocabulary_sources_empty
import com.kuts.klaf.presentation.resources.vocabulary_sources_title
import com.kuts.klaf.theme.MainTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun VocabularySourceListScreen(
    navController: NavHostController,
    backStackEntry: NavBackStackEntry,
    sharedViewModel: BaseMainViewModel,
) {
    val viewModel: VocabularySourceListViewModel = koinViewModel(viewModelStoreOwner = backStackEntry)
    var sourceForDeleting by remember { mutableStateOf<VocabularySource?>(null) }

    sourceForDeleting?.let { source ->
        VocabularySourceDeleteConfirmationDialog(
            source = source,
            onConfirm = {
                sourceForDeleting = null
                viewModel.deleteSource(sourceId = source.id)
            },
            onCancel = { sourceForDeleting = null },
        )
    }

    CollectFlowWithLifecycle(flow = viewModel.eventMessage, onEach = sharedViewModel::notify)

    Surface {
        VocabularySourceListContent(
            holders = viewModel.sourceHolders.collectAsState().value,
            onCreateClick = {
                navController.navigate(route = AppDestination.VocabularySourceCreationDialog)
            },
            onSourceClick = { holder ->
                navController.navigate(route = AppDestination.VocabularySourceDetail(sourceId = holder.source.id))
            },
            onSourceLongClick = { holder ->
                sourceForDeleting = holder.source
            },
        )
    }
}

@Preview(name = "Light", showBackground = true, backgroundColor = 0xFFF4F0E8)
@Composable
private fun VocabularySourceListContentPreview() {
    VocabularySourceListContentPreviewContent(darkTheme = false)
}

@Preview(name = "Dark", showBackground = true, backgroundColor = 0xFF1B1B1F)
@Composable
private fun VocabularySourceListContentDarkPreview() {
    VocabularySourceListContentPreviewContent(darkTheme = true)
}

@Composable
private fun VocabularySourceListContentPreviewContent(darkTheme: Boolean) {
    MainTheme(darkTheme = darkTheme) {
        Surface {
            VocabularySourceListContent(
                holders = vocabularySourceListPreviewHolders(),
                onCreateClick = {},
                onSourceClick = {},
                onSourceLongClick = {},
            )
        }
    }
}

@Preview(name = "Delete Dialog", showBackground = true, backgroundColor = 0xFFF4F0E8)
@Composable
private fun VocabularySourceDeleteConfirmationDialogPreview() {
    MainTheme(darkTheme = false) {
        VocabularySourceDeleteConfirmationDialog(
            source = vocabularySourcePreviewSource(),
            onConfirm = {},
            onCancel = {},
        )
    }
}

private fun vocabularySourceListPreviewHolders(): List<VocabularySourceListItemHolder> {
    return listOf(
        VocabularySourceListItemHolder(
            source = vocabularySourcePreviewSource(),
            pendingCount = 14,
            addedCount = 8,
            ignoredCount = 2,
        ),
        VocabularySourceListItemHolder(
            source = VocabularySource(
                id = 2,
                title = "Interview: product design",
                description = "Short YouTube interview transcript.",
                rawText = "",
                cleanText = "",
                createdAt = 1_718_000_000_000L,
                updatedAt = 1_718_100_000_000L,
                lastAnalyzedAt = null,
            ),
            pendingCount = 0,
            addedCount = 0,
            ignoredCount = 0,
        ),
    )
}

private fun vocabularySourcePreviewSource(): VocabularySource {
    return VocabularySource(
        id = 1,
        title = "Severance S01E01",
        description = "Transcript from the first episode.",
        rawText = "",
        cleanText = "",
        createdAt = 1_718_000_000_000L,
        updatedAt = 1_718_200_000_000L,
        lastAnalyzedAt = 1_718_220_000_000L,
    )
}

@Composable
private fun VocabularySourceDeleteConfirmationDialog(
    source: VocabularySource,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(text = stringResource(resource = Res.string.vocabulary_source_delete_action))
        },
        text = {
            Text(
                text = stringResource(
                    resource = Res.string.vocabulary_source_delete_question,
                    source.title,
                ),
            )
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(text = stringResource(resource = Res.string.vocabulary_source_delete_action))
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
private fun VocabularySourceListContent(
    holders: List<VocabularySourceListItemHolder>,
    onCreateClick: () -> Unit,
    onSourceClick: (VocabularySourceListItemHolder) -> Unit,
    onSourceLongClick: (VocabularySourceListItemHolder) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                modifier = Modifier.padding(start = 18.dp, top = 18.dp, end = 18.dp),
                text = stringResource(resource = Res.string.vocabulary_sources_title),
                style = MainTheme.typographies.cardTransferringScreenTextStyles.header,
            )

            if (holders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = stringResource(resource = Res.string.vocabulary_sources_empty))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 8.dp,
                        top = 12.dp,
                        end = 8.dp,
                        bottom = 124.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(
                        items = holders,
                        key = { holder -> holder.source.id },
                    ) { holder ->
                        VocabularySourceItem(
                            holder = holder,
                            onClick = { onSourceClick(holder) },
                            onLongClick = { onSourceLongClick(holder) },
                        )
                    }
                }
            }
        }

        RoundButton(
            modifier = Modifier
                .align(alignment = Alignment.BottomEnd)
                .padding(bottom = 48.dp, end = 48.dp),
            background = MainTheme.colors.material.primary,
            iconRes = Res.drawable.ic_add_24,
            onClick = onCreateClick,
            elevation = 4.dp,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VocabularySourceItem(
    holder: VocabularySourceListItemHolder,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val source = holder.source

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        colors = CardDefaults.cardColors(
            containerColor = MainTheme.colors.deckListScreen.lightDeckItemBackground,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(
                text = source.title,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            if (source.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = source.description,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = "updated: ${source.updatedAt}")
                Text(text = "analysis: ${source.lastAnalyzedAt ?: "-"}")
            }
            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = "pending: ${holder.pendingCount} / added: ${holder.addedCount} / ignored: ${holder.ignoredCount}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
