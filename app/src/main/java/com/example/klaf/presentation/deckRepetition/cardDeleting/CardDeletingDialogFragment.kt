package com.example.klaf.presentation.deckRepetition.cardDeleting

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.example.domain.common.LoadingState
import com.example.klaf.R
import com.example.klaf.presentation.common.CardDeletingDialogView
import com.example.klaf.presentation.common.TransparentDialogFragment
import com.example.klaf.presentation.common.collectWhenStarted
import com.example.klaf.presentation.deckRepetition.BaseDeckRepetitionViewModel
import com.example.klaf.presentation.theme.MainTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CardDeletingDialogFragment : TransparentDialogFragment(
    layoutId = R.layout.common_compose_layout
) {

    private val args by navArgs<CardDeletingDialogFragmentArgs>()

    private val viewModel by navGraphViewModels<BaseDeckRepetitionViewModel>(R.id.deckRepetitionFragment)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeDeletingState()

        view.findViewById<ComposeView>(R.id.compose_view).setContent {
            MainTheme {
                val eventMessage by sharedViewModel.eventMessage.collectAsState(initial = null)

                CardDeletingDialogView(
                    cardQuantity = 1,
                    eventMessage = eventMessage,
                    onConfirmDeleting = ::deleteCard,
                    onCancel = ::closeDialog
                )
            }
        }
    }

    private fun observeDeletingState() {
        viewModel.cardDeletingState.collectWhenStarted(
            lifecycleOwner = this
        ) { deletingState ->
            when (deletingState) {
                is LoadingState.Success -> closeDialog()
                else -> {}
            }
        }
    }

    private fun deleteCard() {
        viewModel.deleteCard(cardId = args.cardId, deckId = args.deckId)
    }

    private fun closeDialog() {
        findNavController().popBackStack()
    }
}