package com.kuts.klaf.deckList.deckDeleting

import androidx.compose.runtime.Composable
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
internal fun DeckDeletingDialogDestination(
    navController: NavHostController,
    backStackEntry: NavBackStackEntry,
    sharedViewModel: BaseMainViewModel,
    deckId: Int,
    deckName: String,
) {
    val owner = remember(backStackEntry) {
        navController.getBackStackEntry(navController.graph.findStartDestination().id)
    }
    val viewModel: BaseDeckListViewModel = koinViewModel(viewModelStoreOwner = owner)
    val eventMessage by sharedViewModel.eventMessage.collectAsState(initial = null)

    DeckDeletionDialogView(
        deckName = deckName,
        eventMessage = eventMessage,
        onCloseDialogClick = { navController.popBackStack() },
        onConfirmDeckDeletingButtonClick = { viewModel.deleteDeck(deckId = deckId) },
    )
}
