package com.example.klaf.presentation.deckRepetitionInfo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.domain.common.DeckRepetitionSuccessMark
import com.example.domain.common.DeckRepetitionSuccessMark.*
import com.example.domain.common.Emptiable
import com.example.domain.common.ifNull
import com.example.domain.common.otherwise
import com.example.domain.entities.DeckRepetitionInfo
import com.example.klaf.R
import com.example.klaf.data.common.calculateDetailedPreviousScheduledRange
import com.example.klaf.data.common.calculateDetailedScheduledRange
import com.example.klaf.data.common.currentDurationAsTimeOrUnassigned
import com.example.klaf.data.common.markResId
import com.example.klaf.presentation.common.*
import com.example.klaf.presentation.theme.MainTheme
import kotlin.math.max

@Composable
fun DeckRepetitionInfoView(
    viewModel: DeckRepetitionInfoViewModel,
    deckName: String,
    onCloseClick: () -> Unit,
) {
    val deckRepetitionInfo by viewModel.repetitionInfo.collectAsState()

    when (val infoContent = deckRepetitionInfo) {
        is Emptiable.Empty -> {}
        is Emptiable.Content -> {
            ScrollableBox(
                modifier = Modifier.noRippleClickable { onCloseClick() },
            ) {
                FullBackgroundDialog(
                    onBackgroundClick = onCloseClick,
                    mainContent = {
                        infoContent.data.ifNull {
                            Text(
                                text = stringResource(id = R.string.deck_repetition_info_dialog_no_info),
                                textAlign = TextAlign.Center
                            )
                        } otherwise { info ->
                            RepetitionInfo(deckName = deckName, info = info)
                        }
                    },
                    bottomContent = { ClosingButton(onClick = onCloseClick) },
                )
            }
        }
    }
}

@Composable
private fun RepetitionInfo(
    deckName: String,
    info: DeckRepetitionInfo,
) {
    val context = LocalContext.current

    Column(modifier = Modifier.defaultMinSize(minWidth = 300.dp)) {
        InfoHeader(deckName = deckName)
        Spacer(modifier = Modifier.height(16.dp))

        DualInfoItem(
            title = stringResource(R.string.pointer_iteration_duration),
            currentValue = info.currentDurationAsTimeOrUnassigned,
            previousValue = info.previousDuration.timeAsString,
            currentMark = info.currentIterationSuccessMark,
        )
        InfoItemDivider()

        ScheduledDateItem(
            title = stringResource(R.string.pointer_scheduled_repetition),
            nextValue = info.calculateDetailedScheduledRange(context = context),
            previousValue = info.calculateDetailedPreviousScheduledRange(
                context = context
            ),
        )
        InfoItemDivider()

        DualInfoItem(
            title = stringResource(R.string.pointer_iteration_success_mark),
            currentValue = stringResource(
                id = info.currentIterationSuccessMark.markResId
            ),
            previousValue = stringResource(
                id = info.previousIterationSuccessMark.markResId
            ),
            currentMark = info.currentIterationSuccessMark,
        )
        InfoItemDivider()

        FlowableInfoItem(
            textPointer = stringResource(R.string.pointer_repetition_quantity),
            infoValue = info.repetitionQuantity.toString(),
        )
    }
}

@Composable
private fun InfoHeader(deckName: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth(), verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = stringResource(id = R.string.pointer_deck) + ":",
        )
        Text(
            modifier = Modifier.weight(1F),
            text = deckName,
            textAlign = TextAlign.End,
            style = MainTheme.typographies.deckRepetitionInfoScreenTextStyles.deckName
        )
    }
}

@Composable
private fun ScheduledDateItem(
    title: String,
    nextValue: String,
    previousValue: String,
) {
    DualInfoItemWithValueBackground(
        title = title,
        firstPointer = stringResource(id = R.string.pointer_next),
        firstValue = nextValue,
        secondPointer = stringResource(id = R.string.pointer_previous),
        secondValue = previousValue,
        valueBackground = Color.Transparent
    )
}

