package com.kuts.klaf.presentation.deckList.deckNavigation

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.kuts.klaf.R
import com.kuts.klaf.presentation.common.TransparentDialogFragment
import com.kuts.klaf.presentation.deckList.common.BaseDeckListViewModel
import com.kuts.klaf.presentation.deckList.common.DeckListNavigationEvent
import com.kuts.klaf.presentation.theme.MainTheme

class DeckNavigationDialogFragment : TransparentDialogFragment(R.layout.common_compose_layout) {

    private val args by navArgs<DeckNavigationDialogFragmentArgs>()
    private val navController by lazy { findNavController() }

    val viewModel by navGraphViewModels<BaseDeckListViewModel>(R.id.deckListFragment)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ComposeView>(R.id.compose_view).setContent {
            MainTheme {
                DeckNavigationDialogView(
                    deckName = args.deckName,
                    eventMessage = sharedViewModel.eventMessage.collectAsState(initial = null).value,
                    onDeleteDeckClick = ::navigateToDeckRemovingDialogFragment,
                    onRenameDeckClick = ::navigateToDeckRenamingDialogFragment,
                    onBrowseDeckClick = ::navigateToCardViewerFragment,
                    onAddCardsClick = ::navigateToCardAdditionFragment,
                    onTransferCardsClick = ::navigateToCardTransferringFragment,
                    onRepetitionInfoClick = ::navigateToDeckRepetitionInfoDialogFragment,
                    onDeckManagementClick = ::navigateToDeckManagementFragment,
                    onCloseDialogClick = ::closeDialog,
                    onCopyDeckContentClick = ::generateChatGptStoryCrafterPrompt,
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
        ).also { navController.navigate(directions = it) }
    }

    private fun navigateToCardViewerFragment() {
        DeckNavigationDialogFragmentDirections.actionDeckNavigationDialogToCardViewerFragment(
            deckId = args.deckId,
            deckName = args.deckName
        ).also { navController.navigate(directions = it) }
    }

    private fun navigateToCardAdditionFragment() {
        DeckNavigationDialogFragmentDirections.actionDeckNavigationDialogToCardAdditionFragment(
            deckId = args.deckId,
        ).also { navController.navigate(directions = it) }
    }

    private fun navigateToCardTransferringFragment() {
        DeckNavigationDialogFragmentDirections.actionDeckNavigationDialogToCardTransferringFragment(
            sourceDeckId = args.deckId
        ).also { navController.navigate(directions = it) }
    }

    private fun navigateToDeckRepetitionInfoDialogFragment() {
        DeckNavigationDialogFragmentDirections
            .actionDeckNavigationDialogToDeckRepetitionInfoDialogFragment(
                deckId = args.deckId,
                deckName = args.deckName,
            ).also { navController.navigate(directions = it) }
    }

    private fun navigateToDeckManagementFragment() {
        DeckNavigationDialogFragmentDirections.actionDeckNavigationDialogToDeckManagementFragment(
            deckId = args.deckId,
        ).also { navController.navigate(directions = it) }
    }

    private fun closeDialog() {
        viewModel.handleNavigation(event = DeckListNavigationEvent.ToPrevious)
    }

    private fun generateChatGptStoryCrafterPrompt() {
        viewModel.generateGptPromptWithDeckContent(deckId = args.deckId)
    }
}