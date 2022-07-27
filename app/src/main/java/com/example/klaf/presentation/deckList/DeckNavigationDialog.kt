package com.example.klaf.presentation.deckList

import android.app.Dialog
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.klaf.R
import com.example.klaf.presentation.theme.MainTheme
import android.graphics.Color as AndroidColor

class DeckNavigationDialog : DialogFragment(R.layout.dialog_deck_navigation) {

    private val args by navArgs<DeckNavigationDialogArgs>()

    private val navController by lazy { findNavController() }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            window?.setBackgroundDrawable(ColorDrawable(AndroidColor.TRANSPARENT))
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ComposeView>(R.id.dialog_deck_navigation).setContent {
            MainTheme() {
                DeckNavigationDialogView(
                    deckName = args.deckName,
                    deckId = args.deckId,
                    onDeleteDeckClick = ::navigateToDeckRemovingDialogFragment,
                    onRenameDeckClick = ::navigateToDeckRenamingDialogFragment,
                    onBrowseDeckClick = ::navigateToCardViewerFragment,
                    onAddCardsClick = ::navigateToCardAdditionFragment,
                    onCloseDialogClick = { findNavController().popBackStack() }
                )
            }
        }
    }


    private fun navigateToDeckRemovingDialogFragment() {
        DeckNavigationDialogDirections.actionDeckNavigationDialogToDeckRemovingDialogFragment(
            deckId = args.deckId,
            deckName = args.deckName
        ).also { navController.navigate(directions = it) }
    }

    private fun navigateToDeckRenamingDialogFragment() {
        DeckNavigationDialogDirections.actionDeckNavigationDialogToDeckRenamingDialogFragment(
            deckId = args.deckId
        ).also { navController.navigate(it) }
    }

    private fun navigateToCardAdditionFragment() {
        DeckNavigationDialogDirections.actionDeckNavigationDialogToCardAdditionFragment(
            deckId = args.deckId
        ).also { navController.navigate(it) }
    }

    private fun navigateToCardViewerFragment() {
        DeckNavigationDialogDirections.actionDeckNavigationDialogToCardViewerFragment(
            deckId = args.deckId,
            deckName = args.deckName
        ).also { navController.navigate(it) }
    }
}