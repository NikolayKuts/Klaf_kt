package com.kuts.klaf.cardManagement.cardEditing

import android.os.Bundle
import android.view.View
import androidx.compose.material3.Surface
import androidx.compose.ui.platform.ComposeView
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.kuts.klaf.presentation.R
import com.kuts.klaf.cardManagement.cardAddition.CardManagementScreen
import com.kuts.klaf.cardManagement.common.CardManagementState
import com.kuts.klaf.common.BaseFragment
import com.kuts.klaf.common.collectWhenStarted
import com.kuts.klaf.theme.MainTheme
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class CardEditingFragment : BaseFragment(layoutId = R.layout.common_compose_layout) {

    private val args by navArgs<CardEditingFragmentArgs>()

    private val viewModel: CardEditingViewModel by viewModel {
        parametersOf(
            args.deckId,
            args.cardId
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.audioPlayer.onCreate()
    }

    override fun onResume() {
        super.onResume()
        viewModel.audioPlayer.onResume()
    }

    override fun onStop() {
        viewModel.audioPlayer.onStop()
        super.onStop()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeEventMessage()
        observeCardEditingState()

        view.findViewById<ComposeView>(R.id.compose_view).setContent {
            MainTheme {
                Surface {
                    CardManagementScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onDestroy() {
        viewModel.audioPlayer.onDestroy()
        super.onDestroy()
    }

    private fun observeEventMessage() {
        viewModel.eventMessage.collectWhenStarted(
            lifecycleOwner = viewLifecycleOwner,
            onEach = sharedViewModel::notify
        )
    }

    private fun observeCardEditingState() {
        viewModel.cardManagementState.collectWhenStarted(
            lifecycleOwner = viewLifecycleOwner
        ) { managementState ->
            when (managementState) {
                is CardManagementState.InProgress -> {}
                is CardManagementState.Finished -> findNavController().popBackStack()
            }
        }
    }

}
