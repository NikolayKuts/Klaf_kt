package com.kuts.klaf.deckList.deckCreation

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import com.kuts.klaf.presentation.R
import com.kuts.klaf.common.TransparentDialogFragment
import com.kuts.klaf.deckList.common.BaseDeckListViewModel
import com.kuts.klaf.deckList.common.IDeckListNavigationEvent
import com.kuts.klaf.theme.MainTheme
import org.koin.androidx.navigation.koinNavGraphViewModel

class DeckCreationDialogFragment : TransparentDialogFragment(R.layout.common_compose_layout) {

    private val viewModel by koinNavGraphViewModel<BaseDeckListViewModel>(R.id.deckListFragment)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ComposeView>(R.id.compose_view).setContent {
            MainTheme {
                val message by sharedViewModel.eventMessage.collectAsState(initial = null)

                DeckCreationDialog(
                    onConfirmCreationClick = ::confirmDeckCreation,
                    onCloseDialogClick = ::closeDialog,
                    eventMassage = message
                )
            }
        }
    }

    private fun confirmDeckCreation(deckName: String) {
        viewModel.createNewDeck(deckName = deckName)
    }

    private fun closeDialog() {
        viewModel.handleNavigation(event = IDeckListNavigationEvent.ToPrevious)
    }
}
