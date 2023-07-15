package com.kuts.klaf.presentation.deckRepetition

import android.os.Bundle
import android.view.View
import androidx.compose.material.Surface
import androidx.compose.ui.platform.ComposeView
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.kuts.klaf.R
import com.kuts.klaf.presentation.common.BaseFragment
import com.kuts.klaf.presentation.common.collectWhenStarted
import com.kuts.klaf.presentation.common.log
import com.kuts.klaf.presentation.deckRepetitionInfo.RepetitionInfoEvent
import com.kuts.klaf.presentation.theme.MainTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DeckRepetitionFragment : BaseFragment(layoutId = R.layout.common_compose_layout) {

    private val args by navArgs<DeckRepetitionFragmentArgs>()
    private val navController by lazy { findNavController() }

    @Inject
    lateinit var assistedFactory: RepetitionViewModelAssistedFactory
    private val viewModel: BaseDeckRepetitionViewModel
            by navGraphViewModels(R.id.deckRepetitionFragment) {
                RepetitionViewModelFactory(assistedFactory = assistedFactory, deckId = args.deckId)
            }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        observeScreenState()
        subscribeLifecycleObservers()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ComposeView>(R.id.compose_view).setContent {
            MainTheme {
                Surface {
                    DeckRepetitionScreen(
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
        super.onDestroy()

        unsubscribeLifecycleObservers()
    }

    private fun subscribeLifecycleObservers() {
        lifecycle.addObserver(viewModel.timer)
        lifecycle.addObserver(viewModel.audioPlayer)
    }

    private fun unsubscribeLifecycleObservers() {
        lifecycle.removeObserver(viewModel.timer)
        lifecycle.removeObserver(viewModel.audioPlayer)
    }

    private fun observeEventMessage() {
        viewModel.eventMessage.collectWhenStarted(
            lifecycleOwner = viewLifecycleOwner,
            onEach = sharedViewModel::notify,
        )
    }

    private fun observeScreenState() {
        viewModel.screenState.collectWhenStarted(lifecycleOwner = this) {
            if (it is RepetitionScreenState.FinishState) {
                viewModel.resetScreenState()
                navigateToDeckRepetitionInfoDialogFragment(infoEvent = it.repetitionInfoEvent)
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