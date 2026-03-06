package com.kuts.klaf.deckRepetition

import androidx.activity.compose.BackHandler
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import com.kuts.klaf.common.BaseMainViewModel
import com.kuts.klaf.deckRepetitionInfo.RepetitionInfoEvent.Non
import com.kuts.klaf.navigation.AppDestination
import com.kuts.klaf.navigation.CollectFlowWithLifecycle
import com.kuts.klaf.navigation.ObserveAudioLifecycle
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun DeckRepetitionDestination(
    navController: NavHostController,
    backStackEntry: NavBackStackEntry,
    sharedViewModel: BaseMainViewModel,
    deckId: Int,
    deckName: String,
) {
    val viewModel: BaseDeckReviewViewModel = koinViewModel(
        viewModelStoreOwner = backStackEntry,
        parameters = { parametersOf(deckId) },
    )

    ObserveAudioLifecycle(
        onCreate = viewModel.audioPlayer::onCreate,
        onResume = viewModel.audioPlayer::onResume,
        onStop = viewModel.audioPlayer::onStop,
        onDestroy = viewModel.audioPlayer::onDestroy,
    )

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(key1 = lifecycleOwner, key2 = viewModel.timer) {
        lifecycleOwner.lifecycle.addObserver(viewModel.timer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(viewModel.timer)
        }
    }

    CollectFlowWithLifecycle(flow = viewModel.eventMessage, onEach = sharedViewModel::notify)

    CollectFlowWithLifecycle(flow = viewModel.screenState) { state ->
        if (
            state is RepetitionScreenState.FinishState
            && state.repetitionInfoEvent != Non
        ) {
            navController.navigate(
                route = AppDestination.DeckRepetitionInfoDialog(
                    deckId = deckId,
                    deckName = deckName,
                    repetitionInfoEvent = state.repetitionInfoEvent,
                )
            )
        }
    }

    val screenState by viewModel.screenState.collectAsState(RepetitionScreenState.StartState)
    var showExitDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = screenState == RepetitionScreenState.RepetitionState) {
        showExitDialog = showExitDialog.not()
    }

    Surface {
        DeckReviewScreen(
            viewModel = viewModel,
            showExitDialog = showExitDialog,
            onShowExitDialogChange = { showExitDialog = it },
            onExitConfirmed = { navController.popBackStack() },
            onDeleteCardClick = { cardId ->
                navController.navigate(
                    route = AppDestination.DeckRepetitionCardDeletingDialog(
                        deckId = deckId,
                        cardId = cardId,
                    )
                )
            },
            onAddCardClick = {
                navController.navigate(route = AppDestination.CardAddition(deckId = deckId))
            },
            onEditCardClick = { cardId ->
                navController.navigate(route = AppDestination.CardEditing(deckId = deckId, cardId = cardId))
            },
        )
    }
}
