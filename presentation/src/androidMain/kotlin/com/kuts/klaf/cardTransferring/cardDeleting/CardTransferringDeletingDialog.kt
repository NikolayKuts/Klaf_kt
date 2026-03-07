package com.kuts.klaf.cardTransferring.cardDeleting

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavHostController
import com.kuts.klaf.cardTransferring.common.BaseCardTransferringViewModel
import com.kuts.klaf.cardTransferring.common.ICardTransferringAction
import com.kuts.klaf.cardTransferring.common.ICardTransferringNavigationDestination.CardTransferringScreen as CardTransferringScreenDestination
import com.kuts.klaf.common.BaseMainViewModel
import com.kuts.klaf.common.CardDeletingDialogView
import com.kuts.klaf.common.EventMessage
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun CardTransferringDeletingDialog(
    navController: NavHostController,
    sharedViewModel: BaseMainViewModel,
    cardQuantity: Int,
) {
    val owner = navController.previousBackStackEntry ?: return

    val viewModel: BaseCardTransferringViewModel = koinViewModel(viewModelStoreOwner = owner)
    val eventMessage = sharedViewModel.eventMessage.collectAsState(initial = null).value

    CardTransferringDeletingDialogContent(
        cardQuantity = cardQuantity,
        eventMessage = eventMessage,
        onConfirmDeleting = {
            viewModel.sendAction(action = ICardTransferringAction.DeleteCards)
        },
        onCancel = {
            viewModel.sendAction(
                action = ICardTransferringAction.NavigateTo(destination = CardTransferringScreenDestination)
            )
        },
    )
}

@Composable
private fun CardTransferringDeletingDialogContent(
    cardQuantity: Int,
    eventMessage: EventMessage?,
    onConfirmDeleting: () -> Unit,
    onCancel: () -> Unit,
) {
    CardDeletingDialogView(
        cardQuantity = cardQuantity,
        eventMessage = eventMessage,
        onConfirmDeleting = onConfirmDeleting,
        onCancel = onCancel,
    )
}
