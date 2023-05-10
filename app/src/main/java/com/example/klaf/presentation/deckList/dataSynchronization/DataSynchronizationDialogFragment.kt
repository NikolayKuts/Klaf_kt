package com.example.klaf.presentation.deckList.dataSynchronization

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.activityViewModels
import androidx.navigation.navGraphViewModels
import com.example.klaf.R
import com.example.klaf.presentation.common.BaseMainViewModel
import com.example.klaf.presentation.common.MainViewModel
import com.example.klaf.presentation.common.TransparentDialogFragment
import com.example.klaf.presentation.deckList.common.BaseDeckListViewModel
import com.example.klaf.presentation.deckList.common.DeckListNavigationEvent
import com.example.klaf.presentation.theme.MainTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DataSynchronizationDialogFragment : TransparentDialogFragment(
    layoutId = R.layout.common_compose_layout
) {

    private val sharedViewModel: BaseMainViewModel by activityViewModels<MainViewModel>()
    private val viewModel by navGraphViewModels<BaseDeckListViewModel>(R.id.deckListFragment)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ComposeView>(R.id.compose_view).setContent {
            MainTheme {
                val eventMessage by sharedViewModel.eventMessage.collectAsState(initial = null)

                DataSynchronizationDialogView(
                    synchronizationState = viewModel.dataSynchronizationState.collectAsState().value,
                    onConfirmClick = viewModel::synchronizeData,
                    onCloseClick = {
                        viewModel.handleNavigation(event = DeckListNavigationEvent.ToPrevious)
                    },
                    onDispose = viewModel::resetSynchronizationState,
                    eventMassage = eventMessage
                )
            }
        }
    }
}