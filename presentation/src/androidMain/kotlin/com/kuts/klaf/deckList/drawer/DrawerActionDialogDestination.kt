package com.kuts.klaf.deckList.drawer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.kuts.klaf.common.BaseMainViewModel
import com.kuts.klaf.deckList.common.BaseDeckListViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
internal fun DrawerActionDialogDestination(
    navController: NavHostController,
    backStackEntry: NavBackStackEntry,
    sharedViewModel: BaseMainViewModel,
    drawerAction: DrawerAction,
) {
    val owner = remember(backStackEntry) {
        navController.getBackStackEntry(navController.graph.findStartDestination().id)
    }
    val viewModel: BaseDeckListViewModel = koinViewModel(viewModelStoreOwner = owner)

    val eventMessage = sharedViewModel.eventMessage.collectAsState(initial = null).value

    DrawerActionView(
        action = drawerAction,
        loadingState = viewModel.drawerActionLoadingState.collectAsState().value,
        onCloseDialog = { navController.popBackStack() },
        onConfirmationClick = {
            when (drawerAction) {
                DrawerAction.LOG_OUT -> viewModel.logOut()
                DrawerAction.DELETE_ACCOUNT -> viewModel.deleteAccount()
            }
        },
        eventMessage = eventMessage,
    )
}
