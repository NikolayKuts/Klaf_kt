package com.example.klaf.presentation.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.common.ifTrue
import com.example.klaf.R
import com.example.klaf.presentation.theme.MainTheme

@Composable
fun Pointer(
    pointerTextId: Int,
    valueText: String,
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier,
        text = buildAnnotatedString {
            withStyle(style = MainTheme.typographies.cardPointer) {
                append("${stringResource(id = pointerTextId)}: ")
            }

            withStyle(style = MainTheme.typographies.cardAdditionPinterValue) {
                append(valueText)
            }
        },
        overflow = TextOverflow.Ellipsis,
        maxLines = 1
    )
}

@Composable
fun DialogBox(
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
            ),
        content = content
    )
}

@Composable
fun FullBackgroundDialog(
    onBackgroundClick: () -> Unit,
    mainContent: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    bottomContent: @Composable (RowScope.() -> Unit)? = null,
    topContent: @Composable (RowScope.() -> Unit)? = null,
    corners: Shape = RoundedCornerShape(10.dp),
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 16.dp)
            .noRippleClickable(onClick = onBackgroundClick)
    ) {
        Box(modifier = Modifier.align(Alignment.Center)) {
            Card(
                modifier = Modifier
                    .defaultMinSize(minHeight = 150.dp, minWidth = 300.dp)
                    .padding(
                        top = (DIALOG_BUTTON_SIZE / 2).dp,
                        bottom = (DIALOG_BUTTON_SIZE / 2).dp,
                    )
                    .clip(shape = corners),
            ) {
                Box(
                    modifier = Modifier
                        .noRippleClickable { }
                        .padding(MainTheme.dimensions.dialogContentPadding)
                        .bottomPadding(apply = bottomContent != null)
                        .topPadding(apply = topContent != null),
                    content = mainContent
                )
            }

            topContent?.let {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.5F)
                        .padding(bottom = (DIALOG_BUTTON_SIZE / 4).dp)
                        .align(alignment = Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceAround,
                    content = it
                )
            }

            bottomContent?.let {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.5F)
                        .align(alignment = Alignment.BottomCenter),
                    horizontalArrangement = Arrangement.SpaceAround,
                    content = it
                )
            }
        }
    }
}

fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier = composed {
    this.clickable(
        indication = null,
        interactionSource = remember { MutableInteractionSource() },
        onClick = onClick
    )
}

fun Modifier.topPadding(apply: Boolean): Modifier {
    return if (apply) this.padding(top = (DIALOG_BUTTON_SIZE / 4).dp) else this
}

fun Modifier.bottomPadding(apply: Boolean): Modifier {
    return if (apply) this.padding(bottom = (DIALOG_BUTTON_SIZE / 4).dp) else this
}

@Composable
fun DeletingButton(onClick: () -> Unit) {
    RoundButton(
        background = MainTheme.colors.common.negativeDialogButton,
        iconId = R.drawable.ic_delete_24,
        onClick = onClick
    )
}

@Composable
fun ClosingButton(onClick: () -> Unit) {
    RoundButton(
        background = MainTheme.colors.common.neutralDialogButton,
        iconId = R.drawable.ic_close_24,
        onClick = onClick
    )
}

@Composable
fun ConfirmationButton(onClick: () -> Unit) {
    RoundButton(
        background = MainTheme.colors.common.positiveDialogButton,
        iconId = R.drawable.ic_confirmation_24,
        onClick = onClick
    )
}

