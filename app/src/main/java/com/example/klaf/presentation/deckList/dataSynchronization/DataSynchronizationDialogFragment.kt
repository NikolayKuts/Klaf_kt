package com.example.klaf.presentation.deckList.dataSynchronization

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.example.klaf.R
import com.example.klaf.presentation.common.TransparentDialogFragment
import com.example.klaf.presentation.common.collectWhenStarted
import com.example.klaf.presentation.deckList.common.BaseDeckListViewModel
import com.example.klaf.presentation.deckList.common.DeckListNavigationDestination
import com.example.klaf.presentation.theme.MainTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DataSynchronizationDialogFragment : TransparentDialogFragment(
    R.layout.common_compose_layout
) {

    private val viewModel by navGraphViewModels<BaseDeckListViewModel>(R.id.deckListFragment)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        view.findViewById<ComposeView>(R.id.compose_view).setContent {
            MainTheme {
                DataSynchronizationDialogView(
                    synchronizationState = viewModel.dataSynchronizationState.collectAsState().value,
                    onConfirmClick = viewModel::synchronizeData,
                    onCloseClick = ::closeDialog,
                    onDispose = viewModel::resetSynchronizationState
                )
            }
        }

        observeNavigationState()
    }

    private fun observeNavigationState() {
        viewModel.navigationDestination.collectWhenStarted(
            lifecycleOwner = viewLifecycleOwner
        ) { destination ->
            if (destination == DeckListNavigationDestination.SigningTypeChoosingDialog) {
                findNavController().navigate(
                    R.id.action_dataSynchronizationDialogFragment_to_signingTypeChoosingDialogFragment
                )
            }
        }
    }

    private fun closeDialog() {
        findNavController().popBackStack()
    }
}