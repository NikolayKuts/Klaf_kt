package com.example.klaf.presentation.deckList.dataSynchronization

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.klaf.R
import com.example.klaf.data.common.DataSynchronizationState.*
import com.example.klaf.domain.common.toFloatPercents
import com.example.klaf.presentation.common.DIALOG_BUTTON_SIZE
import com.example.klaf.presentation.common.RoundButton
import com.example.klaf.presentation.deckList.common.DeckListViewModel
import com.example.klaf.presentation.theme.MainTheme

@Composable
fun DataSynchronizationDialogView(
    viewModel: DeckListViewModel,
    onClose: () -> Unit,
) {
    val synchronizationState by viewModel.dataSynchronizationState.collectAsState()

    Box {
        when (synchronizationState) {
            UncertainState -> {}
            InitialState -> {
                InitialStateView(onClose = onClose, onConfirm = { viewModel.synchronizeData() })
            }
            is SynchronizingState -> {
                SynchronizationStateView(
                    progress = (synchronizationState as? SynchronizingState)?.progress
                        ?.toFloatPercents()
                        ?: 0.0F
                )
            }
            FinishedState -> {
                FinishStateView(onClose = onClose)
            }
        }
    }
    DisposableEffect(
        key1 = null, effect = {
            onDispose { viewModel.resetSynchronizationState() }
        }
    )
}

@Composable
private fun BoxScope.InitialStateView(
    onClose: () -> Unit,
    onConfirm: () -> Unit,
) {
    Card(
        modifier = Modifier
            .defaultMinSize(minHeight = 150.dp, minWidth = 300.dp)
            .padding(
                top = (DIALOG_BUTTON_SIZE / 2).dp,
                bottom = (DIALOG_BUTTON_SIZE / 2).dp
            )
    ) {
        Box(
            modifier = Modifier
                .padding(MainTheme.dimensions.dialogContentPadding)
                .padding(top = 8.dp)
                .padding(bottom = 16.dp)
        ) {
            Text(
                style = MainTheme.typographies.dialogTextStyle,
                text = stringResource(R.string.data_synchronization_dialog_title)
            )
        }
    }
    SynchronizationLabel()
    Row(
        modifier = Modifier
            .fillMaxWidth(0.5F)
            .align(alignment = Alignment.BottomCenter),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        RoundButton(
            background = MainTheme.colors.positiveDialogButton,
            iconId = R.drawable.ic_comfirmation_24,
            onClick = onConfirm
        )

        RoundButton(
            background = MainTheme.colors.neutralDialogButton,
            iconId = R.drawable.ic_close_24,
            onClick = onClose
        )
    }
}

@Composable
private fun BoxScope.SynchronizationStateView(progress: Float) {
    Card(
        modifier = Modifier
            .defaultMinSize(minHeight = 150.dp, minWidth = 300.dp)
            .padding(
                top = (DIALOG_BUTTON_SIZE / 2).dp,
                bottom = (DIALOG_BUTTON_SIZE / 2).dp
            )
    ) {
        Box(
            modifier = Modifier
                .padding(MainTheme.dimensions.dialogContentPadding)
                .padding(top = 8.dp)
                .padding(bottom = 16.dp)
        ) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                progress = progress
            )
        }
    }
    SynchronizationLabel()
}

@Composable
private fun BoxScope.FinishStateView(onClose: () -> Unit) {
    Card(
        modifier = Modifier
            .defaultMinSize(minHeight = 150.dp, minWidth = 300.dp)
            .padding(
                top = (DIALOG_BUTTON_SIZE / 2).dp,
                bottom = (DIALOG_BUTTON_SIZE / 2).dp
            )
    ) {
        Box(
            modifier = Modifier
                .padding(MainTheme.dimensions.dialogContentPadding)
                .padding(top = 8.dp)
                .padding(bottom = 16.dp)
        ) {
            Text(text = "finish")
        }
    }
    SynchronizationLabel()

    RoundButton(
        modifier = Modifier.align(alignment = Alignment.BottomCenter),
        background = MainTheme.colors.neutralDialogButton,
        iconId = R.drawable.ic_close_24,
        onClick = onClose
    )
}

@Composable
private fun BoxScope.SynchronizationLabel() {
    Card(
        modifier = Modifier
            .size(DIALOG_BUTTON_SIZE.dp)
            .align(alignment = Alignment.TopCenter),
        shape = RoundedCornerShape(DIALOG_BUTTON_SIZE.dp),
        elevation = 0.dp,
    ) {
        Icon(
            modifier = Modifier
                .size(20.dp)
                .background(MainTheme.colors.dataSynchronizationLabelBackground)
                .padding(8.dp),
            painter = painterResource(id = R.drawable.ic_sync_24),
            contentDescription = null,
        )
    }
}