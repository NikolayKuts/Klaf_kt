package com.kuts.klaf.deckList.deckCreation

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
internal fun DeckCreationDialogDestination(
    navController: NavHostController,
    backStackEntry: NavBackStackEntry,
    sharedViewModel: BaseMainViewModel,
) {
    val owner = remember(backStackEntry) {
        navController.getBackStackEntry(navController.graph.findStartDestination().id)
    }
    val viewModel: BaseDeckListViewModel = koinViewModel(viewModelStoreOwner = owner)
    val message by sharedViewModel.eventMessage.collectAsState(initial = null)

    DeckCreationDialog(
        onConfirmCreationClick = viewModel::createNewDeck,
        onCloseDialogClick = { navController.popBackStack() },
        eventMassage = message,
    )
}
