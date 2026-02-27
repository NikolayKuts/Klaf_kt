package com.kuts.klaf.deckList.deckNavigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.kuts.klaf.common.BaseMainViewModel
import com.kuts.klaf.deckList.common.BaseDeckListViewModel
import com.kuts.klaf.deckRepetitionInfo.RepetitionInfoEvent.Non
import com.kuts.klaf.navigation.AppDestination
import org.koin.androidx.compose.koinViewModel

@Composable
internal fun DeckNavigationDialogDestination(
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

    DeckNavigationDialogView(
        deckName = deckName,
        eventMessage = sharedViewModel.eventMessage.collectAsState(initial = null).value,
        onDeleteDeckClick = {
            navController.navigate(
                route = AppDestination.DeckDeletingDialog(
                    deckId = deckId,
                    deckName = deckName,
                )
            )
        },
        onRenameDeckClick = {
            navController.navigate(route = AppDestination.DeckRenamingDialog(deckId = deckId))
        },
        onBrowseDeckClick = {
            navController.navigate(route = AppDestination.CardViewing(deckId = deckId, deckName = deckName))
        },
        onAddCardsClick = {
            navController.navigate(route = AppDestination.CardAddition(deckId = deckId))
        },
        onTransferCardsClick = {
            navController.navigate(route = AppDestination.CardTransferring(sourceDeckId = deckId))
        },
        onRepetitionInfoClick = {
            navController.navigate(
                route = AppDestination.DeckRepetitionInfoDialog(
                    deckId = deckId,
                    deckName = deckName,
                    repetitionInfoEvent = Non,
                )
            )
        },
        onDeckManagementClick = {
            navController.navigate(route = AppDestination.DeckManagement(deckId = deckId))
        },
        onCloseDialogClick = { navController.popBackStack() },
        onCraftStoryClick = { viewModel.generateGptPromptWithDeckContent(deckId = deckId) },
    )
}
