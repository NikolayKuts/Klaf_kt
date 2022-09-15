package com.example.klaf.presentation.deckList.common

import android.os.Bundle
import android.view.View
import androidx.compose.material.Surface
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.example.klaf.R
import com.example.klaf.domain.entities.Deck
import com.example.klaf.presentation.common.collectWhenStarted
import com.example.klaf.presentation.common.showSnackBar
import com.example.klaf.presentation.theme.MainTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DeckListFragment : Fragment(R.layout.fragment_deck_list) {

    private val navController by lazy { findNavController() }

    @Inject
    lateinit var assistedFactory: DeckListViewModelAssistedFactory
    private val viewModel: DeckListViewModel by navGraphViewModels(R.id.deckListFragment) {
        DeckListViewModelFactory(assistedFactory = assistedFactory)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setEvenMessageObserver(view = view)

        view.findViewById<ComposeView>(R.id.compose_view_deck_list).setContent {
            MainTheme {
                Surface {
                    DeckListScreen(
                        viewModel = viewModel,
                        onItemClick = ::navigateToRepeatFragment,
                        onLongItemClick = ::navigateToDeckNavigationDialog,
                        onMainButtonClick = ::navigateToDeckCreationDialog,
                        onSwipeRefresh = ::navigateToDataSynchronizationDialog
                    )
                }
            }
        }
    }

    private fun setEvenMessageObserver(view: View) {
        viewModel.eventMessage.collectWhenStarted(
            lifecycleScope = viewLifecycleOwner.lifecycleScope
        ) { eventMessage ->
            view.showSnackBar(messageId = eventMessage.resId)
        }
    }

    private fun navigateToDeckCreationDialog() {
        navController.navigate(R.id.action_deckListFragment_to_deckCreationDialogFragment)
    }

    private fun navigateToRepeatFragment(deck: Deck) {
        DeckListFragmentDirections.actionDeckListFragmentToDeckRepetitionFragment(
            deckId = deck.id
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
}
