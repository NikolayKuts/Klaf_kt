package com.kuts.klaf.presentation.cardTransferring.cardDeleting

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.kuts.klaf.R
import com.kuts.klaf.presentation.cardTransferring.common.BaseCardTransferringViewModel
import com.kuts.klaf.presentation.cardTransferring.common.CardTransferringAction
import com.kuts.klaf.presentation.cardTransferring.common.CardTransferringNavigationDestination.CardTransferringScreen
import com.kuts.klaf.presentation.common.CardDeletingDialogView
import com.kuts.klaf.presentation.common.TransparentDialogFragment
import com.kuts.klaf.presentation.theme.MainTheme

class CardDeletingDialogFragment : TransparentDialogFragment(
    layoutId = R.layout.common_compose_layout,
) {

    private val args by navArgs<CardDeletingDialogFragmentArgs>()

    private val viewModel by navGraphViewModels<BaseCardTransferringViewModel>(
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
        viewModel.sendAction(action = CardTransferringAction.DeleteCards)
    }

    private fun closeDialog() {
        viewModel.sendAction(
            action = CardTransferringAction.NavigateTo(destination = CardTransferringScreen)
        )
    }
}