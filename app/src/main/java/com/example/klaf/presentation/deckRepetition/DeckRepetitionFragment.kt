package com.example.klaf.presentation.deckRepetition

import android.os.Bundle
import android.view.View
import androidx.compose.material.Surface
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.example.klaf.R
import com.example.klaf.presentation.common.collectWhenStarted
import com.example.klaf.presentation.common.log
import com.example.klaf.presentation.common.showToast
import com.example.klaf.presentation.theme.MainTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DeckRepetitionFragment : Fragment(R.layout.fragment_deck_repetion) {

    private val args by navArgs<DeckRepetitionFragmentArgs>()

    @Inject
    lateinit var assistedFactory: RepetitionViewModelAssistedFactory
    private val viewModel: DeckRepetitionViewModel by navGraphViewModels(R.id.deckRepetitionFragment) {
        RepetitionViewModelFactory(assistedFactory = assistedFactory, deckId = args.deckId)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<ComposeView>(R.id.compose_view_deck_repetition).setContent {
            MainTheme {
                Surface {
                    DeckRepetitionScreen(
                        viewModel = viewModel,
                        onDeleteCardClick = ::navigateToCardRemovingDialogFragment,
                        onAddCardClick = ::navigateToCardAdditionFragment,
                        onEditCardClick = ::navigateToCardEditingFragment,
                        onFinishRepetition = ::navigateToDeckRepetitionInfoDialogFragment,
                    )
                }
            }
        }

        subscribeObservers()
    }

    override fun onDestroy() {
        super.onDestroy()
        removeObservers()
    }

    private fun subscribeObservers() {
        setEventMessageObserver()
        lifecycle.addObserver(viewModel.timer)
        lifecycle.addObserver(viewModel.audioPlayer)
    }

    private fun removeObservers() {
        lifecycle.removeObserver(viewModel.timer)
        lifecycle.removeObserver(viewModel.audioPlayer)
    }

    private fun setEventMessageObserver() {
        viewModel.eventMessage.collectWhenStarted(viewLifecycleOwner.lifecycleScope) { eventMessage ->
            requireContext().showToast(messageId = eventMessage.resId)
        }
    }

    private fun navigateToCardEditingFragment(cardId: Int) {
        DeckRepetitionFragmentDirections.actionDeckRepetitionFragmentToCardEditingFragment(
            cardId = cardId,
            deckId = args.deckId
        ).also { findNavController().navigate(it) }
    }

    private fun navigateToCardAdditionFragment() {
        DeckRepetitionFragmentDirections.actionDeckRepetitionFragmentToCardAdditionFragment(
            deckId = args.deckId,
        ).also { findNavController().navigate(it) }
    }

    private fun navigateToCardRemovingDialogFragment(cardId: Int) {
        DeckRepetitionFragmentDirections.actionDeckRepetitionFragmentToCardRemovingDialogFragment(
            deckId = args.deckId,
            cardId = cardId
        ).also { findNavController().navigate(it) }
    }

    private fun navigateToDeckRepetitionInfoDialogFragment(
        currentDuration: String,
        lastDuration: String,
        scheduledDate: Long,
        previousScheduledDate: Long,
        lastRepetitionIterationDate: String?,
        repetitionQuantity: String,
        lastSuccessMark: String,
    ) {
        DeckRepetitionFragmentDirections
            .actionDeckRepetitionFragmentToDeckRepetitionInfoDialogFragment(
                currentDuration = currentDuration,
                lastDuration = lastDuration,
                scheduledDate = scheduledDate,
                previusScheduledDate = previousScheduledDate,
                lastRepetitionIterationDate = lastRepetitionIterationDate,
                repetitionQuantity = repetitionQuantity,
                lastSuccessMark = lastSuccessMark
            ).also { findNavController().navigate(directions = it) }
    }
}