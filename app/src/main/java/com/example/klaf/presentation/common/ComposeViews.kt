package com.example.klaf.presentation.common

import androidx.annotation.StringRes
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Popup
import com.example.klaf.R
import com.example.klaf.domain.common.ifTrue
import com.example.klaf.domain.ipa.LetterInfo
import com.example.klaf.presentation.cardAddition.AutocompleteState
import com.example.klaf.presentation.theme.MainTheme

@Composable
fun CardManagementView(
    deckName: String,
    cardQuantity: Int,
    letterInfos: List<LetterInfo>,
    nativeWord: String,
    foreignWord: String,
    ipaTemplate: String,
    autocompleteState: AutocompleteState,
    onDismissRequest: () -> Unit,
    onLetterClick: (index: Int, letterInfo: LetterInfo) -> Unit,
    onNativeWordChange: (String) -> Unit,
    onForeignWordChange: (String) -> Unit,
    onIpaChange: (String) -> Unit,
    onConfirmClick: () -> Unit,
    onPronounceIconClick: () -> Unit,
    onAutocompleteItemClick: (chosenWord: String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .noRippleClickable { onDismissRequest() }
            .padding(32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.3F),
        ) {
            DeckInfo(name = deckName, cardQuantity = cardQuantity)
            ForeignWordLettersSelector(
                letterInfos = letterInfos,
                onLetterClick = onLetterClick,
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            CardManagementFields(
                modifier = Modifier.align(alignment = Alignment.TopCenter),
                nativeWord = nativeWord,
                foreignWord = foreignWord,
                ipaTemplate = ipaTemplate,
                autocompleteState = autocompleteState,
                onNativeWordChange = onNativeWordChange,
                onForeignWordChange = onForeignWordChange,
                onIpaChange = onIpaChange,
                onPronounceIconClick = onPronounceIconClick,
                onAutocompleteItemClick = onAutocompleteItemClick
            )

            RoundButton(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp),
                background = MainTheme.colors.positiveDialogButton,
                iconId = R.drawable.ic_confirmation_24,
                onClick = onConfirmClick
            )
        }
    }
}

