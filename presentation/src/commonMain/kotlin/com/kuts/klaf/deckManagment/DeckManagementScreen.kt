package com.kuts.klaf.deckManagment

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
import androidx.compose.material3.Divider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.kuts.domain.common.DateData
import com.kuts.domain.common.DateUnit
import com.kuts.klaf.common.BaseMainViewModel
import com.kuts.klaf.common.ClosingButton
import com.kuts.klaf.common.ConfirmationButton
import com.kuts.klaf.common.ContentHolder
import com.kuts.klaf.common.DateFormatPattern
import com.kuts.klaf.common.DIALOG_APP_LABEL_SIZE
import com.kuts.klaf.common.DialogAppLabel
import com.kuts.klaf.common.FullBackgroundDialog
import com.kuts.klaf.common.ScrollableBox
import com.kuts.klaf.common.asFormattedDate
import com.kuts.klaf.common.asString
import com.kuts.klaf.common.toLabelRes
import com.kuts.klaf.navigation.CollectFlowWithLifecycle
import com.kuts.klaf.theme.MainTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun DeckManagementScreen(
    sharedViewModel: BaseMainViewModel,
    deckId: Int,
) {
    val viewModel: BaseDeckManagementViewModel = koinViewModel(parameters = { parametersOf(deckId) })

    CollectFlowWithLifecycle(flow = viewModel.eventMessage, onEach = sharedViewModel::notify)

    Surface {
        DeckManagementContent(
            deckManagementState = viewModel.deckManagementState.collectAsState().value,
            sendAction = viewModel::sendAction,
        )
    }
}

@Composable
private fun DeckManagementContent(
    deckManagementState: DeckManagementState,
    sendAction: (IDeckManagementAction) -> Unit,
) {
    with(deckManagementState) {
        ScrollableBox {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp)
            ) {
                StateItem(pair = name)
                StateItem(
                    pair = StatePair(
                        pointer = creationDate.pointer,
                        value = creationDate.value.asFormattedDate(pattern = DateFormatPattern.FULL),
                    )
                )
                StateItem(
                    pair = StatePair(
                        pointer = scheduledDateInterval.pointer,
                        value = scheduledDateInterval.value.asString()
                    ),
                    onLongClick = {
                        sendAction(IDeckManagementAction.ScheduledDateIntervalChangeRequested)
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
            if (scheduledDateIntervalChangeState is IScheduledDataIntervalChangeState.Required) {

                Dialog(
                    onDismissRequest = { sendAction(IDeckManagementAction.DismissScheduledDateIntervalDialog) }
                ) {
                    FullBackgroundDialog(
                        onBackgroundClick = {
                            sendAction(IDeckManagementAction.DismissScheduledDateIntervalDialog)
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
                                        text = scheduledDateInterval.value.asString()
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                DialogContent(
                                    dateData = scheduledDateIntervalChangeState.dateData,
                                    onDateDataChange = { dateUnit, action ->
                                        sendAction(
                                            IDeckManagementAction.ScheduledDateIntervalChanged(
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
                                    sendAction(IDeckManagementAction.ScheduledDateIntervalChangeConfirmed)
                                }
                            )
                            ClosingButton(
                                onClick = {
                                    sendAction(IDeckManagementAction.DismissScheduledDateIntervalDialog)
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
    onDateDataChange: (dateUnit: DateUnit, action: IDraggableButtonAction) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        dateData.toList().onEach { dateUnit ->
            val updatedDateUnit by rememberUpdatedState(dateUnit)

            DateItem(
                dateUnit = dateUnit,
                datePointer = stringResource(resource = dateUnit.toLabelRes()),
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
    onDragButtonAction: (action: IDraggableButtonAction) -> Unit,
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
