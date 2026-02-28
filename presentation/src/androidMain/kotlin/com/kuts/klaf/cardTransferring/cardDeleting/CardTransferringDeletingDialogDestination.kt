package com.kuts.klaf.cardTransferring.cardDeleting

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavHostController
import com.kuts.klaf.cardTransferring.common.BaseCardTransferringViewModel
import com.kuts.klaf.cardTransferring.common.ICardTransferringAction
import com.kuts.klaf.cardTransferring.common.ICardTransferringNavigationDestination.CardTransferringScreen as CardTransferringScreenDestination
import com.kuts.klaf.common.BaseMainViewModel
import com.kuts.klaf.common.CardDeletingDialogView
import org.koin.androidx.compose.koinViewModel

@Composable
internal fun CardTransferringDeletingDialogDestination(
    navController: NavHostController,
    sharedViewModel: BaseMainViewModel,
    cardQuantity: Int,
) {
    val owner = navController.previousBackStackEntry ?: return

    val viewModel: BaseCardTransferringViewModel = koinViewModel(viewModelStoreOwner = owner)

    CardDeletingDialogView(
        cardQuantity = cardQuantity,
        onConfirmDeleting = {
            viewModel.sendAction(action = ICardTransferringAction.DeleteCards)
        },
        onCancel = {
            viewModel.sendAction(
                action = ICardTransferringAction.NavigateTo(destination = CardTransferringScreenDestination)
            )
        },
        eventMessage = sharedViewModel.eventMessage.collectAsState(initial = null).value,
    )
}
