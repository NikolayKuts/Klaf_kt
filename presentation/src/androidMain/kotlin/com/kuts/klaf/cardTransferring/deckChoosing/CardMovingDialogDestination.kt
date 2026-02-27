package com.kuts.klaf.cardTransferring.deckChoosing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavHostController
import com.kuts.klaf.cardTransferring.common.BaseCardTransferringViewModel
import com.kuts.klaf.cardTransferring.common.ICardTransferringAction
import com.kuts.klaf.cardTransferring.common.ICardTransferringNavigationDestination.CardTransferringScreen as CardTransferringScreenDestination
import com.kuts.klaf.common.BaseMainViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
internal fun CardMovingDialogDestination(
    navController: NavHostController,
    sharedViewModel: BaseMainViewModel,
) {
    val owner = navController.previousBackStackEntry ?: return

    val viewModel: BaseCardTransferringViewModel = koinViewModel(viewModelStoreOwner = owner)

    DeckChoosingDialogView(
        decks = viewModel.decks.collectAsState().value,
        onConfirmClick = { targetDeck ->
            viewModel.sendAction(action = ICardTransferringAction.MoveCards(targetDeck = targetDeck))
        },
        onCloseClick = {
            viewModel.sendAction(
                action = ICardTransferringAction.NavigateTo(destination = CardTransferringScreenDestination)
            )
        },
        eventMessage = sharedViewModel.eventMessage.collectAsState(initial = null).value,
    )
}
