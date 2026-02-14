package com.kuts.klaf.deckRepetitionInfo

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.kuts.klaf.presentation.R
import com.kuts.klaf.common.*
import com.kuts.klaf.deckRepetitionInfo.RepetitionInfoEvent.*
import com.kuts.klaf.theme.MainTheme
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class DeckRepetitionInfoDialogFragment : TransparentDialogFragment(
    layoutId = R.layout.common_compose_layout
) {

    private val args by navArgs<DeckRepetitionInfoDialogFragmentArgs>()

    private val viewModel by viewModel<DeckRepetitionInfoViewModel> {
        parametersOf(args.deckId)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeEventMessage()

        view.findViewById<ComposeView>(R.id.compose_view).setContent {
            MainTheme {
                TransparentSurface {
                    DeckRepetitionInfoView(
                        viewModel = viewModel,
                        deckName = args.deckName,
                        onCloseClick = ::closeDialog,
                        eventMessage = sharedViewModel.eventMessage.collectAsState(initial = null).value,
                        onRendered = ::handleRepetitionInfoEvent,
                    )
                }
            }
        }
    }

    private fun observeEventMessage() {
        viewModel.eventMessage.collectWhenStarted(
            lifecycleOwner = viewLifecycleOwner
        ) { eventMessage ->
            sharedViewModel.notify(message = eventMessage)
            closeDialog()
        }
    }

    private fun handleRepetitionInfoEvent() {
        val eventMessage = when (args.repetitionInfoEvent) {
            ScheduledSuccessfully -> {
                EventMessage(
                    resId = R.string.deck_repetition_scheduled_successfully,
                    type = EventMessage.Type.Positive
                )
            }
            SchedulingFailed -> {
                EventMessage(
                    resId = R.string.deck_repetition_scheduling_failed,
                    type = EventMessage.Type.Negative
                )
            }
            OneRepetitionToFinish -> {
                EventMessage(resId = R.string.deck_repetition_one_repetition_to_finish_iteration)
            }
            Non -> return
        }

        sharedViewModel.notify(message = eventMessage)
        resetRepetitionInfoEventArgument()
    }

    private fun closeDialog() {
        findNavController().popBackStack()
    }

    private fun resetRepetitionInfoEventArgument() {
        arguments?.putSerializable(args::repetitionInfoEvent.name, Non)
    }
}
