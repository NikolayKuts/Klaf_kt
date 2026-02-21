package com.kuts.klaf.deckList.deckRenaming

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.navigation.fragment.navArgs
import com.kuts.domain.entities.Deck
import com.kuts.klaf.presentation.R
import com.kuts.klaf.common.TransparentDialogFragment
import com.kuts.klaf.deckList.common.BaseDeckListViewModel
import com.kuts.klaf.deckList.common.IDeckListNavigationEvent
import com.kuts.klaf.theme.MainTheme
import org.koin.androidx.navigation.koinNavGraphViewModel

class DeckRenamingDialogFragment : TransparentDialogFragment(R.layout.common_compose_layout) {

    private val args by navArgs<DeckRenamingDialogFragmentArgs>()

    private val viewModel by koinNavGraphViewModel<BaseDeckListViewModel>(R.id.deckListFragment)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ComposeView>(R.id.compose_view).setContent {
            viewModel.getDeckById(deckId = args.deckId).let { receivedDeck: Deck? ->
                if (receivedDeck == null) {
                    closeDialog()
                } else {
                    MainTheme {
                        val eventMessage by sharedViewModel.eventMessage.collectAsState(initial = null)

                        DeckRenamingDialog(
                            deckName = receivedDeck.name,
                            eventMessage = eventMessage,
                            onConfirmRenamingClick = { newName ->
                                viewModel.renameDeck(deck = receivedDeck, newName = newName)
                            },
                            onCloseDialogClick = ::closeDialog,
                        )
                    }
                }
            }
        }
    }

    private fun closeDialog() {
        viewModel.handleNavigation(event = IDeckListNavigationEvent.ToPrevious)
    }
}
