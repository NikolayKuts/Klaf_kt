package com.kuts.klaf.presentation.deckList.dataSynchronization

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kuts.domain.common.ifNotNull
import com.kuts.klaf.R
import com.kuts.klaf.data.common.DataSynchronizationState
import com.kuts.klaf.data.common.DataSynchronizationState.*
import com.kuts.klaf.presentation.common.*
import com.kuts.klaf.presentation.deckList.common.AnimatedSynchronizationLabel
import com.kuts.klaf.presentation.deckList.common.SynchronizationLabel
import com.kuts.klaf.presentation.theme.MainTheme

@Composable
fun DataSynchronizationDialogView(
    synchronizationState: DataSynchronizationState,
    onConfirmClick: () -> Unit,
    onCloseClick: () -> Unit,
    onDispose: () -> Unit,
    eventMassage: EventMessage?,
    onLaunched: () -> Unit,
) {
    ScrollableBox(
        modifier = Modifier.noRippleClickable { onCloseClick() },
        dialogMode = true,
        eventContent = {
            eventMassage.ifNotNull { EventMessageView(message = it) }
        }
    ) {
        when (synchronizationState) {
            Uncertain -> {}
            Initial -> {
                InitialStateView(
                    onCloseClick = onCloseClick,
                    onConfirmClick = onConfirmClick
                )
            }
            is Synchronizing -> {
                SynchronizationStateView(synchronizationData = synchronizationState.synchronizationData)
            }
            SuccessfullyFinished -> {
                FinishStateView(onCloseClick = onCloseClick)
            }
            Failed -> {
                FailureStateView(
                    onResynchronizeClick = onConfirmClick,
                    onCloseClick = onCloseClick)
            }
        }
        DisposableEffect(key1 = null) {
            onDispose { onDispose() }
        }

        LaunchedEffect(key1 = null) { onLaunched() }
    }
}

@Composable
private fun InitialStateView(
    onCloseClick: () -> Unit,
    onConfirmClick: () -> Unit,
) {
    FullBackgroundDialog(
        onBackgroundClick = onCloseClick,
        topContent = ContentHolder(size = ROUNDED_ELEMENT_SIZE.dp) { SynchronizationLabel() },
        mainContent = {
            Text(
                style = MainTheme.typographies.dialogTextStyle,
                text = stringResource(R.string.data_synchronization_dialog_title)
            )
        },
        bottomContent = {
            RoundButton(
                background = MainTheme.colors.common.positiveDialogButton,
                iconId = R.drawable.ic_confirmation_24,
                onClick = onConfirmClick
            )

            RoundButton(
                background = MainTheme.colors.common.neutralDialogButton,
                iconId = R.drawable.ic_close_24,
                onClick = onCloseClick
            )
        }
    )
}

@Composable
private fun SynchronizationStateView(synchronizationData: String) {
    FullBackgroundDialog(
        onBackgroundClick = { },
        topContent = ContentHolder(size = ROUNDED_ELEMENT_SIZE.dp) { AnimatedSynchronizationLabel() },
        mainContent = {
            Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                WarningMessage(textId = R.string.data_synchronization_dialog_waiting_message)
                ContentSpacer()
                SynchronizingText()

                if (synchronizationData.isNotEmpty()) {
                    ContentSpacer()
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = synchronizationData,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1
                    )
                }
            }
        },
    )
}

@Composable
private fun FinishStateView(onCloseClick: () -> Unit) {
    FullBackgroundDialog(
        onBackgroundClick = onCloseClick,
        topContent = ContentHolder(size = ROUNDED_ELEMENT_SIZE.dp) {
            RoundedIcon(
                background = MainTheme.colors.common.positiveDialogButton,
                iconId = R.drawable.ic_confirmation_24,
            )
        },
        mainContent = {
            Text(
                style = MainTheme.typographies.dialogTextStyle,
                text = stringResource(R.string.data_synchronization_dialog_data_synchronized)
            )
        },
        bottomContent = {
            RoundButton(
                background = MainTheme.colors.common.neutralDialogButton,
                iconId = R.drawable.ic_close_24,
                onClick = onCloseClick
            )
        }
    )
}

@Composable
private fun FailureStateView(
    onResynchronizeClick: () -> Unit,
    onCloseClick: () -> Unit,
) {
    FullBackgroundDialog(
        mainContentModifier = Modifier,
        onBackgroundClick = onCloseClick,
        topContent = ContentHolder(size = ROUNDED_ELEMENT_SIZE.dp) {
            RoundedIcon(
                background = MainTheme.colors.common.negativeDialogButton,
                iconId = R.drawable.ic_attention_mark_24,
            )
        },
        mainContent = {
            Column {
                WarningMessage(textId = R.string.data_synchronization_dialog_failure_message)
                ContentSpacer()
                Text(
                    modifier = Modifier.padding(6.dp),
                    text = stringResource(R.string.data_synchronization_dialog_resync_question),
                    style = MainTheme.typographies.dialogTextStyle
                )
            }
        },
        bottomContent = {
            RoundButton(
                background = MainTheme.colors.common.positiveDialogButton,
                iconId = R.drawable.ic_sync_24,
                onClick = onResynchronizeClick,
            )

            ClosingButton(onClick = onCloseClick)
        },
    )
}

@Composable
private fun SynchronizingText() {
    val animationDuration = 700
    val stepDuration = animationDuration / 3

    val alpha1 by animateAlphaWithDelay(
        delay = 50,
        commonDuration = animationDuration,
        stepDuration = stepDuration
    )
    val alpha2 by animateAlphaWithDelay(
        delay = stepDuration,
        commonDuration = animationDuration,
        stepDuration = stepDuration
    )
    val alpha3 by animateAlphaWithDelay(
        delay = stepDuration * 2,
        commonDuration = animationDuration,
        stepDuration = stepDuration
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            style = MainTheme.typographies.dialogTextStyle,
            text = stringResource(R.string.data_synchronization_dialog_sync_process)
        )
        TextDot(alpha1)
        TextDot(alpha2)
        TextDot(alpha3)
    }
}

@Composable
private fun TextDot(alpha: Float) {
    Text(
        modifier = Modifier.alpha(alpha),
        text = "."
    )
}

@Composable
private fun animateAlphaWithDelay(
    delay: Int,
    commonDuration: Int,
    stepDuration: Int,
    minAlpha: Float = 0.0f,
    maxAlpha: Float = 1F,
): State<Float> = rememberInfiniteTransition().animateFloat(
    initialValue = minAlpha,
    targetValue = minAlpha,
    animationSpec = infiniteRepeatable(
        animation = keyframes {
            durationMillis = commonDuration
            minAlpha at 0
            minAlpha at delay
            maxAlpha at delay + (stepDuration / 2)
            maxAlpha at commonDuration
        }
    )
)

@Composable
private fun ContentSpacer() {
    Spacer(modifier = Modifier.height(16.dp))
}