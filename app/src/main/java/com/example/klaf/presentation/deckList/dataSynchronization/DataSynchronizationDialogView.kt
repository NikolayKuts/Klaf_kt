package com.example.klaf.presentation.deckList.dataSynchronization

import android.annotation.SuppressLint
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.klaf.R
import com.example.klaf.data.common.DataSynchronizationState.*
import com.example.klaf.presentation.common.DIALOG_BUTTON_SIZE
import com.example.klaf.presentation.common.FullBackgroundDialog
import com.example.klaf.presentation.common.RoundButton
import com.example.klaf.presentation.common.noRippleClickable
import com.example.klaf.presentation.deckList.common.BaseDeckListViewModel
import com.example.klaf.presentation.theme.MainTheme

@Composable
fun DataSynchronizationDialogView(
    viewModel: BaseDeckListViewModel,
    onCloseClick: () -> Unit,
) {
    val synchronizationState by viewModel.dataSynchronizationState.collectAsState()

    when (val state = synchronizationState) {
        UncertainState -> {}
        InitialState -> {
            InitialStateView(
                onCloseClick = onCloseClick,
                onConfirmClick = { viewModel.synchronizeData() }
            )
        }
        is SynchronizingState -> {
            SynchronizationStateView(synchronizationData = state.synchronizationData)
        }
        FinishedState -> {
            FinishStateView(onCloseClick = onCloseClick)
        }
    }

    DisposableEffect(
        key1 = null,
        effect = {
            onDispose { viewModel.resetSynchronizationState() }
        }
    )
}

@Composable
private fun InitialStateView(
    onCloseClick: () -> Unit,
    onConfirmClick: () -> Unit,
) {
    FullBackgroundDialog(
        onBackgroundClick = onCloseClick,
        topContent = { SynchronizationLabel() },
        mainContent = {
            Text(
                style = MainTheme.typographies.dialogTextStyle,
                text = stringResource(R.string.data_synchronization_dialog_title)
            )
        },
        bottomContent = {
            RoundButton(
                background = MainTheme.colors.positiveDialogButton,
                iconId = R.drawable.ic_confirmation_24,
                onClick = onConfirmClick
            )

            RoundButton(
                background = MainTheme.colors.neutralDialogButton,
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
        topContent = { AnimatedSynchronizationLabel() },
        mainContent = {
            Column {
                Text(
                    style = MainTheme.typographies.dialogTextStyle,
                    text = stringResource(R.string.data_synchronization_dialog_sync_state_text)
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (synchronizationData.isNotEmpty()) {
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
        topContent = { SynchronizationLabel() },
        mainContent = {
            Text(
                style = MainTheme.typographies.dialogTextStyle,
                text = stringResource(R.string.data_synchronization_dialog_finish_state_text)
            )
        },
        bottomContent = {
            RoundButton(
                background = MainTheme.colors.neutralDialogButton,
                iconId = R.drawable.ic_close_24,
                onClick = onCloseClick
            )
        }
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
        initialValue = MainTheme.colors.dataSynchronizationViewColors.labelBackground,
        targetValue = MainTheme.colors.dataSynchronizationViewColors.labelBackgroundSecond,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = animationDuration,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        )
    )

    SynchronizationLabel(
        modifier = Modifier
            .rotate(degrees = rotation)
            .background(color = color)
            .size(20.dp)
            .padding(8.dp),
    )
}

@SuppressLint("ModifierParameter")
@Composable
private fun SynchronizationLabel(
    modifier: Modifier = Modifier
        .size(20.dp)
        .background(MainTheme.colors.dataSynchronizationViewColors.labelBackground)
        .padding(8.dp),
) {
    Card(
        modifier = Modifier
            .size(DIALOG_BUTTON_SIZE.dp)
            .noRippleClickable { },
        shape = RoundedCornerShape(DIALOG_BUTTON_SIZE.dp),
        elevation = 0.dp,
    ) {
        Icon(
            modifier = modifier,
            painter = painterResource(id = R.drawable.ic_sync_24),
            contentDescription = null,
        )
    }
}