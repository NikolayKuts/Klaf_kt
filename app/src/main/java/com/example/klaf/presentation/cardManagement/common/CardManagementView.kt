package com.example.klaf.presentation.cardManagement.common

import androidx.annotation.StringRes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
import com.example.domain.common.LoadingState
import com.example.domain.common.ifTrue
import com.example.domain.common.skipOnNewLineCharacter
import com.example.domain.ipa.IpaHolder
import com.example.domain.ipa.LetterInfo
import com.example.klaf.R
import com.example.klaf.presentation.cardManagement.cardAddition.AutocompleteState
import com.example.klaf.presentation.common.*
import com.example.klaf.presentation.theme.MainTheme

private const val CARD_MANAGEMENT_CONTAINER_WIDTH = 500

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CardManagementView(
    deckName: String,
    cardQuantity: Int,
    letterInfos: List<LetterInfo>,
    nativeWord: String,
    foreignWord: String,
    ipaHolders: List<IpaHolder>,
    autocompleteState: AutocompleteState,
    pronunciationLoadingState: LoadingState<Unit>,
    onCloseAutocompletePopupMenuClick: () -> Unit,
    onLetterClick: (index: Int, letterInfo: LetterInfo) -> Unit,
    onNativeWordChange: (String) -> Unit,
    onForeignWordChange: (String) -> Unit,
    onIpaChange: (letterGroupIndex: Int, ipa: String) -> Unit,
    onConfirmClick: () -> Unit,
    onPronounceIconClick: () -> Unit,
    onAutocompleteItemClick: (chosenWord: String) -> Unit,
) {
    ScrollableBox { parentHeightPx ->
        val keyboardController = LocalSoftwareKeyboardController.current
        val density = LocalDensity.current
        val minContentHeightDp = 500.dp
        val confirmationButtonPadding = getConfirmationButtonPadding(
            parentHeightPx = parentHeightPx,
            minContentHeightPx = density.run { minContentHeightDp.toPx() },
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minContentHeightDp)
                .height(density.run { parentHeightPx.toDp() })
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            DeckInfo(name = deckName, cardQuantity = cardQuantity)

            Spacer(modifier = Modifier.fillMaxHeight(fraction = 0.1f))

            ForeignWordLettersSelector(
                letterInfos = letterInfos,
                onLetterClick = { index: Int, letterInfo: LetterInfo ->
                    onLetterClick(index, letterInfo)
                    onCloseAutocompletePopupMenuClick()
                }
            )

            Spacer(modifier = Modifier.fillMaxHeight(fraction = 0.1f))

            CardManagementFields(
                nativeWord = nativeWord,
                foreignWord = foreignWord,
                ipaHolders = ipaHolders,
                autocompleteState = autocompleteState,
                loadingState = pronunciationLoadingState,
                onNativeWordFieldClick = onCloseAutocompletePopupMenuClick,
                onNativeWordChange = onNativeWordChange,
                onForeignWordChange = onForeignWordChange,
                onIpaChange = onIpaChange,
                onPronounceIconClick = onPronounceIconClick,
                onAutocompleteItemClick = onAutocompleteItemClick,
                confirmationButtonSection = {
                    RoundButton(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(
                                end = confirmationButtonPadding,
                                bottom = confirmationButtonPadding,
                            ),
                        background = MainTheme.colors.common.positiveDialogButton,
                        iconId = R.drawable.ic_confirmation_24,
                        onClick = {
                            keyboardController?.hide()
                            onConfirmClick()
                        }
                    )
                },
            )
        }
    }
}

private fun getConfirmationButtonPadding(
    parentHeightPx: Float,
    minContentHeightPx: Float,
): Dp = if (
    parentHeightPx > 0F
    && parentHeightPx < minContentHeightPx
) 0.dp else 16.dp

