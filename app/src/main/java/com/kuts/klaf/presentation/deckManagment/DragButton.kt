package com.kuts.klaf.presentation.deckManagment

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Spring.StiffnessLow
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.kuts.domain.common.invertedCoerceIn
import com.kuts.klaf.R
import com.lib.lokdroid.core.logD
import com.lib.lokdroid.core.logE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.sign


private const val ICON_BUTTON_ALPHA_INITIAL = 0.3f
private const val CONTAINER_BACKGROUND_ALPHA_INITIAL = 0.6f
private const val CONTAINER_BACKGROUND_ALPHA_MAX = 0.7f
private const val CONTAINER_OFFSET_FACTOR = 0.1f
private const val DRAG_LIMIT_HORIZONTAL_DP = 72
private const val DRAG_LIMIT_VERTICAL_DP = 64
private const val START_DRAG_THRESHOLD_DP = 2
private const val DRAG_LIMIT_HORIZONTAL_THRESHOLD_FACTOR = 0.9f
private const val DRAG_LIMIT_VERTICAL_THRESHOLD_FACTOR = 0.9f
private const val DRAG_HORIZONTAL_ICON_HIGHLIGHT_LIMIT_DP = 36
private const val DRAG_VERTICAL_ICON_HIGHLIGHT_LIMIT_DP = 60
private const val DRAG_CLEAR_ICON_REVEAL_DP = 2
private const val COUNTER_DELAY_INITIAL_MS = 500L
private const val COUNTER_DELAY_FAST_MS = 150L

@Preview(
    showBackground = true,
//    showSystemUi = true
)
@Composable
private fun Preview() {
    Box(modifier = Modifier.padding(32.dp)) {
        DragButton(
            modifier = Modifier
                .width(80.dp)
                .height(35.dp),
            value = "0",
            onDragButtonAction = { /*TODO*/ },
        )
    }
}

