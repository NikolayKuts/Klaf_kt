package com.example.presentation.deckRepetition.cardDeleting

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ComposeView
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.example.klaf.R
import com.example.klaf.presentation.common.TransparentDialogFragment
import com.example.klaf.presentation.common.collectWhenStarted
import com.example.klaf.presentation.deckRepetition.BaseDeckRepetitionViewModel
import com.example.klaf.presentation.theme.MainTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CardDeletingDialogFragment : TransparentDialogFragment(R.layout.dialog_card_deleting) {

    private val args by navArgs<CardDeletingDialogFragmentArgs>()

    private val viewModel by navGraphViewModels<BaseDeckRepetitionViewModel>(R.id.deckRepetitionFragment)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ComposeView>(R.id.compose_view_dialog).setContent {
            MainTheme {
                CardDeletingDialogView(
                    onConfirmDeleting = ::confirmCardRemoving,
                    onCancel = ::closeDialog
                )
            }
        }

        setEventMessageObserver()
    }

    private fun setEventMessageObserver() {
        viewModel.eventMessage.collectWhenStarted(
            lifecycleOwner = viewLifecycleOwner
        ) { eventMessage ->
//            requireContext().showToast(messageId = eventMessage.resId)    ////////////////////////////////////
        }
    }

    private fun confirmCardRemoving() {
        viewModel.deleteCard(cardId = args.cardId, deckId = args.deckId)
        closeDialog()
    }

    private fun closeDialog() {
        findNavController().popBackStack()
    }
}