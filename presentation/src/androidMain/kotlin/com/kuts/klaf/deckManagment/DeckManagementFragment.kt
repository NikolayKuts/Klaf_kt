package com.kuts.klaf.deckManagment

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.kuts.klaf.presentation.R
import com.kuts.klaf.common.BaseFragment
import com.kuts.klaf.common.TransparentSurface
import com.kuts.klaf.common.collectWhenStarted
import com.kuts.klaf.theme.MainTheme
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class DeckManagementFragment : BaseFragment(R.layout.common_compose_layout) {

    private val args by navArgs<DeckManagementFragmentArgs>()
    private val navController by lazy { findNavController() }

    private val viewModel: BaseDeckManagementViewModel by viewModel<DeckManagementViewModel> {
        parametersOf(args.deckId)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeEventMessage()
//        observeManagementEvent()

        view.findViewById<ComposeView>(R.id.compose_view).setContent {
            MainTheme {
                TransparentSurface {
                    DeckManagementScreen(
                        deckManagementState = viewModel.deckManagementState.collectAsState().value,
                        sendAction = viewModel::sendAction
                    )
                }
            }
        }
    }

    private fun observeEventMessage() {
        viewModel.eventMessage.collectWhenStarted(
            lifecycleOwner = viewLifecycleOwner,
            onEach = sharedViewModel::notify,
        )
    }

//    private fun observeManagementEvent() {
//        viewModel.event.collectWhenStarted(
//            lifecycleOwner = viewLifecycleOwner,
//            onEach = ::handleDeckManagementEvent,
//        )
//    }

//    private fun handleDeckManagementEvent(event: IDeckManagementEvent) {
//        when (event) {
//           is IDeckManagementEvent.ShowScheduledDateIntervalChangeDialog -> {
//
//           }
//        }
//    }
}
