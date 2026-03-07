package com.kuts.klaf.deckList.drawer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.kuts.domain.common.ifNotNull
import com.kuts.klaf.common.*
import com.kuts.klaf.common.BaseMainViewModel
import com.kuts.klaf.deckList.common.BaseDeckListViewModel
import com.kuts.klaf.presentation.resources.*
import com.kuts.klaf.theme.MainTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun DrawerActionDialog(
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

    DrawerActionDialogContent(
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

@Composable
private fun DrawerActionDialogContent(
    action: DrawerAction,
    loadingState: Boolean,
    onCloseDialog: () -> Unit,
    onConfirmationClick: () -> Unit,
    eventMessage: EventMessage?
) {
    ScrollableBox(
        modifier = Modifier.noRippleClickable(onClick = onCloseDialog),
        dialogMode = true,
        eventContent = {
            eventMessage.ifNotNull { EventMessageView(message = it) }
        },
    ) {
        FullBackgroundDialog(
            onBackgroundClick = { onCloseDialog() },
            topContent = ContentHolder(size = DIALOG_APP_LABEL_SIZE.dp) {
                DialogAppLabel(isLoading = loadingState)
                                                                        },
            mainContent = {
                when (action) {
                    DrawerAction.LOG_OUT -> LogOutView()
                    DrawerAction.DELETE_ACCOUNT -> DeletingView()
                }
            },
            bottomContent = {
                ConfirmationButton(onClick = onConfirmationClick)
                ClosingButton(onClick = onCloseDialog)
            }
        )
    }
}

@Composable
private fun LogOutView() {
    Text(text = stringResource(resource = Res.string.log_out_confirmation_question))
}

@Composable
private fun DeletingView() {
    Column {
        WarningMessage(textRes = Res.string.account_deleting_warning_message)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            modifier = Modifier.padding(6.dp),
            text = stringResource(resource = Res.string.account_deleting_confirmation_question),
            style = MainTheme.typographies.dialogTextStyle
        )
    }
}
