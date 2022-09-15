package com.example.klaf.presentation.deckList.deckRenaming

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.example.klaf.R
import com.example.klaf.domain.entities.Deck
import com.example.klaf.presentation.common.TransparentDialogFragment
import com.example.klaf.presentation.common.collectWhenStarted
import com.example.klaf.presentation.deckList.common.DeckListViewModel
import com.example.klaf.presentation.theme.MainTheme

class DeckRenamingDialogFragment : TransparentDialogFragment(R.layout.dialog_deck_renaming) {

    private val args by navArgs<DeckRenamingDialogFragmentArgs>()
    private val navController by lazy { findNavController() }

    private val viewModel by navGraphViewModels<DeckListViewModel>(R.id.deckListFragment)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ComposeView>(R.id.dialog_deck_renaming).setContent {
            setDeckRenamingStateObserver()
            viewModel.getDeckById(deckId = args.deckId).let { deck: Deck? ->
                if (deck == null) {
                    navController.popBackStack()
                    return@let
                }
                MainTheme {
                    DeckRenamingDialog(
                        deckName = deck.name,
                        onConfirmRenamingClick = { newName ->
                            confirmDeckRenaming(deck = deck, newName = newName)
                        },
                        onCloseDialogClick = { navController.popBackStack() },
                    )

                }
            }
        }
    }

    private fun setDeckRenamingStateObserver() {
        viewModel.renamingState.collectWhenStarted(
            viewLifecycleOwner.lifecycleScope
        ) { renamingState ->
            when (renamingState) {
                DeckRenamingState.NOT_RENAMED -> {}
                DeckRenamingState.RENAMED -> {
                    viewModel.resetDeckRenamingState()
                    navController.popBackStack()
                }
            }
        }
    }

    private fun confirmDeckRenaming(deck: Deck?, newName: String) {
        viewModel.renameDeck(deck = deck, newName = newName)
    }
}