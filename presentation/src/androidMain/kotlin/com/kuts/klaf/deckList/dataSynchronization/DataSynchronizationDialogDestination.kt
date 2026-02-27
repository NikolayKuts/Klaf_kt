package com.kuts.klaf.deckList.dataSynchronization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.kuts.domain.common.AuthenticationAction
import com.kuts.klaf.authentication.AuthenticationActionResult
import com.kuts.klaf.common.BaseMainViewModel
import com.kuts.klaf.common.EventMessage
import com.kuts.klaf.deckList.common.BaseDeckListViewModel
import com.kuts.klaf.presentation.R
import org.koin.androidx.compose.koinViewModel

@Composable
internal fun DataSynchronizationDialogDestination(
    navController: NavHostController,
    backStackEntry: NavBackStackEntry,
    sharedViewModel: BaseMainViewModel,
    authenticationActionResult: AuthenticationActionResult?,
) {
    val owner = remember(backStackEntry) {
        navController.getBackStackEntry(navController.graph.findStartDestination().id)
    }
    val viewModel: BaseDeckListViewModel = koinViewModel(viewModelStoreOwner = owner)

    val eventMessage by sharedViewModel.eventMessage.collectAsState(initial = null)

    DataSynchronizationDialogView(
        synchronizationState = viewModel.dataSynchronizationState.collectAsState().value,
        onConfirmClick = viewModel::synchronizeData,
        onCloseClick = { navController.popBackStack() },
        onDispose = viewModel::resetSynchronizationState,
        eventMassage = eventMessage,
        onLaunched = {
            if (authenticationActionResult?.isSuccessful != true) {
                return@DataSynchronizationDialogView
            }

            val messageId = when (authenticationActionResult.action) {
                AuthenticationAction.SIGN_IN -> {
                    R.string.authentication_sign_in_success
                }

                AuthenticationAction.SIGN_UP -> {
                    R.string.authentication_sign_up_success
                }
            }

            sharedViewModel.notify(
                message = EventMessage(
                    resId = messageId,
                    type = EventMessage.Type.Positive,
                )
            )
        },
    )
}
