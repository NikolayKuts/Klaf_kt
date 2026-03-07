package com.kuts.klaf.deckList.deckCreation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import com.kuts.klaf.common.BaseMainViewModel
import com.kuts.klaf.common.EventMessage
import com.kuts.klaf.deckList.common.BaseDeckListViewModel
import com.kuts.klaf.deckList.common.DeckNamingView
import com.kuts.klaf.navigation.AppDestination
import com.kuts.klaf.presentation.resources.*
import com.kuts.klaf.theme.MainTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun DeckCreationDialog(
    navController: NavHostController,
    backStackEntry: NavBackStackEntry,
    sharedViewModel: BaseMainViewModel,
) {
    val owner = remember(backStackEntry) {
        navController.getBackStackEntry(route = AppDestination.DeckList)
    }
    val viewModel: BaseDeckListViewModel = koinViewModel(viewModelStoreOwner = owner)
    val message by sharedViewModel.eventMessage.collectAsState(initial = null)

    DeckCreationDialogContent(
        onConfirmCreationClick = viewModel::createNewDeck,
        onCloseDialogClick = { navController.popBackStack() },
        eventMassage = message,
    )
}

@Composable
private fun DeckCreationDialogContent(
    onConfirmCreationClick: (deckName: String) -> Unit,
    onCloseDialogClick: () -> Unit,
    eventMassage: EventMessage? = null,
) {
    DeckNamingView(
        title = { DialogTitle() },
        onConfirmCreationClick = onConfirmCreationClick,
        onCloseDialogClick = onCloseDialogClick,
        eventMessage = eventMassage
    )
}

@Composable
private fun DialogTitle() {
    Text(
        style = MainTheme.typographies.dialogTextStyle,
        text = stringResource(resource = Res.string.deck_creation_dialog_title),
    )
}
