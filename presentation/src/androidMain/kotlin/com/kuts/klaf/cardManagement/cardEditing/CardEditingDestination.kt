package com.kuts.klaf.cardManagement.cardEditing

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import com.kuts.klaf.cardManagement.cardAddition.CardManagementScreen
import com.kuts.klaf.cardManagement.common.CardManagementState
import com.kuts.klaf.common.BaseMainViewModel
import com.kuts.klaf.navigation.CollectFlowWithLifecycle
import com.kuts.klaf.navigation.ObserveAudioLifecycle
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun CardEditingDestination(
    navController: NavHostController,
    backStackEntry: NavBackStackEntry,
    sharedViewModel: BaseMainViewModel,
    deckId: Int,
    cardId: Int,
) {
    val viewModel: CardEditingViewModel = koinViewModel(
        viewModelStoreOwner = backStackEntry,
        parameters = { parametersOf(deckId, cardId) },
    )

    ObserveAudioLifecycle(
        onCreate = viewModel.audioPlayer::onCreate,
        onResume = viewModel.audioPlayer::onResume,
        onStop = viewModel.audioPlayer::onStop,
        onDestroy = viewModel.audioPlayer::onDestroy,
    )

    CollectFlowWithLifecycle(flow = viewModel.eventMessage, onEach = sharedViewModel::notify)

    CollectFlowWithLifecycle(flow = viewModel.cardManagementState) { managementState ->
        if (managementState is CardManagementState.Finished) {
            navController.popBackStack()
        }
    }

    Surface {
        CardManagementScreen(viewModel = viewModel)
    }
}
