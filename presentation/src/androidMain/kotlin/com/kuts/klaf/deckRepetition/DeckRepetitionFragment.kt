package com.kuts.klaf.deckRepetition

import android.os.Bundle
import android.view.View
import androidx.compose.material3.Surface
import androidx.compose.ui.platform.ComposeView
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.kuts.klaf.presentation.R
import com.kuts.klaf.common.BaseFragment
import com.kuts.klaf.common.collectWhenStarted
import com.kuts.klaf.deckRepetitionInfo.RepetitionInfoEvent
import com.kuts.klaf.theme.MainTheme
import com.lib.lokdroid.core.logD
import org.koin.androidx.navigation.koinNavGraphViewModel
import org.koin.core.parameter.parametersOf

class DeckRepetitionFragment : BaseFragment(layoutId = R.layout.common_compose_layout) {

    private val args by navArgs<DeckRepetitionFragmentArgs>()
    private val navController by lazy { findNavController() }

    private val viewModel: BaseDeckReviewViewModel by koinNavGraphViewModel(
        navGraphId = R.id.deckRepetitionFragment,
    ) {
        parametersOf(args.deckId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        observeScreenState()
        lifecycle.addObserver(viewModel.timer)
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

        view.findViewById<ComposeView>(R.id.compose_view).setContent {
            MainTheme {
                Surface {
                    DeckReviewScreen(
                        viewModel = viewModel,
                        onDeleteCardClick = ::navigateToCardRemovingDialogFragment,
                        onAddCardClick = ::navigateToCardAdditionFragment,
                        onEditCardClick = ::navigateToCardEditingFragment,
                    )
                }
            }
        }

        observeEventMessage()
    }

    override fun onDestroy() {
        viewModel.audioPlayer.onDestroy()
        lifecycle.removeObserver(viewModel.timer)
        super.onDestroy()
    }

    private fun observeEventMessage() {
        viewModel.eventMessage.collectWhenStarted(
            lifecycleOwner = viewLifecycleOwner,
            onEach = sharedViewModel::notify,
        )
    }

    private fun observeScreenState() {
        viewModel.screenState.collectWhenStarted(lifecycleOwner = this) { state ->
            logD("observeScreenState(). state: $state")
            if (
                state is RepetitionScreenState.FinishState
                && state.repetitionInfoEvent != RepetitionInfoEvent.Non
            ) {
                navigateToDeckRepetitionInfoDialogFragment(infoEvent = state.repetitionInfoEvent)
            }
        }
    }

    private fun navigateToCardEditingFragment(cardId: Int) {
        DeckRepetitionFragmentDirections.actionDeckRepetitionFragmentToCardEditingFragment(
            cardId = cardId,
            deckId = args.deckId
        ).also { navController.navigate(it) }
    }

    private fun navigateToCardAdditionFragment() {
        DeckRepetitionFragmentDirections.actionDeckRepetitionFragmentToCardAdditionFragment(
            deckId = args.deckId,
        ).also { navController.navigate(it) }
    }

    private fun navigateToCardRemovingDialogFragment(cardId: Int) {
        DeckRepetitionFragmentDirections.actionDeckRepetitionFragmentToCardRemovingDialogFragment(
            deckId = args.deckId,
            cardId = cardId
        ).also { navController.navigate(it) }
    }

    private fun navigateToDeckRepetitionInfoDialogFragment(infoEvent: RepetitionInfoEvent) {
        DeckRepetitionFragmentDirections
            .actionDeckRepetitionFragmentToDeckRepetitionInfoDialogFragment(
                deckId = args.deckId,
                deckName = args.deckName,
                repetitionInfoEvent = infoEvent,
            ).also { navController.navigate(it) }
    }
}
