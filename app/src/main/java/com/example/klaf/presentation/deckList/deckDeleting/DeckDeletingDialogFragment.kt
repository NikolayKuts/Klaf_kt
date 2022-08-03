package com.example.klaf.presentation.deckList.deckDeleting

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.example.klaf.R
import com.example.klaf.presentation.deckList.DeckListViewModel
import com.example.klaf.presentation.theme.MainTheme

class DeckDeletingDialogFragment : DialogFragment(R.layout.dialog_deck_deleting) {

    private val args by navArgs<DeckDeletingDialogFragmentArgs>()
    private val navController by lazy { findNavController() }

    private val viewModel by navGraphViewModels<DeckListViewModel>(R.id.deckListFragment)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ComposeView>(R.id.dialog_deck_removing).setContent {
            MainTheme {
                DeckDeletionDialogView(
                    deckName = args.deckName,
                    onCloseDialogButtonClick = ::closeDialog,
                    onConfirmDeckDeletingButtonClick = ::deleteDeck
                )
            }
        }
    }

    private fun closeDialog() {
        navController.popBackStack()
    }

    private fun deleteDeck() {
        viewModel.deleteDeck(deckId = args.deckId)
        navController.popBackStack()
    }
}