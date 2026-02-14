package com.kuts.klaf.cardTransferring.cardDeleting

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.navigation.fragment.navArgs
import com.kuts.klaf.presentation.R
import com.kuts.klaf.cardTransferring.common.BaseCardTransferringViewModel
import com.kuts.klaf.cardTransferring.common.ICardTransferringAction
import com.kuts.klaf.cardTransferring.common.ICardTransferringNavigationDestination.CardTransferringScreen
import com.kuts.klaf.common.CardDeletingDialogView
import com.kuts.klaf.common.TransparentDialogFragment
import com.kuts.klaf.theme.MainTheme
import org.koin.androidx.navigation.koinNavGraphViewModel

class CardDeletingDialogFragment : TransparentDialogFragment(
    layoutId = R.layout.common_compose_layout,
) {

    private val args by navArgs<CardDeletingDialogFragmentArgs>()

    private val viewModel by koinNavGraphViewModel<BaseCardTransferringViewModel>(
        navGraphId = R.id.cardTransferringFragment
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ComposeView>(R.id.compose_view).setContent {
            MainTheme {
                val eventMessage by sharedViewModel.eventMessage.collectAsState(initial = null)

                CardDeletingDialogView(
                    cardQuantity = args.cardQuantity,
                    onConfirmDeleting = ::deleteCards,
                    onCancel = ::closeDialog,
                    eventMessage = eventMessage,
                )
            }
        }
    }

    private fun deleteCards() {
        viewModel.sendAction(action = ICardTransferringAction.DeleteCards)
    }

    private fun closeDialog() {
        viewModel.sendAction(
            action = ICardTransferringAction.NavigateTo(destination = CardTransferringScreen)
        )
    }
}
