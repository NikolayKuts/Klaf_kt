package com.kuts.klaf.presentation.cardManagement.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.kuts.domain.common.LoadingState
import com.kuts.domain.common.Wordable
import com.kuts.domain.common.skipOnNewLineCharacter
import com.kuts.domain.entities.AutocompleteWord
import com.kuts.domain.ipa.IpaHolder
import com.kuts.klaf.R
import com.kuts.klaf.presentation.cardManagement.cardAddition.AutocompleteState
import com.kuts.klaf.presentation.cardManagement.cardAddition.NativeWordSuggestionsState
import com.kuts.klaf.presentation.common.ConfirmationButton
import com.kuts.klaf.presentation.common.CustomCheckBox
import com.kuts.klaf.presentation.common.ROUNDED_ELEMENT_SIZE
import com.kuts.klaf.presentation.common.RoundButton
import com.kuts.klaf.presentation.common.noRippleClickable
import com.kuts.klaf.presentation.common.rememberAsMutableStateOf
import com.kuts.klaf.presentation.common.verticalScrollbar
import com.kuts.klaf.presentation.theme.MainTheme

private const val CARD_MANAGEMENT_CONTAINER_WIDTH = 500

@Composable
fun CardManagementFields(
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
    nativeWordSuggestionsState: NativeWordSuggestionsState,
    onNativeWordSuggestionClick: (wordIndex: Int) -> Unit,
    onNativeWordFieldArrowIconClick: () -> Unit,
    onConfirmSuggestionsSelection: () -> Unit,
    onClearSelectionClick: () -> Unit,
) {
    Column(
        modifier = modifier.width(CARD_MANAGEMENT_CONTAINER_WIDTH.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DropDownAutocompleteFiled(
            expanded = autocompleteState.isActive && autocompleteState.autocomplete.isNotEmpty(),
            typedWord = foreignWord,
            autocompleteState = autocompleteState,
            loadingState = loadingState,
            onTypedWordChange = onForeignWordChange,
            onPronounceIconClick = onPronounceIconClick,
            onAutocompleteItemClick = { autoCompleteWord, _ ->
                onAutocompleteItemClick(autoCompleteWord.word())
            },
        )

        val isNativeWordSuggestionsStateActiveAndNotEmpty = nativeWordSuggestionsState.isActive
                && nativeWordSuggestionsState.suggestions.isNotEmpty()

        DropDownNativeWordFiled(
            expanded = isNativeWordSuggestionsStateActiveAndNotEmpty,
            typedWord = nativeWord,
            nativeWordSuggestionsState = nativeWordSuggestionsState,
            loadingState = nativeWordSuggestionsState.loadingState,
            onTypedWordChange = onNativeWordChange,
            onArrowIconClick = onNativeWordFieldArrowIconClick,
            onSuggestionClick = onNativeWordSuggestionClick,
            onTextFiledClick = onNativeWordFieldClick,
            onConfirmSuggestionsSelection = onConfirmSuggestionsSelection,
            onClearSelectionClick = onClearSelectionClick,
        )

        IpaSection(
            ipaHolders = ipaHolders,
            onIpaChange = onIpaChange,
            confirmationButtonSection = confirmationButtonSection
        )
    }
}

@Composable
private fun DropDownAutocompleteFiled(
    expanded: Boolean,
    typedWord: String,
    autocompleteState: AutocompleteState,
    loadingState: LoadingState<Unit>,
    onTypedWordChange: (String) -> Unit,
    onPronounceIconClick: () -> Unit,
    onAutocompleteItemClick: (chosenWord: AutocompleteWord, choseIndex: Int) -> Unit,
) {
    DropDownWordFiled(
        expanded = expanded,
        typedWord = typedWord,
        dropdownContent = autocompleteState.autocomplete,
        labelResId = R.string.label_foreign_word,
        textColor = MainTheme.colors.cardManagementView.foreignWord,
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
        },
        onTypedWordChange = onTypedWordChange,
        itemContent = { wordable, wordableIndex, onItemSizeChange ->
            AutocompleteWordItem(
                modifier = Modifier.onSizeChanged { intSize -> onItemSizeChange(intSize) },
                word = wordable,
                prefix = autocompleteState.prefix,
                onAutocompleteItemClick = { onAutocompleteItemClick(wordable, wordableIndex) }
            )
        },
        onTextFiledClick = {}
    )
}

