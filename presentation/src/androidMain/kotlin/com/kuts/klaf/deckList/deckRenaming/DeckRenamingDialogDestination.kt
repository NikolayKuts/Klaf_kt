package com.kuts.klaf.deckList.deckRenaming

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.kuts.klaf.common.BaseMainViewModel
import com.kuts.klaf.deckList.common.BaseDeckListViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
internal fun DeckRenamingDialogDestination(
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

    DeckRenamingDialog(
        deckName = deck.name,
        eventMessage = eventMessage,
        onConfirmRenamingClick = { newName ->
            viewModel.renameDeck(deck = deck, newName = newName)
        },
        onCloseDialogClick = { navController.popBackStack() },
    )
}