@Composable
fun DragButton(
    value: String,
    onDragButtonAction: (DraggableButtonAction) -> Unit,
    modifier: Modifier = Modifier
) {
    var buttonSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .onSizeChanged { newSize -> buttonSize = newSize }
            .then(
                Modifier
                    .widthIn(min = 100.dp)
                    .heightIn(min = 40.dp)
                    .width(200.dp)
                    .height(80.dp)
            )
    ) {
        val thumbOffsetX = remember { Animatable(0f) }
        val thumbOffsetY = remember { Animatable(0f) }
        val verticalDragButtonRevealPx = DRAG_CLEAR_ICON_REVEAL_DP.dp.dpToPx()

        ButtonContainer(
            containerSize = buttonSize,
            thumbOffsetX = thumbOffsetX.value,
            thumbOffsetY = thumbOffsetY.value,
            onAction = onDragButtonAction,
            clearButtonVisible = thumbOffsetY.value >= verticalDragButtonRevealPx,
            modifier = Modifier
        )

        DraggableButton(
            containerSize = { buttonSize },
            value = value,
            thumbOffsetX = thumbOffsetX,
            thumbOffsetY = thumbOffsetY,
            onAction = onDragButtonAction,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Composable
private fun ButtonContainer(
    containerSize: IntSize,
    thumbOffsetX: Float,
    thumbOffsetY: Float,
    onAction: (DraggableButtonAction) -> Unit,
    modifier: Modifier = Modifier,
    clearButtonVisible: Boolean = false,
) {
    // at which point the icon should be fully visible
    val horizontalHighlightLimitPx = DRAG_HORIZONTAL_ICON_HIGHLIGHT_LIMIT_DP.dp.dpToPx()
    val verticalHighlightLimitPx = DRAG_VERTICAL_ICON_HIGHLIGHT_LIMIT_DP.dp.dpToPx()

    // Определяем размер иконок на основе реального размера контейнера
    val iconSize = with(LocalDensity.current) {
        (containerSize.width * 0.25f).toDp() // Иконки будут 30% от ширины контейнера
    }

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .offset {
                IntOffset(
                    (thumbOffsetX * CONTAINER_OFFSET_FACTOR).toInt(),
                    (thumbOffsetY * CONTAINER_OFFSET_FACTOR).toInt(),
                )
            }
            .fillMaxSize()
            .clip(RoundedCornerShape(64.dp))
            .background(
                Color.Black.copy(
                    alpha = if (thumbOffsetX.absoluteValue > 0.0f) {
                        // horizontal
                        (CONTAINER_BACKGROUND_ALPHA_INITIAL + ((thumbOffsetX.absoluteValue / horizontalHighlightLimitPx) / 20f))
                            .coerceAtMost(CONTAINER_BACKGROUND_ALPHA_MAX)
                    } else if (thumbOffsetY.absoluteValue > 0.0f) {
                        // vertical
                        (CONTAINER_BACKGROUND_ALPHA_INITIAL + ((thumbOffsetY.absoluteValue / verticalHighlightLimitPx) / 10f))
                            .coerceAtMost(CONTAINER_BACKGROUND_ALPHA_MAX)
                    } else {
                        CONTAINER_BACKGROUND_ALPHA_INITIAL
                    }
                )
            )
            .padding(horizontal = 8.dp)
    ) {
        // decrease button
        IconControlButton(
            icon = ImageVector.vectorResource(id = R.drawable.ic_remove),
            contentDescription = "Decrease count",
            onClick = { onAction(DraggableButtonAction.Decrease) },
            enabled = !clearButtonVisible,
            tintColor = Color.White.copy(
                alpha = if (clearButtonVisible) {
                    0.0f
                } else if (thumbOffsetX < 0) {
                    (thumbOffsetX.absoluteValue / horizontalHighlightLimitPx).coerceIn(
                        ICON_BUTTON_ALPHA_INITIAL,
                        1f
                    )
                } else {
                    ICON_BUTTON_ALPHA_INITIAL
                }
            ),
            modifier = Modifier.size(iconSize)
        )

        // clear button
        if (clearButtonVisible) {
            IconControlButton(
                icon = Icons.Outlined.Clear,
                contentDescription = "Clear count",
                onClick = { onAction(DraggableButtonAction.Reset) },
                enabled = false,
                tintColor = Color.White.copy(
                    alpha = (thumbOffsetY.absoluteValue / verticalHighlightLimitPx).coerceIn(
                        ICON_BUTTON_ALPHA_INITIAL,
                        1f
                    )
                ),
                modifier = Modifier.size(iconSize)
            )
        }

        // increase button
        IconControlButton(
            icon = Icons.Outlined.Add,
            contentDescription = "Increase count",
            onClick = { onAction(DraggableButtonAction.Increase) },
            enabled = !clearButtonVisible,
            tintColor = Color.White.copy(
                alpha = if (clearButtonVisible) {
                    0.0f
                } else if (thumbOffsetX > 0) {
                    (thumbOffsetX.absoluteValue / horizontalHighlightLimitPx).coerceIn(
                        ICON_BUTTON_ALPHA_INITIAL,
                        1f
                    )
                } else {
                    ICON_BUTTON_ALPHA_INITIAL
                }
            ),
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
private fun IconControlButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tintColor: Color = Color.White,
    clickTintColor: Color = Color.White,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    IconButton(
        onClick = onClick,
        interactionSource = interactionSource,
        enabled = enabled,
        modifier = modifier
            .size(48.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isPressed) clickTintColor else tintColor,
            modifier = Modifier.size(32.dp)
        )
    }
}

sealed interface DraggableButtonAction {
    data object Increase : DraggableButtonAction
    data object Decrease : DraggableButtonAction
    data object Reset : DraggableButtonAction
}

@Composable
private fun DraggableButton(
    containerSize: () -> IntSize,
    value: String,
    thumbOffsetX: Animatable<Float, AnimationVector1D>,
    thumbOffsetY: Animatable<Float, AnimationVector1D>,
    onAction: (DraggableButtonAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dragLimitHorizontalPx = { containerSize().width / 3F }
    val dragLimitVerticalPx = { containerSize().height / 2F }
    val startDragThreshold = START_DRAG_THRESHOLD_DP.dp.dpToPx()
    val scope = rememberCoroutineScope()

    val dragDirection = remember {
        mutableStateOf(DragDirection.NONE)
    }

    val iconSize = with(LocalDensity.current) {
        (containerSize().width * 0.3f).toDp() // Иконки будут 30% от ширины контейнера
    }

    var buttonReleased by remember {
        mutableStateOf(true)
    }

    val verticalAlpha = if (thumbOffsetY.value > 0 && !buttonReleased) {
        (thumbOffsetY.value.absoluteValue / dragLimitVerticalPx()).invertedCoerceIn(
            min = 0f,
            max = 1f
        )
    } else {
        1F
    }

    var quickChangingValueJob: Job? = null

    Box(
        modifier = modifier
            // change the x and y position of the composable
            .offset { IntOffset(thumbOffsetX.value.toInt(), thumbOffsetY.value.toInt()) }
            .size(iconSize)
            .shadow(8.dp, shape = CircleShape)
            .size(64.dp)
            .clip(CircleShape)
            .clickable {
                // only allow clicks while not dragging
                if (thumbOffsetX.value.absoluteValue <= startDragThreshold &&
                    thumbOffsetY.value.absoluteValue <= startDragThreshold
                ) {
                    onAction(DraggableButtonAction.Increase)
                }
            }
            .alpha(verticalAlpha)
            .background(Color.Gray)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change: PointerInputChange, dragAmount: Offset ->
                        buttonReleased = false

                        scope.launch {
                            if (
                                (dragDirection.value == DragDirection.NONE && dragAmount.x.absoluteValue >= startDragThreshold)
                                || dragDirection.value == DragDirection.HORIZONTAL
                            ) {
                                manageQuickChangingValues(
                                    dragDirection = dragDirection.value,
                                    scope = scope,
                                    thumbOffsetX = thumbOffsetX,
                                    dragLimitHorizontalPx = dragLimitHorizontalPx,
                                    onAction = onAction,
                                    onLaunch = { quickChangingValueJob = it }
                                )

                                manageHorizontalDrag(
                                    dragDirectionState = dragDirection,
                                    thumbOffsetX = thumbOffsetX,
                                    dragAmount = dragAmount,
                                    dragLimitHorizontalPx = dragLimitHorizontalPx
                                )
                            } else if (
                                (dragDirection.value != DragDirection.HORIZONTAL
                                        && dragAmount.y >= startDragThreshold)
                            ) {
                                manageVerticalDrag(
                                    dragDirectionState = dragDirection,
                                    thumbOffsetY = thumbOffsetY,
                                    dragAmount = dragAmount,
                                    dragLimitVerticalPx = dragLimitVerticalPx
                                )
                            }
                        }
                    },
                    onDragEnd = {
                        logE("onDragEnd DETECTED")
                        quickChangingValueJob?.cancel()
                        buttonReleased = true

                        scope.launch {
                            handleButtonRelease(
                                thumbOffsetX = thumbOffsetX,
                                thumbOffsetY = thumbOffsetY,
                                dragLimitHorizontalPx = dragLimitHorizontalPx,
                                dragLimitVerticalPx = dragLimitVerticalPx,
                                onAction = onAction
                            )

                            returnDragButtonIdDeeded(
                                dragDirectionState = dragDirection,
                                thumbOffsetX = thumbOffsetX,
                                thumbOffsetY = thumbOffsetY
                            )
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            modifier = Modifier.alpha(verticalAlpha),
            text = value,
//            color = Color.White,
//            style = MaterialTheme.typography.h5,
            textAlign = TextAlign.Center,
        )
    }
}

private fun manageQuickChangingValues(
    dragDirection: DragDirection,
    scope: CoroutineScope,
    thumbOffsetX: Animatable<Float, AnimationVector1D>,
    dragLimitHorizontalPx: () -> Float,
    onAction: (DraggableButtonAction) -> Unit,
    onLaunch: ((Job) -> Unit),
) {
    if (dragDirection == DragDirection.NONE) {
        val job = scope.launch {
            delay(COUNTER_DELAY_INITIAL_MS)

            var elapsed = COUNTER_DELAY_INITIAL_MS

            val condition = isActive
                    && thumbOffsetX.value.absoluteValue >= (dragLimitHorizontalPx() * DRAG_LIMIT_HORIZONTAL_THRESHOLD_FACTOR)

            while (condition) {
                if (thumbOffsetX.value.sign > 0) {
                    onAction(DraggableButtonAction.Increase)
                } else {
                    onAction(DraggableButtonAction.Decrease)
                }

                delay(COUNTER_DELAY_FAST_MS)
                elapsed += COUNTER_DELAY_FAST_MS
            }
        }

        onLaunch(job)
    }
}

private suspend fun manageHorizontalDrag(
    dragDirectionState: MutableState<DragDirection>,
    thumbOffsetX: Animatable<Float, AnimationVector1D>,
    dragAmount: Offset,
    dragLimitHorizontalPx: () -> Float,
) {
// mark horizontal dragging direction to prevent vertical dragging until released
    dragDirectionState.value = DragDirection.HORIZONTAL

    // calculate the drag factor so the more the thumb
    // is closer to the border, the more effort it takes to drag it
    val dragFactor = (1 - (thumbOffsetX.value / dragLimitHorizontalPx())
        .absoluteValue) * 1.1F
    val delta = dragAmount.x * dragFactor

    val targetValue = thumbOffsetX.value + delta
    val targetValueWithinBounds =
        targetValue.coerceIn(
            -dragLimitHorizontalPx(),
            dragLimitHorizontalPx()
        )

    thumbOffsetX.snapTo(targetValueWithinBounds)
}

private suspend fun manageVerticalDrag(
    dragDirectionState: MutableState<DragDirection>,
    thumbOffsetY: Animatable<Float, AnimationVector1D>,
    dragAmount: Offset,
    dragLimitVerticalPx: () -> Float,
) {
    // mark vertical dragging direction to prevent horizontal dragging until released
    dragDirectionState.value = DragDirection.VERTICAL

    val dragFactor =
        1 - (thumbOffsetY.value / dragLimitVerticalPx()).absoluteValue
    val delta =
        dragAmount.y * dragFactor

    val targetValue = thumbOffsetY.value + delta
    val targetValueWithinBounds =
        targetValue.coerceIn(
            -dragLimitVerticalPx(),
            dragLimitVerticalPx()
        )

    thumbOffsetY.snapTo(targetValueWithinBounds)
}

private fun handleButtonRelease(
    thumbOffsetX: Animatable<Float, AnimationVector1D>,
    thumbOffsetY: Animatable<Float, AnimationVector1D>,
    dragLimitHorizontalPx: () -> Float,
    dragLimitVerticalPx: () -> Float,
    onAction: (DraggableButtonAction) -> Unit,

    ) {
    if (thumbOffsetX.value.absoluteValue >= (dragLimitHorizontalPx() * DRAG_LIMIT_HORIZONTAL_THRESHOLD_FACTOR)) {
        val action = if (thumbOffsetX.value.sign > 0) {
            DraggableButtonAction.Increase
        } else {
            DraggableButtonAction.Decrease
        }

        onAction(action)
    } else if (thumbOffsetY.value.absoluteValue >= (dragLimitVerticalPx() * DRAG_LIMIT_VERTICAL_THRESHOLD_FACTOR)) {
        onAction(DraggableButtonAction.Reset)
    }
}

private suspend fun returnDragButtonIdDeeded(
    dragDirectionState: MutableState<DragDirection>,
    thumbOffsetX: Animatable<Float, AnimationVector1D>,
    thumbOffsetY: Animatable<Float, AnimationVector1D>,
) {

    if (dragDirectionState.value == DragDirection.HORIZONTAL && thumbOffsetX.value != 0f) {
        logD("returning button HORIZONTAL")
        thumbOffsetX.animateTo(
            targetValue = 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = StiffnessLow
            )
        )
        // reset drag direction
        dragDirectionState.value = DragDirection.NONE
    } else if (dragDirectionState.value == DragDirection.VERTICAL && thumbOffsetY.value != 0f) {
        thumbOffsetY.animateTo(
            targetValue = 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = StiffnessLow
            )
        )
        // reset drag direction
        dragDirectionState.value = DragDirection.NONE
    }
}

@Composable
private fun Dp.dpToPx() = with(LocalDensity.current) { this@dpToPx.toPx() }

private enum class DragDirection {
    NONE, HORIZONTAL, VERTICAL
}