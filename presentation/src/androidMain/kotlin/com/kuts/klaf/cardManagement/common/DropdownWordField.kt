package com.kuts.klaf.cardManagement.common

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.kuts.domain.common.IWordable
import com.kuts.domain.common.ifTrue
import com.kuts.klaf.common.rememberAsMutableStateOf
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import com.kuts.klaf.common.verticalScrollbar
import com.kuts.klaf.theme.MainTheme
import com.lib.lokdroid.core.logD

private val fallbackAvailableDropdownHeight = 600.dp

@Composable
fun <T : IWordable> DropDownWordField(
    expanded: Boolean,
    typedTextFieldValue: TextFieldValue,
    dropdownContent: List<T>,
    trailingIcon: @Composable () -> Unit,
    labelRes: StringResource,
    textColor: Color,
    onTextFieldClick: () -> Unit,
    onTypedWordFieldValueChange: (TextFieldValue) -> Unit,
    itemContent: @Composable (
        wordable: T,
        wordableIndex: Int,
        onSizeChange: (IntSize) -> Unit
    ) -> Unit,
    bottomDropdownMenuContent: @Composable () -> Unit = { },
) {
    BoxWithConstraints(modifier = Modifier) {
        val density = LocalDensity.current
        val availableHeightDp: Dp =
            if (maxHeight != Dp.Infinity) maxHeight else fallbackAvailableDropdownHeight
        var textFieldPosition by remember { mutableStateOf(Offset.Zero) }
        var textFieldSize by remember { mutableStateOf(IntSize.Zero) }
        var itemHeightDp by rememberAsMutableStateOf(value = 10.dp)
        var popupContentContainerHeightDp by rememberAsMutableStateOf(value = 0.dp)
        val popupMenuPadding = 6.dp
        var bottomDropdownMenuContentHeightDp by remember { mutableStateOf(0.dp) }

        val interactionSource = remember { MutableInteractionSource() }

        LaunchedEffect(interactionSource) {
            interactionSource.interactions.collect { interaction ->
                if (interaction is PressInteraction.Release) {
                    onTextFieldClick()
                }
            }
        }

        LaunchedEffect(
            dropdownContent,
            textFieldSize,
            itemHeightDp,
            textFieldPosition,
            bottomDropdownMenuContentHeightDp,
            availableHeightDp,
        ) {
            val popupMenuPositionDp = density.run {
                textFieldPosition.y.toDp() + textFieldSize.height.toDp()
            }
            val freeContentHeight =
                (availableHeightDp - popupMenuPositionDp - popupMenuPadding * 2).coerceAtLeast(0.dp)
            val neededHeight =
                (itemHeightDp * dropdownContent.size) +
                    bottomDropdownMenuContentHeightDp +
                    popupMenuPadding * 2

            popupContentContainerHeightDp = if (neededHeight < freeContentHeight) {
                neededHeight
            } else {
                freeContentHeight
            }
        }

        Box(modifier = Modifier) {
            TextField(
                modifier = Modifier
                    .onGloballyPositioned { coordinates ->
                        textFieldPosition = coordinates.positionInRoot()
                        textFieldSize = coordinates.size
                    }
                    .width(500.dp),
                value = typedTextFieldValue,
                onValueChange = {
                    logD("onValueChange() called")
                    onTypedWordFieldValueChange(it)
                },
                label = { Text(text = stringResource(resource = labelRes)) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MainTheme.colors.cardManagementView.textFieldBackground,
                    unfocusedContainerColor = MainTheme.colors.cardManagementView.textFieldBackground,
                ),
                trailingIcon = trailingIcon,
                singleLine = true,
                interactionSource = interactionSource
            )

            expanded.ifTrue {
                Popup(offset = IntOffset(x = 0, y = textFieldSize.height)) {
                    val menuShape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
                    val lazyListSate = rememberLazyListState()
                    val bottomContentDividerSpace =
                        if (bottomDropdownMenuContentHeightDp == 0.dp) 0.dp else 16.dp
                    val lazyLiatHeight =
                        popupContentContainerHeightDp -
                            bottomDropdownMenuContentHeightDp -
                            bottomContentDividerSpace

                    Box(
                        Modifier
                            .height(popupContentContainerHeightDp)
                            .width(density.run { textFieldSize.width.toDp() })
                            .shadow(elevation = 4.dp, shape = menuShape)
                            .clip(shape = menuShape)
                            .background(MainTheme.colors.cardManagementView.autocompleteMenuBackground)
                            .padding(
                                start = 16.dp,
                                top = popupMenuPadding,
                                end = 16.dp,
                                bottom = popupMenuPadding,
                            )
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .height(lazyLiatHeight)
                                .verticalScrollbar(
                                    state = lazyListSate,
                                    color = MainTheme.colors.material.primary,
                                ),
                            state = lazyListSate
                        ) {
                            dropdownContent.onEachIndexed { index, wordable ->
                                item {
                                    itemContent(wordable, index) { intSize ->
                                        itemHeightDp = density.run { intSize.height.toDp() }
                                    }
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .onGloballyPositioned { coordinates ->
                                    bottomDropdownMenuContentHeightDp = density.run {
                                        coordinates.size.height.toDp()
                                    }
                                }
                        ) {
                            bottomDropdownMenuContent()
                        }
                    }
                }
            }
        }
    }
}