@Composable
private fun ForeignWordLettersSelector(
    letterInfos: List<LetterInfo>,
    onLetterClick: (index: Int, letterInfo: LetterInfo) -> Unit,
) {
    LazyRow(
        state = rememberLazyListState(),
        modifier = Modifier
            .defaultMinSize(minHeight = 50.dp)
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
private fun LazyItemScope.LetterItem(
    letterInfo: LetterInfo,
    onClick: () -> Unit,
) {
    val cellColor = when {
        letterInfo.isChecked -> MainTheme.colors.cardManagementView.checkedLetterCell
        else -> MainTheme.colors.cardManagementView.uncheckedLetterCell
    }

    Text(
        modifier = Modifier
            .animateItemPlacement(animationSpec = tween(durationMillis = 700))
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
private fun DeckInfo(
    name: String,
    cardQuantity: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
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
private fun CardManagementFields(
    nativeWord: String,
    foreignWord: String,
    ipaHolders: List<IpaHolder>,
    autocompleteState: AutocompleteState,
    loadingState: LoadingState<Unit>,
    modifier: Modifier = Modifier,
    confirmationButtonSection: @Composable BoxScope.() -> Unit,
    onNativeWordFieldClick: () -> Unit,
    onNativeWordChange: (String) -> Unit,
    onForeignWordChange: (String) -> Unit,
    onIpaChange: (letterGroupIndex: Int, ipa: String) -> Unit,
    onPronounceIconClick: () -> Unit,
    onAutocompleteItemClick: (chosenWord: String) -> Unit,
) {
    Column(
        modifier = modifier.width(CARD_MANAGEMENT_CONTAINER_WIDTH.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        WordTextField(
            value = nativeWord,
            labelTextId = R.string.label_native_word,
            textColor = MainTheme.colors.cardManagementView.nativeWord,
            onClick = onNativeWordFieldClick,
            onValueChange = onNativeWordChange,
        )

        DropDownAutocompleteFiled(
            expanded = autocompleteState.isActive && autocompleteState.autocomplete.isNotEmpty(),
            typedWord = foreignWord,
            autocompleteState = autocompleteState,
            loadingState = loadingState,
            onTypedWordChange = onForeignWordChange,
            onPronounceIconClick = onPronounceIconClick,
            onAutocompleteItemClick = onAutocompleteItemClick,
        )

        IpaSection(
            ipaHolders = ipaHolders,
            onIpaChange = onIpaChange,
            confirmationButtonSection = confirmationButtonSection
        )
    }
}

@Composable
private fun WordTextField(
    value: String,
    @StringRes labelTextId: Int,
    textColor: Color,
    onClick: () -> Unit,
    onValueChange: (String) -> Unit,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    val scrollState = rememberScrollState()
    val maxVisibleHeight = 80.dp
    val interactionSource = remember { MutableInteractionSource() }

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Release) {
                onClick()
            }
        }
    }

    TextField(
        modifier = Modifier
            .width(CARD_MANAGEMENT_CONTAINER_WIDTH.dp)
            .heightIn(max = maxVisibleHeight)
            .verticalScroll(state = scrollState)
            .verticalScrollBar(state = scrollState, visibleHeight = maxVisibleHeight),
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = stringResource(id = labelTextId)) },
        colors = TextFieldDefaults.textFieldColors(
            backgroundColor = MainTheme.colors.cardManagementView.textFieldBackground,
            textColor = textColor
        ),
        trailingIcon = trailingIcon,
        interactionSource = interactionSource
    )
}

@Composable
private fun DropDownAutocompleteFiled(
    expanded: Boolean,
    typedWord: String,
    autocompleteState: AutocompleteState,
    loadingState: LoadingState<Unit>,
    onTypedWordChange: (String) -> Unit,
    onPronounceIconClick: () -> Unit,
    onAutocompleteItemClick: (chosenWord: String) -> Unit,
) {
    val density = LocalDensity.current
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    var textFieldPosition by remember { mutableStateOf(Offset.Zero) }
    var textFieldSize by remember { mutableStateOf(IntSize.Zero) }
    var itemsHeightDp by rememberAsMutableStateOf(value = 10.dp)
    var popupContentContainerHeight by rememberAsMutableStateOf(value = 0.dp)
    val popupMenuPadding = 6.dp

    LaunchedEffect(
        autocompleteState,
        textFieldPosition,
        textFieldSize,
        itemsHeightDp
    ) {
        val popupMenuPosition = density.run {
            textFieldPosition.y.toDp() + textFieldSize.height.toDp()
        }
        val freeContentHeight = screenHeightDp.dp - popupMenuPosition - 32.dp
        val neededHeight = itemsHeightDp * autocompleteState.autocomplete.size

        popupContentContainerHeight = if (neededHeight < freeContentHeight) {
            neededHeight
        } else {
            freeContentHeight
        } + popupMenuPadding * 2
    }

    Box(
        modifier = Modifier
            .onGloballyPositioned { coordinates ->
                textFieldPosition = coordinates.positionInRoot()
                textFieldSize = coordinates.size
            }
    ) {
        WordTextFieldForPopupMenu(
            value = typedWord,
            labelTextId = R.string.label_foreign_word,
            textColor = MainTheme.colors.cardManagementView.foreignWord,
            onValueChange = onTypedWordChange,
            trailingIcon = {
                val (iconColor: Color, clickable: Boolean) = when (loadingState) {
                    is LoadingState.Success -> {
                        MainTheme.colors.cardManagementView.activePronunciationIcon to true
                    }
                    else -> MainTheme.colors.cardManagementView.inactivePronunciationIcon to false
                }

                if (loadingState == LoadingState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(35.dp))
                }

                Icon(
                    modifier = Modifier
                        .clip(shape = RoundedCornerShape(50.dp))
                        .clickable(enabled = clickable) { onPronounceIconClick() }
                        .padding(5.dp),
                    painter = painterResource(id = R.drawable.ic_baseline_volume_up_24),
                    contentDescription = null,
                    tint = iconColor,
                )
            }
        )

        expanded.ifTrue {
            Popup(offset = IntOffset(x = 0, y = textFieldSize.height)) {
                val menuShape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
                val lazySate = rememberLazyListState()

                LazyColumn(
                    modifier = Modifier
                        .height(popupContentContainerHeight)
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
                        .verticalScrollbar(
                            state = lazySate,
                            color = MainTheme.colors.material.primary,
                        ),
                    state = lazySate
                ) {
                    autocompleteState.autocomplete.onEach { word ->
                        item {
                            AutocompleteWordItem(
                                modifier = Modifier.onSizeChanged { intSize ->
                                    itemsHeightDp = density.run { intSize.height.toDp() }
                                },
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
}

@Composable
private fun WordTextFieldForPopupMenu(
    value: String,
    @StringRes labelTextId: Int,
    textColor: Color,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    var textFieldValue by rememberAsMutableStateOf(
        value = TextFieldValue(text = value, selection = TextRange(value.length))
    )

    if (textFieldValue.text != value) {
        textFieldValue = TextFieldValue(text = value, selection = TextRange(value.length))
    }

    TextField(
        modifier = modifier.width(CARD_MANAGEMENT_CONTAINER_WIDTH.dp),
        value = textFieldValue,
        onValueChange = {
            if (it.text != value) {
                onValueChange(it.text)
            }
            textFieldValue = it
        },
        label = { Text(text = stringResource(id = labelTextId)) },
        colors = TextFieldDefaults.textFieldColors(
            backgroundColor = MainTheme.colors.cardManagementView.textFieldBackground,
            textColor = textColor
        ),
        trailingIcon = trailingIcon,
        singleLine = true,
    )
}

@Composable
private fun AutocompleteWordItem(
    word: String,
    prefix: String,
    onAutocompleteItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onAutocompleteItemClick(word) },
        text = buildAnnotatedString {
            withStyle(
                style = MainTheme.typographies
                    .cardManagementViewTextStyles
                    .foreignWordAutocompleteSpanStyle
            ) {
                append(prefix)
            }

            append(word.drop(prefix.length))
        },
        fontStyle = FontStyle.Italic
    )
}

@Composable
private fun IpaSection(
    ipaHolders: List<IpaHolder>,
    onIpaChange: (letterGroupIndex: Int, ipa: String) -> Unit,
    confirmationButtonSection: @Composable BoxScope.() -> Unit,
) {
    var parentWidthPx by rememberAsMutableStateOf(value = 0F)
    val cellShape = RoundedCornerShape(size = 6.dp)
    val equalSing = "="
    val density = LocalDensity.current
    val chosenLettersWidthPx = parentWidthPx * 0.6F
    val ipaValueWidthPx = parentWidthPx * 0.5F
    val scrollState = rememberLazyListState()

    Box(modifier = Modifier.fillMaxHeight(fraction = 1f)) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { parentWidthPx = it.width.toFloat() }
                .padding(start = 16.dp, end = 16.dp, top = 6.dp)
                .verticalScrollbar(
                    state = scrollState,
                    color = MainTheme.colors.material.primary,
                ),
            state = scrollState,
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(bottom = ROUNDED_ELEMENT_SIZE.dp * 2 / 3)
        ) {
            itemsIndexed(items = ipaHolders) { letterGroupIndex, ipaHolder ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 3.dp, bottom = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        modifier = Modifier
                            .widthIn(max = density.run { chosenLettersWidthPx.toDp() })
                            .clip(shape = cellShape)
                            .background(color = MainTheme.colors.cardManagementView.checkedLetterCell)
                            .padding(6.dp),
                        text = ipaHolder.letterGroup,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1
                    )

                    Text(
                        modifier = Modifier.padding(start = 6.dp, end = 6.dp),
                        text = equalSing,
                    )

                    BasicTextField(
                        modifier = Modifier
                            .widthIn(min = 30.dp, max = density.run { ipaValueWidthPx.toDp() })
                            .width(IntrinsicSize.Min)
                            .clip(shape = cellShape)
                            .background(MainTheme.colors.cardManagementView.ipaCellBackground)
                            .padding(6.dp),
                        value = ipaHolder.ipa,
                        cursorBrush = Brush.verticalGradient(
                            0.00f to Color.Transparent,
                            0.15f to Color.Transparent,
                            0.15f to MainTheme.colors.material.onPrimary,
                            0.90f to MainTheme.colors.material.onPrimary,
                            0.90f to Color.Transparent,
                            1.00f to Color.Transparent,
                        ),
                        onValueChange = { newText ->
                            onIpaChange(letterGroupIndex, newText.skipOnNewLineCharacter())
                        },
                        textStyle = MainTheme.typographies.cardManagementViewTextStyles.ipaValue,
                    )
                }
            }
        }

        confirmationButtonSection()
    }
}