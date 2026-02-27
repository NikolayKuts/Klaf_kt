package com.kuts.klaf.deckRepetition.cardDeleting

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import com.kuts.domain.common.LoadingState
import com.kuts.klaf.common.BaseMainViewModel
import com.kuts.klaf.common.CardDeletingDialogView
import com.kuts.klaf.deckRepetition.BaseDeckReviewViewModel
import com.kuts.klaf.navigation.CollectFlowWithLifecycle
import org.koin.androidx.compose.koinViewModel

@Composable
internal fun DeckRepetitionCardDeletingDialogDestination(
    navController: NavHostController,
    sharedViewModel: BaseMainViewModel,
    deckId: Int,
    cardId: Int,
) {
    val owner = navController.previousBackStackEntry ?: return

    val viewModel: BaseDeckReviewViewModel = koinViewModel(viewModelStoreOwner = owner)

    CollectFlowWithLifecycle(flow = viewModel.cardDeletingState) { deletingState ->
        if (deletingState is LoadingState.Success) {
            navController.popBackStack()
        }
    }

    val eventMessage by sharedViewModel.eventMessage.collectAsState(initial = null)

    CardDeletingDialogView(
        cardQuantity = 1,
        eventMessage = eventMessage,
        onConfirmDeleting = { viewModel.deleteCard(cardId = cardId, deckId = deckId) },
        onCancel = { navController.popBackStack() },
    )
}
