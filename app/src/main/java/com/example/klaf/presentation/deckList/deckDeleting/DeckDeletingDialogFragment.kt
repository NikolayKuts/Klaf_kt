package com.example.klaf.presentation.deckList.deckDeleting

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.example.klaf.R
import com.example.klaf.presentation.common.BaseMainViewModel
import com.example.klaf.presentation.common.MainViewModel
import com.example.klaf.presentation.common.TransparentDialogFragment
import com.example.klaf.presentation.deckList.common.BaseDeckListViewModel
import com.example.klaf.presentation.deckList.common.DeckListNavigationEvent
import com.example.klaf.presentation.theme.MainTheme

class DeckDeletingDialogFragment : TransparentDialogFragment(R.layout.common_compose_layout) {

    private val args by navArgs<DeckDeletingDialogFragmentArgs>()

    private val sharedViewModel: BaseMainViewModel by activityViewModels<MainViewModel>()
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
        viewModel.handleNavigation(event = DeckListNavigationEvent.ToPrevious)
    }

    private fun deleteDeck() {
        viewModel.deleteDeck(deckId = args.deckId)
    }
}