@Composable
private fun DualInfoItem(
    title: String,
    currentValue: String,
    previousValue: String,
    currentMark: DeckRepetitionSuccessMark,
) {
    DualInfoItemWithValueBackground(
        title = title,
        firstPointer = stringResource(id = R.string.pointer_current),
        firstValue = currentValue,
        secondPointer = stringResource(id = R.string.pointer_previous),
        secondValue = previousValue,
        valueBackground = getValueBackgroundColorBySuccessMark(mark = currentMark)
    )
}

@Composable
private fun DualInfoItemWithValueBackground(
    title: String,
    firstPointer: String,
    firstValue: String,
    secondPointer: String,
    secondValue: String,
    valueBackground: Color,
) {
    Column {
        Column {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
                    .clip(shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                    .background(
                        color = MainTheme.colors.deckRepetitionInfoScreen.pointerBackground
                    )
                    .padding(start = 4.dp),
                text = title,
                style = MainTheme.typographies.deckRepetitionInfoScreenTextStyles.pointer
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    modifier = Modifier.weight(1F),
                    text = firstPointer
                )
                Text(
                    modifier = Modifier
                        .clip(shape = RoundedCornerShape(4.dp))
                        .background(color = valueBackground)
                        .valuePadding(),
                    text = firstValue,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    modifier = Modifier.weight(1F),
                    text = secondPointer
                )
                Text(
                    modifier = Modifier.valuePadding(),
                    text = secondValue,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Composable
private fun FlowableInfoItem(textPointer: String, infoValue: String) {
    InfoFlowLayout {
        Text(
            modifier = Modifier.padding(end = 8.dp),
            text = "$textPointer:",
        )
        Text(text = infoValue)
    }
}

@Composable
private fun InfoFlowLayout(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(
        content = content,
        modifier = modifier,
        measurePolicy = infoItemLayoutMeasurePolicy(),
    )
}

private fun infoItemLayoutMeasurePolicy(): MeasurePolicy =
    MeasurePolicy { measurables, constraints ->
        val placeables = measurables.map { measurable ->
            measurable.measure(constraints)
        }
        val firstElement = placeables.first()
        val secondElement = placeables[1]
        val potentialMaxContentWidth = placeables.sumOf { it.measuredWidth }
        val totalContentHeight = if (potentialMaxContentWidth < constraints.maxWidth) {
            max(firstElement.height, secondElement.height)
        } else {
            firstElement.height + secondElement.height
        }

        layout(
            width = constraints.maxWidth,
            height = totalContentHeight
        ) {
            val startPosition = 0
            val xPosition: Int
            val yPosition: Int

            if (potentialMaxContentWidth > constraints.maxWidth) {
                xPosition = constraints.maxWidth - secondElement.width
                yPosition = firstElement.height
            } else {
                xPosition = constraints.maxWidth - secondElement.width
                yPosition = startPosition
            }

            firstElement.placeRelative(x = startPosition, y = startPosition)
            secondElement.placeRelative(x = xPosition, y = yPosition)
        }
    }

@Composable
private fun InfoItemDivider() {
    Spacer(modifier = Modifier.height(8.dp))
    Divider(
        modifier = Modifier.height(1.dp),
        color = MainTheme.colors.deckRepetitionInfoScreen.itemDivider,
    )
    Spacer(modifier = Modifier.height(16.dp))
}

fun Modifier.valuePadding(): Modifier {
    return this.padding(start = 4.dp, top = 2.dp, end = 4.dp, bottom = 2.dp)
}

@Composable
private fun getValueBackgroundColorBySuccessMark(mark: DeckRepetitionSuccessMark): Color {
    return when (mark) {
        SUCCESS -> MainTheme.colors.deckRepetitionInfoScreen.successMark
        FAILURE -> MainTheme.colors.deckRepetitionInfoScreen.failureMark
        UNASSIGNED -> Color.Transparent
    }
}