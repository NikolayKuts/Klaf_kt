package com.kuts.klaf.presentation.deckList.dataSynchronization

import androidx.annotation.StringRes
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kuts.domain.common.ifNotNull
import com.kuts.klaf.R
import com.kuts.klaf.data.common.DataSynchronizationState
import com.kuts.klaf.data.common.DataSynchronizationState.*
import com.kuts.klaf.presentation.common.*
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
        topContent = {
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
private fun AnimatedSynchronizationLabel() {
    val animationDuration = 1000
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0F,
        targetValue = -360F,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = animationDuration,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        )
    )
    val color by infiniteTransition.animateColor(
        initialValue = MainTheme.colors.common.dialogBackground,
        targetValue = MainTheme.colors.dataSynchronizationView.targetLabelBackground,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = animationDuration,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        )
    )

    SynchronizationLabel(
        modifier = Modifier.rotate(degrees = rotation),
        color = color
    )
}

@Composable
private fun SynchronizationLabel(
    modifier: Modifier = Modifier,
    color: Color = MainTheme.colors.common.dialogBackground,
) {
    Card(
        modifier = Modifier
            .size(ROUNDED_ELEMENT_SIZE.dp)
            .noRippleClickable { },
        shape = RoundedCornerShape(ROUNDED_ELEMENT_SIZE.dp),
        elevation = 0.dp,
    ) {
        Icon(
            modifier = modifier
                .size(20.dp)
                .background(color)
                .padding(8.dp)
                .then(modifier),
            painter = painterResource(id = R.drawable.ic_sync_24),
            contentDescription = null,
        )
    }
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

@Composable
private fun WarningMessage(@StringRes textId: Int) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(6.dp))
            .background(animatedWarningColor())
            .padding(16.dp),
        text = stringResource(textId),
        style = MainTheme.typographies.dialogTextStyle
    )
}

@Composable
private fun animatedWarningColor(): Color = rememberInfiniteTransition()
    .animateColor(
        initialValue = MainTheme.colors.dataSynchronizationView.initialWarning,
        targetValue = MainTheme.colors.dataSynchronizationView.targetWarning,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse
        )
    ).value