@Composable
fun DropDownNativeWordFiled(
    expanded: Boolean,
    typedWord: String,
    nativeWordSuggestionsState: NativeWordSuggestionsState,
    loadingState: LoadingState<Unit>,
    onTypedWordChange: (String) -> Unit,
    onArrowIconClick: () -> Unit,
    onSuggestionClick: (wordIndex: Int) -> Unit,
    onTextFiledClick: () -> Unit,
    onConfirmSuggestionsSelection: () -> Unit,
    onClearSelectionClick: () -> Unit,
) {
    DropDownWordFiled(
        expanded = expanded,
        typedWord = typedWord,
        dropdownContent = nativeWordSuggestionsState.suggestions.map { wordSuggestion ->
            Wordable { wordSuggestion.word }
        },
        textColor = MainTheme.colors.cardManagementView.nativeWord,
        labelResId = R.string.label_native_word,
        trailingIcon = {
            val (iconColor: Color, clickable: Boolean) = when {
                loadingState is LoadingState.Success
                        && nativeWordSuggestionsState.suggestions.isNotEmpty() -> {
                    MainTheme.colors.cardManagementView.activePronunciationIcon to true
                }

                else -> MainTheme.colors.cardManagementView.inactivePronunciationIcon to false
            }

            if (loadingState == LoadingState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(35.dp))
            }
            val rotationDegree by animateFloatAsState(if (expanded) 90f else 0f, label = "")

            Icon(
                modifier = Modifier
                    .clip(shape = RoundedCornerShape(50.dp))
                    .clickable(enabled = clickable) { onArrowIconClick() }
                    .padding(5.dp)
                    .rotate(rotationDegree),
                painter = painterResource(id = R.drawable.ic_arrow_forward),
                contentDescription = null,
                tint = iconColor,
            )
        },
        onTypedWordChange = onTypedWordChange,
        onTextFiledClick = onTextFiledClick,
        itemContent = { wordable, wordableIndex, onItemSizeChange ->
            Row(
                modifier = Modifier.noRippleClickable { onSuggestionClick(wordableIndex) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier
                        .weight(1f)
                        .onSizeChanged { intSize -> onItemSizeChange(intSize) },
                    text = wordable.word(),
                    fontStyle = FontStyle.Italic
                )

                CustomCheckBox(
                    modifier = Modifier.padding(6.dp),
                    checked = nativeWordSuggestionsState.suggestions[wordableIndex].isSelected,
                    onCheckedChange = { onSuggestionClick(wordableIndex) },
                    uncheckedBorderColor = MainTheme.colors.cardTransferringScreen.unCheckedBorder,
                    checkedBoxColor = MainTheme.colors.cardTransferringScreen.selectedCheckBox
                )
            }
            Divider()
        },
        dropdownMenuContent = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                RoundButton(
                    background = MainTheme.colors.common.negativeDialogButton,
                    iconId = R.drawable.ic_list_clear,
                    onClick = onClearSelectionClick
                )
                ConfirmationButton() {
                    onConfirmSuggestionsSelection()
                }
            }
        }
    )
}

@Composable
private fun  AutocompleteWordItem(
    word: AutocompleteWord,
    prefix: String,
    onAutocompleteItemClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onAutocompleteItemClick() },
        text = buildAnnotatedString {
            withStyle(
                style = MainTheme.typographies
                    .cardManagementViewTextStyles
                    .foreignWordAutocompleteSpanStyle
            ) {
                append(prefix)
            }

            append(word.word().drop(prefix.length))
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