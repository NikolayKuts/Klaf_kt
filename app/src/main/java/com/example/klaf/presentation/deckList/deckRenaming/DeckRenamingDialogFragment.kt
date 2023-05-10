package com.example.klaf.presentation.deckList.deckRenaming

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.example.domain.entities.Deck
import com.example.klaf.R
import com.example.klaf.presentation.common.BaseMainViewModel
import com.example.klaf.presentation.common.MainViewModel
import com.example.klaf.presentation.common.TransparentDialogFragment
import com.example.klaf.presentation.deckList.common.BaseDeckListViewModel
import com.example.klaf.presentation.deckList.common.DeckListNavigationEvent
import com.example.klaf.presentation.theme.MainTheme

class DeckRenamingDialogFragment : TransparentDialogFragment(R.layout.common_compose_layout) {

    private val args by navArgs<DeckRenamingDialogFragmentArgs>()

    private val sharedViewModel: BaseMainViewModel by activityViewModels<MainViewModel>()
    private val viewModel by navGraphViewModels<BaseDeckListViewModel>(R.id.deckListFragment)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ComposeView>(R.id.compose_view).setContent {
            viewModel.getDeckById(deckId = args.deckId).let { receivedDeck: Deck? ->
                if (receivedDeck == null) {
                    closeDialog()
                } else {
                    MainTheme {
                        val eventMessage =
                            sharedViewModel.eventMessage.collectAsState(initial = null).value

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
        viewModel.handleNavigation(event = DeckListNavigationEvent.ToPrevious)
    }
}