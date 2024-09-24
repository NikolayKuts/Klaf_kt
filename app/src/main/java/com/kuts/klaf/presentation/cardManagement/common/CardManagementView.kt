package com.kuts.klaf.presentation.cardManagement.common

import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kuts.domain.common.LoadingState
import com.kuts.domain.ipa.LetterInfo
import com.kuts.klaf.R
import com.kuts.klaf.presentation.cardManagement.cardAddition.AutocompleteState
import com.kuts.klaf.presentation.cardManagement.cardAddition.NativeWordSuggestionsState
import com.kuts.klaf.presentation.common.Pointer
import com.kuts.klaf.presentation.common.RoundButton
import com.kuts.klaf.presentation.common.ScrollableBox
import com.kuts.klaf.presentation.common.noRippleClickable
import com.kuts.klaf.presentation.theme.MainTheme

@Composable
fun CardManagementView(
    deckName: String,
    cardQuantity: Int,
    letterInfos: List<LetterInfo>,
    foreignWordFieldValue: TextFieldValue,
    nativeWordFieldValue: TextFieldValue,
    textFieldValueIpaHolders: List<TextFieldValueIpaHolder>,
    autocompleteState: AutocompleteState,
    pronunciationLoadingState: LoadingState<Unit>,
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