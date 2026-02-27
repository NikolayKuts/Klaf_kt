package com.kuts.klaf.cardTransferring.common

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import com.kuts.klaf.common.BaseMainViewModel
import com.kuts.klaf.navigation.AppDestination
import com.kuts.klaf.navigation.CollectFlowWithLifecycle
import com.kuts.klaf.navigation.ObserveAudioLifecycle
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun CardTransferringDestination(
    navController: NavHostController,
    backStackEntry: NavBackStackEntry,
    sharedViewModel: BaseMainViewModel,
    sourceDeckId: Int,
) {
    val viewModel: BaseCardTransferringViewModel = koinViewModel(
        viewModelStoreOwner = backStackEntry,
        parameters = { parametersOf(sourceDeckId) },
    )

    ObserveAudioLifecycle(
        onCreate = viewModel.audioPlayer::onCreate,
        onResume = viewModel.audioPlayer::onResume,
        onStop = viewModel.audioPlayer::onStop,
        onDestroy = viewModel.audioPlayer::onDestroy,
    )

    CollectFlowWithLifecycle(flow = viewModel.eventMessage, onEach = sharedViewModel::notify)

    CollectFlowWithLifecycle(flow = viewModel.navigationEvent) { event ->
        when (event) {
            is ICardTransferringNavigationEvent.ToCardEditingScreen -> {
                navController.navigate(
                    route = AppDestination.CardEditing(
                        deckId = event.deckId,
                        cardId = event.cardId,
                    )
                )
            }

            ICardTransferringNavigationEvent.ToCardMovingDialog -> {
                navController.navigate(route = AppDestination.CardMovingDialog)
            }

            is ICardTransferringNavigationEvent.ToCardAddingScreen -> {
                navController.navigate(route = AppDestination.CardAddition(deckId = event.sourceDeckId))
            }

            is ICardTransferringNavigationEvent.ToCardDeletingDialog -> {
                navController.navigate(
                    route = AppDestination.CardTransferringDeletingDialog(
                        cardQuantity = event.cardQuantity
                    )
                )
            }

            ICardTransferringNavigationEvent.ToPrevious -> {
                navController.popBackStack()
            }
        }
    }

    Surface {
        CardTransferringScreen(viewModel = viewModel)
    }
}
