package com.example.klaf.presentation.deckList.deckNavigation

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ComposeView
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.klaf.R
import com.example.klaf.presentation.common.TransparentDialogFragment
import com.example.klaf.presentation.deckList.DeckNavigationDialogView
import com.example.klaf.presentation.theme.MainTheme

class DeckNavigationDialogFragment : TransparentDialogFragment(R.layout.dialog_deck_navigation) {

    private val args by navArgs<DeckNavigationDialogFragmentArgs>()

    private val navController by lazy { findNavController() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ComposeView>(R.id.dialog_deck_navigation).setContent {
            MainTheme() {
                DeckNavigationDialogView(
                    deckName = args.deckName,
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
        DeckNavigationDialogFragmentDirections.actionDeckNavigationDialogToDeckDeletingDialogFragment(
            deckId = args.deckId,
            deckName = args.deckName
        ).also { navController.navigate(directions = it) }
    }

    private fun navigateToDeckRenamingDialogFragment() {
        DeckNavigationDialogFragmentDirections.actionDeckNavigationDialogToDeckRenamingDialogFragment(
            deckId = args.deckId
        ).also { navController.navigate(it) }
    }

    private fun navigateToCardAdditionFragment() {
        DeckNavigationDialogFragmentDirections.actionDeckNavigationDialogToCardAdditionFragment(
            deckId = args.deckId
        ).also { navController.navigate(it) }
    }

    private fun navigateToCardViewerFragment() {
        DeckNavigationDialogFragmentDirections.actionDeckNavigationDialogToCardViewerFragment(
            deckId = args.deckId,
            deckName = args.deckName
        ).also { navController.navigate(it) }
    }
}