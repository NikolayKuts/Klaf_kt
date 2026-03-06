package com.kuts.klaf.deckRepetitionInfo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.kuts.domain.common.*
import com.kuts.domain.common.DeckReviewPassSuccessMark.*
import com.kuts.domain.entities.DeckRepetitionInfo
import com.kuts.klaf.common.*
import com.kuts.klaf.common.BaseMainViewModel
import com.kuts.klaf.common.EventMessage
import com.kuts.klaf.navigation.CollectFlowWithLifecycle
import com.kuts.klaf.presentation.resources.*
import com.kuts.klaf.theme.MainTheme
import kotlin.math.max
import org.jetbrains.compose.resources.stringResource
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun DeckRepetitionInfoDialog(
    navController: NavHostController,
    sharedViewModel: BaseMainViewModel,
    deckId: Int,
    deckName: String,
    repetitionInfoEvent: RepetitionInfoEvent,
) {
    val viewModel: DeckRepetitionInfoViewModel = koinViewModel(
        parameters = { parametersOf(deckId) }
    )

    CollectFlowWithLifecycle(flow = viewModel.eventMessage) { eventMessage ->
        sharedViewModel.notify(message = eventMessage)
        navController.popBackStack()
    }

    var isRepetitionInfoEventHandled by remember(repetitionInfoEvent) {
        mutableStateOf(value = false)
    }

    DeckRepetitionInfoDialogContent(
        viewModel = viewModel,
        deckName = deckName,
        onCloseClick = { navController.popBackStack() },
        eventMessage = sharedViewModel.eventMessage.collectAsState(initial = null).value,
        onRendered = {
            if (isRepetitionInfoEventHandled) {
                return@DeckRepetitionInfoDialogContent
            }

            val eventMessage = when (repetitionInfoEvent) {
                RepetitionInfoEvent.ScheduledSuccessfully -> {
                    EventMessage(
                        resId = Res.string.deck_repetition_scheduled_successfully,
                        type = EventMessage.Type.Positive,
                    )
                }

                RepetitionInfoEvent.SchedulingFailed -> {
                    EventMessage(
                        resId = Res.string.deck_repetition_scheduling_failed,
                        type = EventMessage.Type.Negative,
                    )
                }

                RepetitionInfoEvent.OneRepetitionToFinish -> {
                    EventMessage(resId = Res.string.deck_repetition_one_repetition_to_finish_iteration)
                }

                RepetitionInfoEvent.Non -> null
            }

            eventMessage?.let { sharedViewModel.notify(message = it) }
            isRepetitionInfoEventHandled = true
        },
    )
}

@Composable
private fun DeckRepetitionInfoDialogContent(
    viewModel: DeckRepetitionInfoViewModel,
    deckName: String,
    onCloseClick: () -> Unit,
    eventMessage: EventMessage?,
    onRendered: () -> Unit,
) {
    val deckRepetitionInfo by viewModel.repetitionInfo.collectAsState()

    when (val infoContent = deckRepetitionInfo) {
        is IEmptiable.Empty -> {}
        is IEmptiable.Content -> {
            ScrollableBox(
                modifier = Modifier.noRippleClickable { onCloseClick() },
                dialogMode = true,
                eventContent = {
                    eventMessage.ifNotNull { EventMessageView(message = it) }
                },
            ) {
                FullBackgroundDialog(
                    onBackgroundClick = onCloseClick,
                    topContent = ContentHolder(size = DIALOG_APP_LABEL_SIZE.dp) { DialogAppLabel() },
                    mainContent = {
                        infoContent.data.ifNull {
                            Text(
                                text = stringResource(resource = Res.string.deck_repetition_info_dialog_no_info),
                                textAlign = TextAlign.Center
                            )
                        } otherwise { info ->
                            RepetitionInfo(deckName = deckName, info = info)
                        }
                    },
                    bottomContent = { ClosingButton(onClick = onCloseClick) },
                )

                LaunchedEffect(key1 = null) { onRendered() }
            }
        }
    }
}

@Composable
private fun RepetitionInfo(
    deckName: String,
    info: DeckRepetitionInfo,
) {
    Column(modifier = Modifier.defaultMinSize(minWidth = 300.dp)) {
        InfoHeader(deckName = deckName)
        Spacer(modifier = Modifier.height(16.dp))

        DualInfoItem(
            title = stringResource(resource = Res.string.pointer_iteration_duration),
            currentValue = info.currentDurationAsTimeOrUnassigned,
            previousValue = info.previousDuration.timeAsString,
            currentMark = info.currentIterationSuccessMark,
        )
        InfoItemDivider()

        ScheduledDateItem(
            title = stringResource(resource = Res.string.pointer_scheduled_repetition),
            nextValue = info.calculateDetailedScheduledRange(),
            previousValue = info.calculateDetailedPreviousScheduledRange(),
        )
        InfoItemDivider()

        DualInfoItem(
            title = stringResource(resource = Res.string.pointer_iteration_success_mark),
            currentValue = stringResource(
                resource = info.currentIterationSuccessMark.markResId
            ),
            previousValue = stringResource(
                resource = info.previousIterationSuccessMark.markResId
            ),
            currentMark = info.currentIterationSuccessMark,
        )
        InfoItemDivider()

        FlowableInfoItem(
            textPointer = stringResource(resource = Res.string.pointer_repetition_quantity),
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
            text = stringResource(resource = Res.string.pointer_deck) + ":",
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
        firstPointer = stringResource(resource = Res.string.pointer_next),
        firstValue = nextValue,
        secondPointer = stringResource(resource = Res.string.pointer_previous),
        secondValue = previousValue,
        valueBackground = Color.Transparent
    )
}

@Composable
private fun DualInfoItem(
    title: String,
    currentValue: String,
    previousValue: String,
    currentMark: DeckReviewPassSuccessMark,
) {
    DualInfoItemWithValueBackground(
        title = title,
        firstPointer = stringResource(resource = Res.string.pointer_current),
        firstValue = currentValue,
        secondPointer = stringResource(resource = Res.string.pointer_previous),
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
private fun getValueBackgroundColorBySuccessMark(mark: DeckReviewPassSuccessMark): Color {
    return when (mark) {
        SUCCESS -> MainTheme.colors.deckRepetitionInfoScreen.successMark
        FAILURE -> MainTheme.colors.deckRepetitionInfoScreen.failureMark
        UNASSIGNED -> Color.Transparent
    }
}
