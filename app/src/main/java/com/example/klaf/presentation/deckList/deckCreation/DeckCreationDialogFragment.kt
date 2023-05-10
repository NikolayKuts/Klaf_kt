package com.example.klaf.presentation.deckList.deckCreation

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.activityViewModels
import androidx.navigation.navGraphViewModels
import com.example.domain.common.LoadingState
import com.example.klaf.R
import com.example.klaf.presentation.common.*
import com.example.klaf.presentation.deckList.common.BaseDeckListViewModel
import com.example.klaf.presentation.deckList.common.DeckListNavigationEvent
import com.example.klaf.presentation.theme.MainTheme

class DeckCreationDialogFragment : TransparentDialogFragment(R.layout.common_compose_layout) {

    private val sharedViewModel: BaseMainViewModel by activityViewModels<MainViewModel>()
    private val viewModel by navGraphViewModels<BaseDeckListViewModel>(R.id.deckListFragment)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeDeckCreationState()

        view.findViewById<ComposeView>(R.id.compose_view).setContent {
            MainTheme {
                TransparentSurface {
                    val message by sharedViewModel.eventMessage.collectAsState(initial = null)

                    DeckCreationDialog(
                        onConfirmCreationClick = ::confirmDeckCreation,
                        onCloseDialogClick = ::closeDialog,
                        eventMassage = message
                    )
                }
            }
        }
    }

    private fun observeDeckCreationState() {
        viewModel.deckCreationState.collectWhenStarted(
            lifecycleOwner = viewLifecycleOwner
        ) { deckCreationState ->
            when (deckCreationState) {
                is LoadingState.Success -> closeDialog()
                else -> {}
            }
        }
    }

    private fun confirmDeckCreation(deckName: String) {
        viewModel.createNewDeck(deckName = deckName)
    }

    private fun closeDialog() {
        viewModel.handleNavigation(event = DeckListNavigationEvent.ToPrevious)
    }
}