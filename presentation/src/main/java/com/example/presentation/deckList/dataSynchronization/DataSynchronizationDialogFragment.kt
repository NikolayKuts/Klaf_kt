package com.example.presentation.deckList.dataSynchronization

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ComposeView
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.example.presentation.R
import com.example.presentation.common.TransparentDialogFragment
import com.example.presentation.deckList.common.BaseDeckListViewModel
import com.example.presentation.theme.MainTheme
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
                    onCloseClick = ::closeDialog,
                )
            }
        }
    }

    private fun closeDialog() {
        findNavController().popBackStack()
    }
}