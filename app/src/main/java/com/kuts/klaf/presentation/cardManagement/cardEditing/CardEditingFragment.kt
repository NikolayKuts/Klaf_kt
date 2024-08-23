package com.kuts.klaf.presentation.cardManagement.cardEditing

import android.os.Bundle
import android.view.View
import androidx.compose.material.Surface
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.kuts.klaf.R
import com.kuts.klaf.presentation.cardManagement.cardAddition.CardManagementScreen
import com.kuts.klaf.presentation.cardManagement.common.BaseCardManagementViewModel
import com.kuts.klaf.presentation.cardManagement.common.CardManagementState
import com.kuts.klaf.presentation.common.BaseFragment
import com.kuts.klaf.presentation.common.collectWhenStarted
import com.kuts.klaf.presentation.theme.MainTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CardEditingFragment : BaseFragment(layoutId = R.layout.common_compose_layout) {

    private val args by navArgs<CardEditingFragmentArgs>()

    @Inject
    lateinit var cardEditingAssistedViewModelFactory: CardEditingAssistedViewModelFactory
    private val viewModel: BaseCardManagementViewModel by viewModels {
        CardEditingViewModelFactory(
            assistedFactory = cardEditingAssistedViewModelFactory,
            deckId = args.deckId,
            cardId = args.cardId
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        subscribeAudioPlayerObserver()
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
        super.onDestroy()

        unsubscribeAudioPlayerObserver()
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

    private fun subscribeAudioPlayerObserver() {
        lifecycle.addObserver(viewModel.audioPlayer)
    }

    private fun unsubscribeAudioPlayerObserver() {
        lifecycle.removeObserver(viewModel.audioPlayer)
    }
}