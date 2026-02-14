package com.kuts.klaf.deckList.deckDeleting

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.kuts.klaf.presentation.R
import com.kuts.klaf.common.TransparentDialogFragment
import com.kuts.klaf.deckList.common.BaseDeckListViewModel
import com.kuts.klaf.deckList.common.IDeckListNavigationEvent
import com.kuts.klaf.theme.MainTheme

class DeckDeletingDialogFragment : TransparentDialogFragment(R.layout.common_compose_layout) {

    private val args by navArgs<DeckDeletingDialogFragmentArgs>()

    private val viewModel by navGraphViewModels<BaseDeckListViewModel>(R.id.deckListFragment)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ComposeView>(R.id.compose_view).setContent {
            MainTheme {
                val eventMassage by sharedViewModel.eventMessage.collectAsState(initial = null)

                DeckDeletionDialogView(
                    deckName = args.deckName,
                    eventMessage = eventMassage,
                    onCloseDialogClick = ::closeDialog,
                    onConfirmDeckDeletingButtonClick = ::deleteDeck
                )
            }
        }
    }

    private fun closeDialog() {
        viewModel.handleNavigation(event = IDeckListNavigationEvent.ToPrevious)
    }

    private fun deleteDeck() {
        viewModel.deleteDeck(deckId = args.deckId)
    }
}