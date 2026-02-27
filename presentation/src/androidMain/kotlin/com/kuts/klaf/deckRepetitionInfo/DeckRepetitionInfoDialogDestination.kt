package com.kuts.klaf.deckRepetitionInfo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import com.kuts.klaf.common.BaseMainViewModel
import com.kuts.klaf.common.EventMessage
import com.kuts.klaf.navigation.CollectFlowWithLifecycle
import com.kuts.klaf.presentation.R
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun DeckRepetitionInfoDialogDestination(
    navController: NavHostController,
    sharedViewModel: BaseMainViewModel,
    deckId: Int,
    deckName: String,
    repetitionInfoEvent: RepetitionInfoEvent,
) {
    val viewModel: DeckRepetitionInfoViewModel = koinViewModel(
        parameters = { parametersOf(deckId) }
    )

    CollectFlowWithLifecycle(flow = viewModel.eventMessage) { eventMessage ->
        sharedViewModel.notify(message = eventMessage)
        navController.popBackStack()
    }

    var isRepetitionInfoEventHandled by remember(repetitionInfoEvent) {
        mutableStateOf(value = false)
    }

    DeckRepetitionInfoView(
        viewModel = viewModel,
        deckName = deckName,
        onCloseClick = { navController.popBackStack() },
        eventMessage = sharedViewModel.eventMessage.collectAsState(initial = null).value,
        onRendered = {
            if (isRepetitionInfoEventHandled) {
                return@DeckRepetitionInfoView
            }

            val eventMessage = when (repetitionInfoEvent) {
                RepetitionInfoEvent.ScheduledSuccessfully -> {
                    EventMessage(
                        resId = R.string.deck_repetition_scheduled_successfully,
                        type = EventMessage.Type.Positive,
                    )
                }

                RepetitionInfoEvent.SchedulingFailed -> {
                    EventMessage(
                        resId = R.string.deck_repetition_scheduling_failed,
                        type = EventMessage.Type.Negative,
                    )
                }

                RepetitionInfoEvent.OneRepetitionToFinish -> {
                    EventMessage(resId = R.string.deck_repetition_one_repetition_to_finish_iteration)
                }

                RepetitionInfoEvent.Non -> null
            }

            eventMessage?.let { sharedViewModel.notify(message = it) }
            isRepetitionInfoEventHandled = true
        },
    )
}
