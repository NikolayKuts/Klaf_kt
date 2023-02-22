package com.example.klaf.presentation.deckList.deckRenaming

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ComposeView
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.example.klaf.R
import com.example.domain.entities.Deck
import com.example.klaf.presentation.common.TransparentDialogFragment
import com.example.klaf.presentation.common.collectWhenStarted
import com.example.klaf.presentation.common.showSnackBar
import com.example.klaf.presentation.deckList.common.BaseDeckListViewModel
import com.example.klaf.presentation.theme.MainTheme

class DeckRenamingDialogFragment : TransparentDialogFragment(R.layout.dialog_deck_renaming) {

    private val args by navArgs<DeckRenamingDialogFragmentArgs>()
    private val navController by lazy { findNavController() }

    private val viewModel by navGraphViewModels<BaseDeckListViewModel>(R.id.deckListFragment)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setEvenMessageObserver(view = view)

        view.findViewById<ComposeView>(R.id.dialog_deck_renaming).setContent {
            setDeckRenamingStateObserver()
            viewModel.getDeckById(deckId = args.deckId).let { receivedDeck: Deck? ->
                if (receivedDeck == null) {
                    navController.popBackStack()
                } else {
                    MainTheme {
                        DeckRenamingDialog(
                            deckName = receivedDeck.name,
                            onConfirmRenamingClick = { newName ->
                                confirmDeckRenaming(deck = receivedDeck, newName = newName)
                            },
                            onCloseDialogClick = { navController.popBackStack() },
                        )
                    }
                }
            }
        }
    }

    private fun setEvenMessageObserver(view: View) {
        viewModel.eventMessage.collectWhenStarted(
            lifecycleOwner = viewLifecycleOwner
        ) { eventMessage ->
            view.showSnackBar(messageId = eventMessage.resId)
        }
    }

    private fun setDeckRenamingStateObserver() {
        viewModel.renamingState.collectWhenStarted(
            lifecycleOwner = viewLifecycleOwner
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

    private fun confirmDeckRenaming(deck: Deck, newName: String) {
        viewModel.renameDeck(deck = deck, newName = newName)
    }
}