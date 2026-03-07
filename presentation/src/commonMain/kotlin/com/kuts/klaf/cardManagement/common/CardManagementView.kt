package com.kuts.klaf.cardManagement.common

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kuts.domain.common.LoadingState
import com.kuts.domain.ipa.LetterInfo
import com.kuts.klaf.cardManagement.cardAddition.AutocompleteState
import com.kuts.klaf.cardManagement.cardAddition.NativeWordSuggestionsState
import com.kuts.klaf.common.Pointer
import com.kuts.klaf.common.RoundButton
import com.kuts.klaf.common.ScrollableBox
import com.kuts.klaf.common.noRippleClickable
import com.kuts.klaf.presentation.resources.*
import com.kuts.klaf.theme.MainTheme

@Composable
fun CardManagementView(
    deckName: String,
    cardQuantity: Int,
    letterInfos: List<LetterInfo>,
    foreignWordFieldValue: TextFieldValue,
    nativeWordFieldValue: TextFieldValue,
    textFieldValueIpaHolders: List<TextFieldValueIpaHolder>,
    autocompleteState: AutocompleteState,
    pronunciationLoadingState: LoadingState<Unit, Unit>,
    cambridgeDataAvailable: Boolean,
    ipaKeyboardState: IpaKeyboardState,
    onBottomSheetAction: () -> Unit,
    closeAutocompletePopupMenu: () -> Unit,
    onLetterClick: (index: Int, letterInfo: LetterInfo) -> Unit,
    onForeignWordTextFieldClick: () -> Unit,
    onForeignWordFieldValueChange: (TextFieldValue) -> Unit,
    onIpaTextFieldValueChange: (letterGroupIndex: Int, ipa: TextFieldValue) -> Unit,
    onConfirmClick: () -> Unit,
    onPronounceIconClick: () -> Unit,
    onAutocompleteItemClick: (chosenWord: String) -> Unit,
    transcription: String,
    closeNativeWordSuggestionsPopupMenu: () -> Unit,
    onNativeWordFieldValueChange: (TextFieldValue) -> Unit,
    nativeWordSuggestionsState: NativeWordSuggestionsState,
    onNativeWordFieldArrowIconClick: () -> Unit,
    onNativeWordSuggestionItemClick: (chosenWordIndex: Int) -> Unit,
    onConfirmSuggestionsSelection: () -> Unit,
    onClearNativeWordSuggestionsSelectionClick: () -> Unit,
    onIpaTextFieldFocusChanged: (List<IpaTextFieldFocusState>) -> Unit,
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
                .noRippleClickable {
                    closeAutocompletePopupMenu()
                    closeNativeWordSuggestionsPopupMenu()
                }
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
                    closeAutocompletePopupMenu()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Transcription(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                text = transcription
            )
            Spacer(modifier = Modifier.fillMaxHeight(fraction = 0.1f))

            CardManagementFields(
                nativeWordFieldValue = nativeWordFieldValue,
                foreignWordFieldValue = foreignWordFieldValue,
                textFieldValueIpaHolders = textFieldValueIpaHolders,
                autocompleteState = autocompleteState,
                loadingState = pronunciationLoadingState,
                onForeignWordTextFieldClick = onForeignWordTextFieldClick,
                onForeignWordFieldValueChange = onForeignWordFieldValueChange,
                onIpaTextFieldValueChange = onIpaTextFieldValueChange,
                onPronounceIconClick = onPronounceIconClick,
                onAutocompleteItemClick = onAutocompleteItemClick,
                onNativeWordFieldValueChange = onNativeWordFieldValueChange,
                onNativeWordFieldClick = closeAutocompletePopupMenu,
                onNativeWordSuggestionClick = onNativeWordSuggestionItemClick,
                onNativeWordFieldArrowIconClick = onNativeWordFieldArrowIconClick,
                nativeWordSuggestionsState = nativeWordSuggestionsState,
                onConfirmSuggestionsSelection = onConfirmSuggestionsSelection,
                onClearSelectionClick = onClearNativeWordSuggestionsSelectionClick,
                onIpaTextFieldFocusChanged = onIpaTextFieldFocusChanged,
                confirmationButtonSection = {
                    Column(modifier = Modifier.align(Alignment.BottomCenter)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            if (cambridgeDataAvailable) {
                                RoundButton(
                                    modifier = Modifier
                                        .padding(
                                            end = confirmationButtonPadding,
                                            bottom = confirmationButtonPadding,
                                        ),
                                    background = Color(0xff59bdc0),
                                    iconRes = Res.drawable.ic_arrow_drop_down_24,
                                    onClick = onBottomSheetAction
                                )
                            }

                            RoundButton(
                                modifier = Modifier
                                    .padding(
                                        end = confirmationButtonPadding,
                                        bottom = confirmationButtonPadding,
                                    ),
                                background = MainTheme.colors.common.positiveDialogButton,
                                iconRes = Res.drawable.ic_confirmation_24,
                                onClick = {
                                    keyboardController?.hide()
                                    onConfirmClick()
                                }
                            )
                        }

                        if (ipaKeyboardState.enabled) {
                            IpaKeyboard(
                                ipaKeyboardState = ipaKeyboardState,
                                onIpaTextFieldValueChange = onIpaTextFieldValueChange
                            )
                        }
                    }
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

@Composable
fun Transcription(modifier: Modifier, text: String) {
    Text(
        modifier = modifier,
        style = MainTheme.typographies.cardManagementTranscription,
        text = text,
    )
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
            .animateItem()
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
            pointerTextRes = Res.string.pointer_deck,
            valueText = name
        )

        Pointer(
            pointerTextRes = Res.string.pointer_card_quantity,
            valueText = cardQuantity.toString()
        )
    }
}

@Composable
private fun IpaKeyboard(
    ipaKeyboardState: IpaKeyboardState,
    onIpaTextFieldValueChange: (letterGroupIndex: Int, ipa: TextFieldValue) -> Unit,
    modifier: Modifier = Modifier
) {
    val onClick: (text: String) -> Unit = {
        val index = ipaKeyboardState.holderIndex
        val text = ipaKeyboardState.ipaTextFieldValue?.text

        if (index != null && text != null) {
            val selectionTextRangeEnd = ipaKeyboardState.ipaTextFieldValue.selection.end + it.length
            val newTextRange = TextRange(selectionTextRangeEnd, selectionTextRangeEnd)
            onIpaTextFieldValueChange(
                index,
                ipaKeyboardState.ipaTextFieldValue.copy(text = text + it, selection = newTextRange)
            )
        }
    }

    LazyRow(
        modifier = modifier
    ) {
        items(ipaKeyboardState.keys) {
            Text(
                text = it,
                modifier = Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .clickable { onClick(it) }
                    .background(Color(0x4a838383))
                    .padding(5.dp)
            )
            Spacer(modifier = Modifier.padding(4.dp))
        }
    }
}