@Composable
fun ColumnScope.ForeignWordLettersSelector(
    letterInfos: List<LetterInfo>,
    onLetterClick: (index: Int, letterInfo: LetterInfo) -> Unit
) {
    LazyRow(
        state = rememberLazyListState(),
        modifier = Modifier
            .weight(0.5F)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        itemsIndexed(items = letterInfos) { index, letterInfo ->
            LetterItem(
                letterInfo = letterInfo,
                onClick = { onLetterClick(index, letterInfo) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LazyItemScope.LetterItem(
    letterInfo: LetterInfo,
    onClick: () -> Unit,
) {
    val cellColor = when {
        letterInfo.isChecked -> MainTheme.colors.checkedLetterCell
        else -> MainTheme.colors.uncheckedLetterCell
    }

    Text(
        modifier = Modifier
            .animateItemPlacement()
            .padding(4.dp)
            .clickable { onClick() }
            .clip(shape = RoundedCornerShape(4.dp))
            .background(cellColor)
            .padding(4.dp),
        text = letterInfo.letter,
        fontSize = if (letterInfo.isChecked) 30.sp else 24.sp,
        style = MainTheme.typographies.dialogTextStyle.copy()
    )
}

@Composable
fun ColumnScope.DeckInfo(name: String, cardQuantity: Int) {
    Column(
        modifier = Modifier
            .weight(0.5F)
            .fillMaxWidth(),
    ) {
        Pointer(
            pointerTextId = R.string.pointer_deck,
            valueText = name
        )

        Pointer(
            pointerTextId = R.string.pointer_card_quantity,
            valueText = cardQuantity.toString()
        )
    }
}

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
fun CardManagementFields(
    nativeWord: String,
    foreignWord: String,
    ipaTemplate: String,
    autocompleteState: AutocompleteState,
    onNativeWordChange: (String) -> Unit,
    onForeignWordChange: (String) -> Unit,
    onIpaChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onPronounceIconClick: () -> Unit,
    onAutocompleteItemClick: (chosenWord: String) -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        WordTextField(
            value = nativeWord,
            labelTextId = R.string.label_native_word,
            textColor = MainTheme.colors.cardManagementViewColors.nativeWord,
            onValueChange = onNativeWordChange,
        )

        DropDownAutocompleteFiled(
            expanded = autocompleteState.isActive && autocompleteState.autocomplete.isNotEmpty(),
            typedWord = foreignWord,
            onTypedWordChange = onForeignWordChange,
            onPronounceIconClick = onPronounceIconClick,
            autocompleteState = autocompleteState,
            onAutocompleteItemClick = onAutocompleteItemClick
        )

        WordTextField(
            value = ipaTemplate,
            labelTextId = R.string.label_ipa,
            textColor = MainTheme.colors.cardManagementViewColors.ipa,
            onValueChange = onIpaChange
        )
    }
}

@Composable
fun DropDownAutocompleteFiled(
    expanded: Boolean,
    typedWord: String,
    onTypedWordChange: (String) -> Unit,
    autocompleteState: AutocompleteState,
    onPronounceIconClick: () -> Unit,
    onAutocompleteItemClick: (chosenWord: String) -> Unit,
) {
    var sizeTopBar by remember { mutableStateOf(IntSize.Zero) }
    var textFieldPosition by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier.onGloballyPositioned { coordinates ->
            sizeTopBar = coordinates.size
            textFieldPosition = coordinates.positionInWindow()
        }
    ) {
        WordTextFieldForPopupMenu(
            value = typedWord,
            labelTextId = R.string.label_foreign_word,
            textColor = MainTheme.colors.cardManagementViewColors.foreignWord,
            onValueChange = onTypedWordChange,
            trailingIcon = {
                Icon(
                    modifier = Modifier
                        .clip(shape = RoundedCornerShape(50.dp))
                        .clickable { onPronounceIconClick() }
                        .padding(5.dp),
                    painter = painterResource(id = R.drawable.ic_baseline_volume_up_24),
                    contentDescription = null
                )
            }
        )

        expanded.ifTrue {
            Popup(offset = IntOffset(x = 0, y = sizeTopBar.height)) {
                val menuShape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)

                Column(
                    modifier = Modifier
                        .width(LocalDensity.current.run { sizeTopBar.width.toDp() })
                        .shadow(elevation = 4.dp, shape = menuShape)
                        .clip(shape = menuShape)
                        .background(MainTheme.colors.cardManagementViewColors.autocompleteMenuBackground)
                        .padding(start = 16.dp, top = 6.dp, end = 16.dp, bottom = 6.dp)
                ) {
                    autocompleteState.autocomplete.onEach { word ->
                        AutocompleteWordItem(
                            word = word.value,
                            prefix = autocompleteState.prefix,
                            onAutocompleteItemClick = onAutocompleteItemClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AutocompleteWordItem(
    word: String,
    prefix: String,
    onAutocompleteItemClick: (String) -> Unit,
) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAutocompleteItemClick(word) },
        text = buildAnnotatedString {
            withStyle(style = MainTheme.typographies.foreignWordAutocompleteSpanStyle) {
                append(prefix)
            }
            append(word.drop(prefix.length))
        },
        fontStyle = FontStyle.Italic
    )
}

@Composable
private fun WordTextField(
    modifier: Modifier = Modifier,
    value: String,
    @StringRes labelTextId: Int,
    textColor: Color,
    onValueChange: (String) -> Unit,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    TextField(
        modifier = modifier,
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = stringResource(id = labelTextId)) },
        colors = TextFieldDefaults.textFieldColors(
            backgroundColor = MainTheme.colors.cardManagementViewColors.textFieldBackground,
            textColor = textColor
        ),
        trailingIcon = trailingIcon
    )
}

@Composable
private fun WordTextFieldForPopupMenu(
    modifier: Modifier = Modifier,
    value: String,
    @StringRes labelTextId: Int,
    textColor: Color,
    onValueChange: (String) -> Unit,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    var textFieldValue by rememberAsMutableStateOf(
        value = TextFieldValue(text = value, selection = TextRange(value.length))
    )

    if (textFieldValue.text != value) {
        textFieldValue = TextFieldValue(text = value, selection = TextRange(value.length))
    }

    TextField(
        modifier = modifier,
        value = textFieldValue,
        onValueChange = {
            if (it.text != value) {
                onValueChange(it.text)
            }
            textFieldValue = it
        },
        label = { Text(text = stringResource(id = labelTextId)) },
        colors = TextFieldDefaults.textFieldColors(
            backgroundColor = MainTheme.colors.cardManagementViewColors.textFieldBackground,
            textColor = textColor
        ),
        trailingIcon = trailingIcon
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
    bottomContent: @Composable (RowScope.() -> Unit)? = null,
    topContent: @Composable (RowScope.() -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
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
        background = MainTheme.colors.negativeDialogButton,
        iconId = R.drawable.ic_delete_24,
        onClick = onClick
    )
}

@Composable
fun ClosingButton(onClick: () -> Unit) {
    RoundButton(
        background = MainTheme.colors.neutralDialogButton,
        iconId = R.drawable.ic_close_24,
        onClick = onClick
    )
}

@Composable
fun ConfirmationButton(onClick: () -> Unit) {
    RoundButton(
        background = MainTheme.colors.positiveDialogButton,
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