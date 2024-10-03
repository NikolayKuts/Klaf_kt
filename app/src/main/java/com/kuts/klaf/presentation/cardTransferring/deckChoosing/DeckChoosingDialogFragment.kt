package com.kuts.klaf.presentation.cardTransferring.deckChoosing

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.navigation.navGraphViewModels
import com.kuts.domain.entities.Deck
import com.kuts.klaf.R
import com.kuts.klaf.presentation.cardTransferring.common.BaseCardTransferringViewModel
import com.kuts.klaf.presentation.cardTransferring.common.CardTransferringAction
import com.kuts.klaf.presentation.cardTransferring.common.CardTransferringNavigationDestination.CardTransferringScreen
import com.kuts.klaf.presentation.common.TransparentDialogFragment
import com.kuts.klaf.presentation.theme.MainTheme

class DeckChoosingDialogFragment : TransparentDialogFragment(
    layoutId = R.layout.common_compose_layout
) {

    private val viewModel by navGraphViewModels<BaseCardTransferringViewModel>(
        navGraphId = R.id.cardTransferringFragment
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ComposeView>(R.id.compose_view).setContent {
            MainTheme {
                val eventMessage by sharedViewModel.eventMessage.collectAsState(initial = null)

                DeckChoosingDialogView(
                    decks = viewModel.decks.collectAsState().value,
                    onConfirmClick = ::sendMoveCardAction,
                    onCloseClick = ::closeDialog,
                    eventMessage = eventMessage
                )
            }
        }
    }

    private fun closeDialog() {
        viewModel.sendAction(
            action = CardTransferringAction.NavigateTo(destination = CardTransferringScreen)
        )
    }

    private fun sendMoveCardAction(targetDeck: Deck) {
        viewModel.sendAction(action = CardTransferringAction.MoveCards(targetDeck = targetDeck))
    }
}