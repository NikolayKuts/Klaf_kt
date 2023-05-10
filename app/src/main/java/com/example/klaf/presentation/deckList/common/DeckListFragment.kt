package com.example.klaf.presentation.deckList.common

import android.os.Bundle
import android.view.View
import androidx.compose.material.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.example.domain.entities.Deck
import com.example.klaf.R
import com.example.klaf.presentation.common.BaseMainViewModel
import com.example.klaf.presentation.common.MainViewModel
import com.example.klaf.presentation.common.collectWhenStarted
import com.example.klaf.presentation.common.log
import com.example.klaf.presentation.deckList.common.DeckListNavigationEvent.*
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

    private val sharedViewModel: BaseMainViewModel by activityViewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        observeNavigationEvent()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeEvenMessage()

        view.findViewById<ComposeView>(R.id.compose_view).setContent {
            MainTheme {
                Surface {
                    DeckListScreen(
                        decks = viewModel.deckSource.collectAsState().value,
                        onItemClick = {
                            viewModel.handleNavigation(event = ToDeckRepetitionScreen(deck = it))
                        },
                        onLongItemClick = {
                            viewModel.handleNavigation(event = ToDeckNavigationDialog(deck = it))
                        },
                        onRefresh = {
                            viewModel.handleNavigation(event = ToDataSynchronizationDialog)
                        },
                        onMainButtonClick = {
                            viewModel.handleNavigation(event = ToDeckCreationDialog)
                        },
                        onRestartApp = ::restartApp,
                    )
                }
            }
        }
    }

    private fun observeEvenMessage() {
        viewModel.eventMessage.collectWhenStarted(
            lifecycleOwner = viewLifecycleOwner,
            onEach = sharedViewModel::notify
        )
    }

    private fun observeNavigationEvent() {
        viewModel.navigationEvent.collectWhenStarted(lifecycleOwner = this) { event ->
            log(event, "event")
            when (event) {
                ToDataSynchronizationDialog -> {
                    navigateToDataSynchronizationDialog()
                }
                ToDeckCreationDialog -> navigateToDeckCreationDialog()
                is ToDeckNavigationDialog -> {
                    navigateToDeckNavigationDialog(deck = event.deck)
                }
                is ToDeckRepetitionScreen -> {
                    navigateToRepetitionFragment(deck = event.deck)
                }
                ToPrevious -> navController.popBackStack()
                is ToCardTransferringScreen -> {
                    navigateCardTransferringFragment(deckId = event.deckId)
                }
                ToSigningTypeChoosingDialog -> {
                    navigateToSigningTypeChoosingDialog()
                }
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

    private fun navigateToSigningTypeChoosingDialog() {
        navController.navigate(
            R.id.action_dataSynchronizationDialogFragment_to_signingTypeChoosingDialogFragment
        )
    }

    private fun restartApp() {
        viewModel.reopenApp()
        requireActivity().finish()
    }
}