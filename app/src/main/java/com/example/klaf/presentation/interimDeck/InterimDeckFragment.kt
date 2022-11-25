package com.example.klaf.presentation.interimDeck

import android.os.Bundle
import android.view.View
import androidx.compose.material.Surface
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.klaf.R
import com.example.klaf.presentation.common.collectWhenStarted
import com.example.klaf.presentation.common.log
import com.example.klaf.presentation.interimDeck.InterimDeckNavigationDestination.*
import com.example.klaf.presentation.theme.MainTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InterimDeckFragment : Fragment(R.layout.fragment_interim_deck) {

    private val viewModel: BaseInterimDeckViewModel by viewModels<InterimDeckViewModel>()

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
            lifecycleScope = lifecycleScope
        ) { destination ->
            when (destination) {
                CardAddingFragmentDestination -> TODO()
                CardDeletingDialogDestination -> TODO()
                CardMovingDialogDestination -> TODO()
            }
        }
    }
}