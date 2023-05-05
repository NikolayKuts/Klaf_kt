package com.example.klaf.presentation.deckList.common

import android.os.Bundle
import android.view.View
import androidx.compose.material.Surface
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.example.domain.entities.Deck
import com.example.klaf.R
import com.example.klaf.presentation.common.collectWhenStarted
import com.example.klaf.presentation.common.showSnackBar
import com.example.klaf.presentation.deckList.common.DeckListNavigationDestination.*
import com.example.klaf.presentation.theme.MainTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DeckListFragment : Fragment(R.layout.common_compose_layout) {

    private val navController by lazy { findNavController() }

    @Inject
    lateinit var assistedFactory: DeckListViewModelAssistedFactory
    private val viewModel: BaseDeckListViewModel by navGraphViewModels(R.id.deckListFragment) {
        DeckListViewModelFactory(assistedFactory = assistedFactory)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeEvenMessage(view = view)
        observeNavigationChanges()

        view.findViewById<ComposeView>(R.id.compose_view).setContent {
            MainTheme {
                Surface {
                    DeckListScreen(
                        viewModel = viewModel,
                        onMainButtonClick = ::navigateToDeckCreationDialog,
                        onRestartApp = ::restartApp,
                    )
                }
            }
        }
    }

    private fun observeEvenMessage(view: View) {
        viewModel.eventMessage.collectWhenStarted(
            lifecycleOwner = viewLifecycleOwner
        ) { eventMessage ->
            view.showSnackBar(messageId = eventMessage.resId)
        }
    }

    private fun observeNavigationChanges() {
        viewModel.navigationDestination.collectWhenStarted(
            lifecycleOwner = viewLifecycleOwner
        ) { destination ->
            when (destination) {
                DeckCreationDialog -> navigateToDeckCreationDialog()
                is DeckRepetitionScreen -> {
                    navigateToRepetitionFragment(deck = destination.deck)
                }
                is DeckNavigationDialog -> {
                    navigateToDeckNavigationDialog(deck = destination.deck)
                }
                DataSynchronizationDialog -> navigateToDataSynchronizationDialog()
                is CardTransferringScreen -> {
                    navigateCardTransferringFragment(deckId = destination.deckId)
                }
                SigningTypeChoosingDialog -> {}
            }
        }
    }

    private fun navigateToDeckCreationDialog() {
        navController.navigate(R.id.action_deckListFragment_to_deckCreationDialogFragment)
    }

    private fun navigateToRepetitionFragment(deck: Deck) {
        DeckListFragmentDirections.actionDeckListFragmentToDeckRepetitionFragment(
            deckId = deck.id,
            deckName = deck.name
        ).also { navController.navigate(it) }
    }

    private fun navigateToDeckNavigationDialog(deck: Deck) {
        DeckListFragmentDirections.actionDeckListFragmentToDeckNavigationDialog(
            deckId = deck.id,
            deckName = deck.name
        ).also { navController.navigate(directions = it) }
    }

    private fun navigateToDataSynchronizationDialog() {
        navController.navigate(R.id.action_deckListFragment_to_dataSynchronizationDialogFragment)
    }

    private fun navigateCardTransferringFragment(deckId: Int) {
        DeckListFragmentDirections.actionDeckListFragmentToCardTransferringFragment(
            sourceDeckId = deckId
        ).also { navController.navigate(directions = it) }
    }

    private fun restartApp() {
        viewModel.reopenApp()
        requireActivity().finish()
    }
}