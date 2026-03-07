package com.kuts.klaf.deckList.deckRenaming

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.kuts.klaf.common.BaseMainViewModel
import com.kuts.klaf.common.EventMessage
import com.kuts.klaf.deckList.common.BaseDeckListViewModel
import com.kuts.klaf.deckList.common.DeckNamingView
import com.kuts.klaf.presentation.resources.*
import com.kuts.klaf.theme.MainTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun DeckRenamingDialog(
    navController: NavHostController,
    backStackEntry: NavBackStackEntry,
    sharedViewModel: BaseMainViewModel,
    deckId: Int,
) {
    val owner = remember(backStackEntry) {
        navController.getBackStackEntry(navController.graph.findStartDestination().id)
    }
    val viewModel: BaseDeckListViewModel = koinViewModel(viewModelStoreOwner = owner)
    val deck = viewModel.getDeckById(deckId = deckId)

    if (deck == null) {
        LaunchedEffect(key1 = Unit) { navController.popBackStack() }
        return
    }

    val eventMessage by sharedViewModel.eventMessage.collectAsState(initial = null)

    DeckRenamingDialogContent(
        deckName = deck.name,
        eventMessage = eventMessage,
        onConfirmRenamingClick = { newName ->
            viewModel.renameDeck(deck = deck, newName = newName)
        },
        onCloseDialogClick = { navController.popBackStack() },
    )
}

@Composable
private fun DeckRenamingDialogContent(
    deckName: String,
    onConfirmRenamingClick: (newName: String) -> Unit,
    onCloseDialogClick: () -> Unit,
    eventMessage: EventMessage?,
) {
    DeckNamingView(
        title = { DialogTitle(deckName = deckName) },
        onConfirmCreationClick = onConfirmRenamingClick,
        onCloseDialogClick = onCloseDialogClick,
        initialName = deckName,
        eventMessage = eventMessage,
    )
}

@Composable
private fun DialogTitle(deckName: String) {
    Text(
        style = MainTheme.typographies.dialogTextStyle,
        text = buildAnnotatedString {
            withStyle(style = SpanStyle()) {
                append(text = stringResource(resource = Res.string.deck_navigation_dialog_item_rename_deck))
            }
            withStyle(style = MainTheme.typographies.accentedDialogText) {
                append(" \"${deckName}\"")
            }
        }
    )
}
