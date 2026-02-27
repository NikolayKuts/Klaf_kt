package com.kuts.klaf.cardManagement.cardAddition

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavBackStackEntry
import com.kuts.domain.common.ifTrue
import com.kuts.klaf.common.BaseMainViewModel
import com.kuts.klaf.navigation.CollectFlowWithLifecycle
import com.kuts.klaf.navigation.ObserveAudioLifecycle
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

private const val MIME_TYPE_TEXT_PLAIN = "text/plain"

@Composable
internal fun CardAdditionDestination(
    backStackEntry: NavBackStackEntry,
    sharedViewModel: BaseMainViewModel,
    deckId: Int,
) {
    val context = LocalContext.current

    val viewModel: CardAdditionViewModel = koinViewModel(
        viewModelStoreOwner = backStackEntry,
        parameters = {
            parametersOf(
                deckId,
                context.retrieveSmartSelectedWord(),
            )
        },
    )

    ObserveAudioLifecycle(
        onCreate = viewModel.audioPlayer::onCreate,
        onResume = viewModel.audioPlayer::onResume,
        onStop = viewModel.audioPlayer::onStop,
        onDestroy = viewModel.audioPlayer::onDestroy,
    )

    CollectFlowWithLifecycle(flow = viewModel.eventMessage, onEach = sharedViewModel::notify)

    Surface {
        CardManagementScreen(viewModel = viewModel)
    }
}

private fun Context.retrieveSmartSelectedWord(): String? {
    val activity = this as? Activity ?: return null

    val intent = activity.intent ?: return null
    if (intent.action != Intent.ACTION_PROCESS_TEXT) {
        return null
    }

    val selectedWord = intent.type
        ?.startsWith(MIME_TYPE_TEXT_PLAIN)
        ?.ifTrue { intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString() }

    if (selectedWord != null) {
        intent.removeExtra(Intent.EXTRA_PROCESS_TEXT)
        intent.action = Intent.ACTION_MAIN
    }

    return selectedWord
}
