package com.kuts.klaf.presentation.common

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kuts.domain.common.ifNotNull
import com.kuts.domain.common.ifTrue
import com.kuts.klaf.R
import com.kuts.klaf.presentation.theme.MainTheme

val MinElementWidth = 400.dp

val Configuration.isOrientationLandscape: Boolean
    get() = this.orientation == Configuration.ORIENTATION_LANDSCAPE

const val DIALOG_APP_LABEL_SIZE = 70

data class ContentHolder(val size: Dp, val content: @Composable RowScope.() -> Unit)

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
fun FullBackgroundDialog(
    onBackgroundClick: () -> Unit,
    mainContent: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    mainContentModifier: Modifier = Modifier,
    topContent: ContentHolder? = null,
    bottomContent: @Composable (RowScope.() -> Unit)? = null,
    corners: Shape = RoundedCornerShape(10.dp),
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val sidePadding = if (screenWidth <= 320.dp) 0.dp else 16.dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(start = sidePadding, end = sidePadding, bottom = 16.dp)
            .noRippleClickable(onClick = onBackgroundClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier.align(Alignment.Center),
            contentAlignment = Alignment.Center,
        ) {
            val isScreenTurned = screenWidth > 520.dp
            val isScreenSmall = screenWidth <= 320.dp
            val fraction = if (configuration.isOrientationLandscape && isScreenTurned) {
                0.6F
            } else {
                if (isScreenSmall) 1F else 0.8F
            }

            val maxCardWidth = screenWidth * fraction

            Card(
                modifier = Modifier
                    .heightIn(min = 150.dp)
                    .widthIn(min = Dp.Unspecified, max = maxCardWidth)
                    .padding(
                        top = ((topContent?.size ?: 0.dp) / 2),
                        bottom = (ROUNDED_ELEMENT_SIZE.dp / 2),
                    )
                    .clip(shape = corners),
                backgroundColor = MainTheme.colors.common.dialogBackground,
                contentColor = contentColorFor(MaterialTheme.colors.surface),
            ) {
                Box(
                    modifier = mainContentModifier
                        .noRippleClickable { }
                        .padding(MainTheme.dimensions.dialogContentPadding)
                        .bottomPadding(apply = bottomContent != null)
                        .topPadding(apply = topContent != null),
                    content = mainContent,
                    contentAlignment = Alignment.Center
                )
            }

            topContent?.let { contentHolder ->
                Row(
                    modifier = Modifier.align(alignment = Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceAround,
                    content = contentHolder.content
                )
            }

            bottomContent?.let {
                Row(
                    modifier = Modifier
                        .widthIn(min = 180.dp)
                        .align(alignment = Alignment.BottomCenter),
                    horizontalArrangement = Arrangement.SpaceAround,
                    content = it
                )
            }
        }
    }
}

@Composable
fun TransparentSurface(content: @Composable () -> Unit) {
    Surface(
        color = Color.Transparent,
        contentColor = contentColorFor(MaterialTheme.colors.surface),
        content = content,
    )
}

fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier = composed {
    this.clickable(
        indication = null,
        interactionSource = remember { MutableInteractionSource() },
        onClick = onClick
    )
}

private fun Modifier.topPadding(apply: Boolean): Modifier {
    return if (apply) this.padding(top = (ROUNDED_ELEMENT_SIZE / 4).dp) else this
}

private fun Modifier.bottomPadding(apply: Boolean): Modifier {
    return if (apply) this.padding(bottom = (ROUNDED_ELEMENT_SIZE / 4).dp) else this
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
fun ScrollableBox(
    modifier: Modifier = Modifier,
    reverseLayout: Boolean = false,
    dialogMode: Boolean = false,
    verticalArrangement: Arrangement.Vertical =
        if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    eventContent: @Composable BoxScope.() -> Unit = {},
    content: @Composable LazyItemScope.(parentHeightPx: Float) -> Unit,
) {
    val density = LocalDensity.current
    val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp
    var parentHeightPx by rememberAsMutableStateOf(value = density.run { screenHeightDp.toPx() })

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { parentHeightPx = it.height.toFloat() },
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .align(alignment = if (dialogMode) Alignment.Center else Alignment.TopCenter),
            reverseLayout = reverseLayout,
            verticalArrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment,
        ) {
            item { content(parentHeightPx) }
        }

        eventContent()
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

@Composable
fun CardDeletingDialogView(
    cardQuantity: Int,
    eventMessage: EventMessage?,
    onConfirmDeleting: () -> Unit,
    onCancel: () -> Unit,
) {
    ScrollableBox(
        modifier = Modifier.noRippleClickable { onCancel() },
        dialogMode = true,
        eventContent = {
            eventMessage.ifNotNull { EventMessageView(message = it) }
        }
    ) {
        FullBackgroundDialog(
            onBackgroundClick = onCancel,
            topContent = ContentHolder(size = ROUNDED_ELEMENT_SIZE.dp) {
                RoundedIcon(
                    background = MainTheme.colors.common.negativeDialogButton,
                    iconId = R.drawable.ic_attention_mark_24
                )
            },
            mainContent = {
                Text(
                    modifier = Modifier,
                    style = MainTheme.typographies.dialogTextStyle,
                    text = getDialogTitleByCardCount(quantity = cardQuantity)
                )
            },
            bottomContent = {
                DeletingButton(onClick = onConfirmDeleting)
                ClosingButton(onClick = onCancel)
            },
        )
    }
}

@Composable
fun DialogAppLabel() {
    val filterColor = MainTheme.colors.common.appLabelColorFilter

    Image(
        modifier = Modifier
            .size(70.dp)
            .clip(shape = RoundedCornerShape(50.dp))
            .background(MainTheme.colors.common.dialogBackground)
            .padding(10.dp),
        painter = painterResource(id = R.drawable.ic_app_labale),
        contentDescription = null,
        colorFilter = ColorFilter.lighting(filterColor, filterColor)
    )
}

@Composable
fun WarningMessage(@StringRes textId: Int) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(6.dp))
            .background(animatedWarningColor())
            .padding(16.dp),
        text = stringResource(textId),
        style = MainTheme.typographies.dialogTextStyle
    )
}

@Composable
private fun getDialogTitleByCardCount(quantity: Int): String {
    return if (quantity == 1) {
        stringResource(id = R.string.single_cards_deleting_dialog_title, quantity)
    } else {
        stringResource(id = R.string.multiple_cards_deleting_dialog_title, quantity)
    }
}

@Composable
private fun animatedWarningColor(): Color = rememberInfiniteTransition()
    .animateColor(
        initialValue = MainTheme.colors.dataSynchronizationView.initialWarning,
        targetValue = MainTheme.colors.dataSynchronizationView.targetWarning,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse
        )
    ).value