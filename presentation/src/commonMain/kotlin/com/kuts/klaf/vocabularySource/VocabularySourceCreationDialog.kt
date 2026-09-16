package com.kuts.klaf.vocabularySource

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import com.kuts.klaf.common.BaseMainViewModel
import com.kuts.klaf.navigation.AppDestination
import com.kuts.klaf.navigation.CollectFlowWithLifecycle
import com.kuts.klaf.presentation.resources.Res
import com.kuts.klaf.presentation.resources.vocabulary_source_create_action
import com.kuts.klaf.presentation.resources.vocabulary_source_creation_title
import com.kuts.klaf.presentation.resources.vocabulary_source_description_label
import com.kuts.klaf.presentation.resources.vocabulary_source_title_label
import com.kuts.klaf.theme.MainTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun VocabularySourceCreationDialog(
    navController: NavHostController,
    backStackEntry: NavBackStackEntry,
    sharedViewModel: BaseMainViewModel,
) {
    val owner = remember(backStackEntry) {
        navController.getBackStackEntry(route = AppDestination.VocabularySourceList)
    }
    val viewModel: VocabularySourceListViewModel = koinViewModel(viewModelStoreOwner = owner)

    CollectFlowWithLifecycle(flow = viewModel.eventMessage, onEach = sharedViewModel::notify)

    var title by remember { mutableStateOf(value = "") }
    var description by remember { mutableStateOf(value = "") }

    VocabularySourceCreationDialogContent(
        title = title,
        description = description,
        onTitleChanged = { title = it },
        onDescriptionChanged = { description = it },
        onConfirm = {
            viewModel.createSource(
                title = title,
                description = description,
                onCreated = { sourceId ->
                    navController.popBackStack()
                    navController.navigate(
                        route = AppDestination.VocabularySourceDetail(sourceId = sourceId)
                    )
                },
            )
        },
        onCancel = { navController.popBackStack() },
    )
}

@Preview(name = "Light", showBackground = true, backgroundColor = 0xFFF4F0E8)
@Composable
private fun VocabularySourceCreationDialogContentPreview() {
    VocabularySourceCreationDialogContentPreviewContent(darkTheme = false)
}

@Preview(name = "Dark", showBackground = true, backgroundColor = 0xFF1B1B1F)
@Composable
private fun VocabularySourceCreationDialogContentDarkPreview() {
    VocabularySourceCreationDialogContentPreviewContent(darkTheme = true)
}

@Composable
private fun VocabularySourceCreationDialogContentPreviewContent(darkTheme: Boolean) {
    MainTheme(darkTheme = darkTheme) {
        VocabularySourceCreationDialogContent(
            title = "Severance S01E01",
            description = "Transcript from YouTube captions.",
            onTitleChanged = {},
            onDescriptionChanged = {},
            onConfirm = {},
            onCancel = {},
        )
    }
}

@Composable
private fun VocabularySourceCreationDialogContent(
    title: String,
    description: String,
    onTitleChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(text = stringResource(resource = Res.string.vocabulary_source_creation_title))
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = title,
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
                    value = description,
                    onValueChange = onDescriptionChanged,
                    label = {
                        Text(text = stringResource(resource = Res.string.vocabulary_source_description_label))
                    },
                    minLines = 3,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(resource = Res.string.vocabulary_source_create_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(text = "Cancel")
            }
        },
    )
}
