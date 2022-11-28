package com.example.klaf.presentation.interimDeck.common

import android.os.Bundle
import android.view.View
import androidx.compose.material.Surface
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.example.klaf.R
import com.example.klaf.presentation.common.LifecycleObservingLogger
import com.example.klaf.presentation.common.collectWhenStarted
import com.example.klaf.presentation.interimDeck.common.InterimDeckNavigationDestination.*
import com.example.klaf.presentation.theme.MainTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class InterimDeckFragment : Fragment(R.layout.fragment_interim_deck) {

    @Inject
    lateinit var assistedFactory: InterimDeckViewModelAssistedFactory
    private val viewModel: BaseInterimDeckViewModel by navGraphViewModels(
        navGraphId = R.id.interimDeckFragment
    ) {
        InterimDeckViewModuleFactory(assistedFactory = assistedFactory)
    }
    private val navController by lazy { findNavController() }
    val observer = LifecycleObservingLogger(ownerName = "lifecycle -> $this")

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeNavigationChanges()
        view.findViewById<ComposeView>(R.id.compose_view_dialog).setContent {
            MainTheme {
                Surface() {
                    InterimDeckScreen(viewModel = viewModel)
                }
            }
        }
    }

    private fun observeNavigationChanges() {
        viewModel.navigationDestination.collectWhenStarted(
            lifecycleScope = viewLifecycleOwner.lifecycleScope
        ) { destination ->
            when (destination) {
                CardMovingDialogDestination -> navigateToCardMovingDialog()
                is CardAddingFragmentDestination -> {
                    navigateToCardAdditionDialog(interimDeckId = destination.interimDeckId)
                }
                is CardDeletingDialogDestination -> {
                    navigateToCardDeletingDialog(cardQuantity = destination.cardQuantity)
                }
                InterimDeckFragment -> navController.popBackStack()
            }
        }
    }

    private fun navigateToCardMovingDialog() {
        navController.navigate(R.id.action_interimDeckFragment_to_cardMovingDialogFragment)
    }

    private fun navigateToCardAdditionDialog(interimDeckId: Int) {
        InterimDeckFragmentDirections.actionInterimDeckFragmentToCardAdditionFragment(
            deckId = interimDeckId
        ).also { navController.navigate(directions = it) }
    }

    private fun navigateToCardDeletingDialog(cardQuantity: Int) {
        InterimDeckFragmentDirections.actionInterimDeckFragmentToCardDeletingDialogFragment(
            cardQuantity = cardQuantity
        ).also { navController.navigate(directions = it) }
    }
}