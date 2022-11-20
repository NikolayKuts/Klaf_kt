package com.example.klaf.presentation.deckRepetitionInfo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.klaf.R
import com.example.klaf.data.common.currentDurationAsTimeOrUnassigned
import com.example.klaf.domain.common.asFormattedDate
import com.example.klaf.domain.entities.Deck
import com.example.klaf.presentation.common.timeAsString
import com.example.klaf.presentation.deckRepetition.BaseDeckRepetitionViewModel
import com.example.klaf.presentation.theme.MainTheme
import kotlin.math.max

@Composable
fun DeckRepetitionInfoView(viewModel: BaseDeckRepetitionViewModel) {
    val deckRepetitionInfo by viewModel.deckRepetitionInfo.collectAsState(initial = null)
    val deck = viewModel.deck.collectAsState(initial = null).value ?: return

    deckRepetitionInfo?.let { info ->
        Card(
            shape = RoundedCornerShape(24.dp),
            elevation = 4.dp,
        ) {

            Column(
                modifier = Modifier
                    .defaultMinSize(minWidth = 300.dp)
                    .padding(24.dp),
            ) {
                InfoHeader(deck = deck)
                Spacer(modifier = Modifier.height(16.dp))

                DualInfoItem(
                    title = stringResource(R.string.pointer_iteration_duration),
                    current = info.currentDurationAsTimeOrUnassigned,
                    previous = info.previousDuration.timeAsString
                )
                InfoItemDivider()

                DualInfoItem(
                    title = stringResource(R.string.pointer_scheduled_date),
                    current = info.scheduledDate.asFormattedDate(),
                    previous = info.previousScheduledDate.asFormattedDate(),
                )
                InfoItemDivider()

                DualInfoItem(
                    title = stringResource(R.string.pointer_last_iteration_success_mark),
                    current = stringResource(id = info.currentIterationSuccessMark.markResId),
                    previous = stringResource(id = info.previousIterationSuccessMark.markResId),
                )
                InfoItemDivider()

                FlowableInfoItem(
                    textPointer = stringResource(R.string.pointer_last_iteration_date),
                    infoValue = info.lastIterationDate?.asFormattedDate()
                        ?: stringResource(id = R.string.unassigned_string_value),
                )
                InfoItemDivider()

                FlowableInfoItem(
                    textPointer = stringResource(R.string.pointer_repetition_quantity),
                    infoValue = info.repetitionQuantity.toString(),
                )
            }
        }
    }
}

@Composable
fun InfoHeader(deck: Deck) {
    Row(modifier = Modifier
        .fillMaxWidth(), verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = stringResource(id = R.string.pointer_deck) + ":",
        )
        Text(
            modifier = Modifier.weight(1F),
            text = deck.name,
            textAlign = TextAlign.End,
            style = MainTheme.typographies.deckRepetitionInfoScreenTextStyles.deckName
        )
    }
}

@Composable
private fun DualInfoItem(
    title: String,
    current: String,
    previous: String,
) {
    Column {
        Column {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
                    .clip(shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                    .background(
                        color = MainTheme.colors.deckRepetitionInfoScreenColors.pointerBackground
                    )
                    .padding(start = 4.dp),
                text = title,
                style = MainTheme.typographies.deckRepetitionInfoScreenTextStyles.pointer
            )
            Row {
                Text(text = stringResource(id = R.string.pointer_current))
                Text(
                    modifier = Modifier.weight(1F),
                    text = current,
                    textAlign = TextAlign.End
                )
            }
            Row {
                Text(text = stringResource(R.string.pointer_previous))
                Text(
                    modifier = Modifier.weight(1F),
                    text = previous,
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
fun InfoFlowLayout(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(
        content = content,
        modifier = modifier,
        measurePolicy = infoItemLayoutMeasurePolicy(),
    )
}

fun infoItemLayoutMeasurePolicy(): MeasurePolicy = MeasurePolicy { measurables, constraints ->
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
        color = MainTheme.colors.deckRepetitionInfoScreenColors.itemDivider,
    )
    Spacer(modifier = Modifier.height(16.dp))
}