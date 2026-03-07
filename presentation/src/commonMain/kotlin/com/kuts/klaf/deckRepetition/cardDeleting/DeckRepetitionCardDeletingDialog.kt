package com.kuts.klaf.deckRepetition.cardDeleting

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import com.kuts.domain.common.LoadingState
import com.kuts.klaf.common.BaseMainViewModel
import com.kuts.klaf.common.CardDeletingDialogView
import com.kuts.klaf.common.EventMessage
import com.kuts.klaf.deckRepetition.BaseDeckReviewViewModel
import com.kuts.klaf.navigation.CollectFlowWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun DeckRepetitionCardDeletingDialog(
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

    DeckRepetitionCardDeletingDialogContent(
        cardQuantity = 1,
        eventMessage = eventMessage,
        onConfirmDeleting = { viewModel.deleteCard(cardId = cardId, deckId = deckId) },
        onCancel = { navController.popBackStack() },
    )
}

@Composable
private fun DeckRepetitionCardDeletingDialogContent(
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
