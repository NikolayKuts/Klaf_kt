package com.kuts.klaf.presentation.deckManagment

import android.os.Bundle
import android.view.View
import androidx.compose.material.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.kuts.klaf.R
import com.kuts.klaf.presentation.common.BaseFragment
import com.kuts.klaf.presentation.common.collectWhenStarted
import com.kuts.klaf.presentation.theme.MainTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DeckManagementFragment : BaseFragment(R.layout.common_compose_layout) {

    private val args by navArgs<DeckManagementFragmentArgs>()
    private val navController by lazy { findNavController() }

    @Inject
    lateinit var assistedFactory: DeckManagementAssistedViewModelFactory
    private val viewModel: BaseDeckManagementViewModel by viewModels<DeckManagementViewModel> {
        DeckManagementViewModelFactory(assistedFactory = assistedFactory, deckId = args.deckId)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeEventMessage()
//        observeManagementEvent()

        view.findViewById<ComposeView>(R.id.compose_view).setContent {
            MainTheme {
                Surface {
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

//    private fun handleDeckManagementEvent(event: DeckManagementEvent) {
//        when (event) {
//           is DeckManagementEvent.ShowScheduledDateIntervalChangeDialog -> {
//
//           }
//        }
//    }
}