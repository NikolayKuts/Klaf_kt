package com.kuts.klaf.presentation.deckManagment

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.kuts.klaf.R
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
//            StateItem(pair = repetitionIterationDates)
//            StateItem(pair = scheduledIterationDates)
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
                val intervalDateDate = scheduledDateIntervalChangeState.dateData

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
//                                        modifier = Modifier.background(Color(0x4900FF00)),
                                        text = scheduledDateInterval.value.asString(LocalContext.current)
//                                        text = "30min"
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                DialogContent(
                                    dateData = intervalDateDate,
                                    onDateDataChange = { dateUnit, value ->
                                        sendAction(
                                            DeckManagementAction.ScheduledDateIntervalChanged(
                                                dateUnit = dateUnit,
                                                value = value,
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
//    onDateDataChange: (DateData) -> Unit
    onDateDataChange: (dateUnit: DateUnit, value: String) -> Unit
) {
    with(dateData) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DateItem(
//                dateData = dateData,
                dateUnit = year,
                datePointer = stringResource(R.string.year),
                onDateUnitChange = onDateDataChange
            )
            DateItem(
//                dateData = dateData,
                dateUnit = month,
                datePointer = stringResource(R.string.month),
                onDateUnitChange = onDateDataChange
            )
            DateItem(
//                dateData = dateData,
                dateUnit = week,
                datePointer = stringResource(R.string.week),
                onDateUnitChange = onDateDataChange
            )
            DateItem(
//                dateData = dateData,
                dateUnit = day,
                datePointer = stringResource(R.string.day),
                onDateUnitChange = onDateDataChange,
            )
            DateItem(
//                dateData = dateData,
                dateUnit = hour,
                datePointer = stringResource(R.string.hour),
                onDateUnitChange = onDateDataChange,
            )
            DateItem(
//                dateData = dateData,
                dateUnit = minute,
                datePointer = stringResource(R.string.minute),
                onDateUnitChange = onDateDataChange
            )
        }
    }
}

@Composable
private fun DateItem(
//    dateData: DateData,
    dateUnit: DateUnit,
    datePointer: String,
//    onDateUnitChange: (DateData) -> Unit,
    onDateUnitChange: (dateUnit: DateUnit, value: String) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        val density = LocalDensity.current
        val cellShape = RoundedCornerShape(size = 6.dp)
        val value = if (dateUnit.isEmpty()) "" else dateUnit.value.toString()

        BasicTextField(
            modifier = Modifier
//                .widthIn(min = 30.dp, max = density.run { ipaValueWidthPx.toDp() })
                .widthIn(min = 30.dp, max = 50.dp)
                .width(IntrinsicSize.Min)
                .clip(shape = cellShape)
                .background(MainTheme.colors.cardManagementView.ipaCellBackground)
                .padding(6.dp),
//            value = dateUnit.value.toString(),
            value = value,
            cursorBrush = Brush.verticalGradient(
                0.00f to Color.Transparent,
                0.15f to Color.Transparent,
                0.15f to MainTheme.colors.material.onPrimary,
                0.90f to MainTheme.colors.material.onPrimary,
                0.90f to Color.Transparent,
                1.00f to Color.Transparent,
            ),
            onValueChange = { valueAsString: String ->
                onDateUnitChange(dateUnit, valueAsString)

//                val valueAsLong = valueAsString.toLongOrNull()
//
//                valueAsLong?.let {
//                    val updatedDateData = dateData.copyByUnit(unit = dateUnit, value = it)
//                    onDateUnitChange(updatedDateData)
//                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = MainTheme.typographies.cardManagementViewTextStyles.ipaValue,
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
//            Text(text = ": ")
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
//                append(text = stringResource(id = R.string.deck_navigation_dialog_item_rename_deck))
                append(text = "Change the scheduled date interval")
            }
//                withStyle(style = MainTheme.typographies.accentedDialogText) {
//                    append(" \"${deckName}\"")
//                }
        }
    )
}