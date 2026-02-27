package com.kuts.klaf.deckManagment

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.kuts.klaf.common.BaseMainViewModel
import com.kuts.klaf.navigation.CollectFlowWithLifecycle
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun DeckManagementDestination(
    sharedViewModel: BaseMainViewModel,
    deckId: Int,
) {
    val viewModel: BaseDeckManagementViewModel = koinViewModel(parameters = { parametersOf(deckId) })

    CollectFlowWithLifecycle(flow = viewModel.eventMessage, onEach = sharedViewModel::notify)

    Surface {
        DeckManagementScreen(
            deckManagementState = viewModel.deckManagementState.collectAsState().value,
            sendAction = viewModel::sendAction,
        )
    }
}
