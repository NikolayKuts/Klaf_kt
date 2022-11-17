package com.example.klaf.presentation.deckList.dataSynchronization

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ComposeView
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.example.klaf.R
import com.example.klaf.presentation.common.TransparentDialogFragment
import com.example.klaf.presentation.deckList.common.BaseDeckListViewModel
import com.example.klaf.presentation.theme.MainTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DataSynchronizationDialogFragment : TransparentDialogFragment(
    R.layout.dialog_data_synchronization
) {

    private val viewModel by navGraphViewModels<BaseDeckListViewModel>(R.id.deckListFragment)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ComposeView>(R.id.compose_view_data_synchronization).setContent {
            MainTheme {
                DataSynchronizationDialogView(
                    viewModel = viewModel,
                    onClose = ::closeDialog,
                )
            }
        }
    }

    private fun closeDialog() {
        findNavController().popBackStack()
    }
}