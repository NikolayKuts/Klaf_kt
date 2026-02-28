package com.kuts.klaf.cardViewing

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import com.kuts.klaf.common.BaseMainViewModel
import com.kuts.klaf.navigation.CollectFlowWithLifecycle
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun CardViewingDestination(
    sharedViewModel: BaseMainViewModel,
    deckId: Int,
) {
    val viewModel: CardViewingViewModel = koinViewModel(parameters = { parametersOf(deckId) })

    CollectFlowWithLifecycle(flow = viewModel.eventMessage, onEach = sharedViewModel::notify)

    Surface {
        CardViewingScreen(viewModel = viewModel)
    }
}