@Composable
fun CustomCheckBox(
    modifier: Modifier,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    checkBoxSize: Dp = 20.dp,
    borderWidth: Dp = 1.dp,
    checkedBoxColor: Color = MaterialTheme.colors.secondary,
    uncheckedBoxColor: Color = checkedBoxColor.copy(alpha = 0f),
    checkmarkColor: Color = MaterialTheme.colors.surface,
    checkedBorderColor: Color = checkedBoxColor,
    uncheckedBorderColor: Color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
    shape: Shape = RoundedCornerShape(size = 4.dp),
    contentDescription: String? = null,
) {
    val background = if (checked) checkedBoxColor else uncheckedBoxColor
    val borderColor = if (checked) checkedBorderColor else uncheckedBorderColor

    Box(
        modifier = modifier
            .clip(shape = shape)
            .background(color = background)
            .border(
                border = BorderStroke(width = borderWidth, color = borderColor),
                shape = RoundedCornerShape(size = 6.dp)
            )
            .size(checkBoxSize)
            .clickable { onCheckedChange(checked) },
        contentAlignment = Alignment.Center
    ) {
        checked.ifTrue {
            Icon(
                Icons.Default.Check,
                tint = checkmarkColor,
                contentDescription = contentDescription,
            )
        }
    }
}

@Composable
fun AdaptiveBox(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Center,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable LazyItemScope.(parentHeightPx: Float) -> Unit,
) {
    val density = LocalDensity.current
    val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp
    var parentHeightPx by rememberAsMutableStateOf(value = density.run { screenHeightDp.toPx() })

    LazyColumn(
        modifier = modifier.onSizeChanged { parentHeightPx = it.height.toFloat() },
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment,
    ) {
        item {
            content(parentHeightPx)
        }
    }
}

fun Modifier.verticalScrollbar(
    state: LazyListState,
    width: Dp = 3.dp,
    color: Color = Color.Gray,
    enterDuration: Int = 150,
    exitDuration: Int = 500,
    minAlpha: Float = 0f,
    maxAlpha: Float = 0.5f,
    alwaysVisible: Boolean = false,
): Modifier = composed {
    val targetAlpha = if (alwaysVisible || state.isScrollInProgress) maxAlpha else minAlpha
    val duration = if (state.isScrollInProgress) enterDuration else exitDuration

    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = duration)
    )

    drawWithContent {
        drawContent()

        val firstVisibleElementIndex = state.layoutInfo.visibleItemsInfo.firstOrNull()?.index
        val needDrawScrollbar = state.isScrollInProgress || alpha > 0f

        if (needDrawScrollbar && firstVisibleElementIndex != null) {
            val elementHeight = this.size.height / state.layoutInfo.totalItemsCount
            val scrollbarOffsetY = firstVisibleElementIndex * elementHeight
            val scrollbarHeight = state.layoutInfo.visibleItemsInfo.size * elementHeight

            drawRect(
                color = color,
                topLeft = Offset(this.size.width - width.toPx(), scrollbarOffsetY),
                size = Size(width = width.toPx(), height = scrollbarHeight),
                alpha = alpha
            )
        }
    }
}

fun Modifier.verticalScrollBar(
    state: ScrollState,
    visibleHeight: Dp,
    width: Dp = 5.dp,
    color: Color = Color.Gray,
    enterDuration: Int = 150,
    exitDuration: Int = 1000,
    minAlpha: Float = 0f,
    maxAlpha: Float = 0.5f,
    alwaysVisible: Boolean = false,
): Modifier = composed {
    val targetAlpha = if (alwaysVisible || state.isScrollInProgress) maxAlpha else minAlpha
    val duration = if (state.isScrollInProgress) enterDuration else exitDuration

    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = duration)
    )

    drawWithContent {
        drawContent()

        val needDrawScrollbar = state.isScrollInProgress || alpha > 0f

        if (needDrawScrollbar) {

            val fieldHeight = visibleHeight.toPx()
            val scrollPosition = state.value
            val maxScrollValue = state.maxValue
            val barHeight =
                fieldHeight - (maxScrollValue / (fieldHeight + maxScrollValue)) * fieldHeight
            val scrollPositionPercent =
                (scrollPosition * 100) / if (maxScrollValue == 0) 1 else maxScrollValue
            val maxOffsetValue = fieldHeight - barHeight
            val scrollbarOffsetY = ((maxOffsetValue * scrollPositionPercent) / 100) + scrollPosition

            drawRect(
                color = color,
                topLeft = Offset(x = this.size.width - width.toPx(), y = scrollbarOffsetY),
                size = Size(width = width.toPx(), height = barHeight),
                alpha = alpha,
            )
        }
    }
}