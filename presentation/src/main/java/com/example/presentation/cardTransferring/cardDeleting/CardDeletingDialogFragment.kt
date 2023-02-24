package com.example.presentation.cardTransferring.cardDeleting

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ComposeView
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.example.klaf.R
import com.example.klaf.presentation.cardTransferring.common.BaseCardTransferringViewModel
import com.example.klaf.presentation.common.TransparentDialogFragment
import com.example.klaf.presentation.common.collectWhenStarted
import com.example.klaf.presentation.theme.MainTheme

class CardDeletingDialogFragment :
    TransparentDialogFragment(contentLayoutId = R.layout.dialog_interim_deck) {

    private val viewModel by navGraphViewModels<BaseCardTransferringViewModel>(
        navGraphId = R.id.cardTransferringFragment
    )
    private val args by navArgs<CardDeletingDialogFragmentArgs>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeCardDeletingState()

        view.findViewById<ComposeView>(R.id.compose_view_dialog).setContent {
            MainTheme {
                CardDeletingDialogView(
                    cardQuantity = args.cardQuantity,
                    onConfirmDeleting = ::deleteCards,
                    onCancel = ::closeDialog
                )
            }
        }
    }

    private fun observeCardDeletingState() {
        viewModel.cardDeletingState.collectWhenStarted(
            lifecycleOwner = viewLifecycleOwner
        ) { deletingState ->
            when (deletingState) {
                CardDeletingState.NON -> {}
                CardDeletingState.IN_PROGRESS -> {}
                CardDeletingState.FINISHED -> closeDialog()
            }
        }
    }

    private fun deleteCards() {
        viewModel.deleteCards()
    }

    private fun closeDialog() {
        viewModel.resetCardDeletingState()
        findNavController().popBackStack()
    }
}