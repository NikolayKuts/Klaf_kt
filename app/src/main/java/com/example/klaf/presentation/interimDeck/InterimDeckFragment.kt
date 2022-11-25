package com.example.klaf.presentation.interimDeck

import android.os.Bundle
import android.view.View
import androidx.compose.material.Surface
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.klaf.R
import com.example.klaf.presentation.common.LifecycleObservingLogger
import com.example.klaf.presentation.common.collectWhenStarted
import com.example.klaf.presentation.common.log
import com.example.klaf.presentation.interimDeck.InterimDeckNavigationDestination.*
import com.example.klaf.presentation.theme.MainTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InterimDeckFragment : Fragment(R.layout.fragment_interim_deck) {

    private val viewModel: BaseInterimDeckViewModel by viewModels<InterimDeckViewModel>()
    private val navController by lazy { findNavController() }
    val observer = LifecycleObservingLogger(ownerName = "lifecycle -> $this")

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeNavigationChanges()
        view.findViewById<ComposeView>(R.id.compose_view_interim_deck).setContent {
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
                is CardAddingFragmentDestination -> {
                    navigateToCardAdditionDialog(interimDeckId = destination.interimDeckId)
                }
                CardDeletingDialogDestination -> {
                    TODO()
                }
                CardMovingDialogDestination -> {
                    TODO()
                }
            }
        }
    }

    private fun navigateToCardAdditionDialog(interimDeckId: Int) {
        InterimDeckFragmentDirections.actionInterimDeckFragmentToCardAdditionFragment(
            deckId = interimDeckId
        ).also { navController.navigate(directions = it) }
    }
}