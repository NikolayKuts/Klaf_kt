package com.kuts.klaf.presentation.deckManagment

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.kuts.klaf.data.common.asString
import com.kuts.klaf.presentation.common.ClosingButton
import com.kuts.klaf.presentation.common.ConfirmationButton
import com.kuts.klaf.presentation.common.ContentHolder
import com.kuts.klaf.presentation.common.DIALOG_APP_LABEL_SIZE
import com.kuts.klaf.presentation.common.DialogAppLabel
import com.kuts.klaf.presentation.common.FullBackgroundDialog
import com.kuts.klaf.presentation.common.ScrollableBox
import com.kuts.klaf.presentation.theme.MainTheme

@Composable
fun DeckManagementScreen(
    deckManagementState: DeckManagementState,
    sendAction: (DeckManagementAction) -> Unit,
) {
    with(deckManagementState) {
        ScrollableBox {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp)
            ) {
                StateItem(pair = name)
                StateItem(pair = creationDate)
                StateItem(
                    pair = StatePair(
                        pointer = scheduledDateInterval.pointer,
                        value = scheduledDateInterval.value.asString(LocalContext.current)
                    ),
                    onLongClick = {
                        sendAction(DeckManagementAction.ScheduledDateIntervalChangeRequested)
                    }
                )
                StateItem(pair = repetitionQuantity)
                StateItem(pair = cardQuantity)
                StateItem(pair = lastFirstRepetitionDuration)
                StateItem(pair = lastSecondRepetitionDuration)
                StateItem(pair = lastRepetitionIterationDuration)
                StateItem(pair = isLastIterationSucceeded)
            }

            val scheduledDateIntervalChangeState =
                deckManagementState.scheduledDateIntervalChangeState
            if (scheduledDateIntervalChangeState is ScheduledDataIntervalChangeState.Required) {

                Dialog(
                    onDismissRequest = { sendAction(DeckManagementAction.DismissScheduledDateIntervalDialog) }
                ) {
                    FullBackgroundDialog(
                        onBackgroundClick = {
                            sendAction(DeckManagementAction.DismissScheduledDateIntervalDialog)
                        },
                        topContent = ContentHolder(size = DIALOG_APP_LABEL_SIZE.dp) { DialogAppLabel() },
                        mainContent = {
                            Column {
                                DialogTitle()
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "Current interval: ")
                                    Text(
                                        text = scheduledDateInterval.value.asString(LocalContext.current)
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                DialogContent(
                                    dateData = scheduledDateIntervalChangeState.dateData,
                                    onDateDataChange = { dateUnit, action ->
                                        sendAction(
                                            DeckManagementAction.ScheduledDateIntervalChanged(
                                                dateUnit = dateUnit,
                                                buttonAction = action,
                                            )
                                        )
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        },
                        bottomContent = {
                            ConfirmationButton(
                                onClick = {
                                    sendAction(DeckManagementAction.ScheduledDateIntervalChangeConfirmed)
                                }
                            )
                            ClosingButton(
                                onClick = {
                                    sendAction(DeckManagementAction.DismissScheduledDateIntervalDialog)
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DialogContent(
    dateData: DateData,
    onDateDataChange: (dateUnit: DateUnit, action: DraggableButtonAction) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        dateData.toList().onEach { dateUnit ->
            val updatedDateUnit by rememberUpdatedState(dateUnit)

            DateItem(
                dateUnit = dateUnit,
                datePointer = stringResource(id = dateUnit.nameRes),
                onDragButtonAction = { action ->
                    onDateDataChange(updatedDateUnit, action)
                }
            )
        }
    }
}

@Composable
private fun DateItem(
    dateUnit: DateUnit,
    datePointer: String,
    onDragButtonAction: (action: DraggableButtonAction) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        DragButton(
            value = dateUnit.value.toString(),
            onDragButtonAction = { action -> onDragButtonAction(action) },
            modifier = Modifier
                .width(100.dp)
                .height(40.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))
        Text(text = datePointer)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StateItem(pair: StatePair<*>, onLongClick: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .combinedClickable(
                onClick = { },
                onLongClick = { onLongClick() },
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "${pair.pointer}: ")
            Text(text = pair.value.toString())
        }

        Divider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
        )
    }
}

@Composable
private fun DialogTitle() {
    Text(
        style = MainTheme.typographies.dialogTextStyle,
        text = buildAnnotatedString {
            withStyle(style = SpanStyle()) {
                append(text = "Change the scheduled date interval")
            }
        }
    )
}