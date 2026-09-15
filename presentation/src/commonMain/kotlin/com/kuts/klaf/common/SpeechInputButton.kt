package com.kuts.klaf.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kuts.klaf.presentation.resources.Res
import com.kuts.klaf.presentation.resources.ic_mic_24
import com.kuts.klaf.presentation.resources.mnemonic_comment_dictate_action
import com.kuts.klaf.theme.MainTheme
import org.jetbrains.compose.resources.stringResource

private const val SPEECH_INPUT_BUTTON_SIZE = 40
private val PROGRESS_RING_INSET = 6.dp

/**
 * A round microphone button that wears its own progress ring while a dictation session is running,
 * so the field it belongs to keeps its place in the layout.
 */
@Composable
fun SpeechInputButton(
    isBusy: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = SPEECH_INPUT_BUTTON_SIZE.dp,
) {
    val ringSize = size + PROGRESS_RING_INSET
    val background = when {
        isBusy -> MainTheme.colors.common.klafServerReconnectingButton
        enabled -> MainTheme.colors.common.positiveDialogButton
        else -> MainTheme.colors.common.separator.copy(alpha = 0.24f)
    }

    Box(
        modifier = modifier.size(ringSize),
        contentAlignment = Alignment.Center,
    ) {
        RoundButton(
            background = background,
            iconRes = Res.drawable.ic_mic_24,
            enabled = enabled,
            onClick = onClick,
            size = size,
            contentDescription = stringResource(Res.string.mnemonic_comment_dictate_action),
        )

        if (isBusy) {
            CircularProgressIndicator(
                modifier = Modifier.size(ringSize),
                strokeWidth = 2.dp,
            )
        }
    }
}
