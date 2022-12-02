package com.example.klaf.presentation.common

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.klaf.R
import com.example.klaf.domain.common.generateLetterInfos
import com.example.klaf.domain.common.ifTrue
import com.example.klaf.domain.ipa.LetterInfo
import com.example.klaf.domain.ipa.convertToUncompletedIpa
import com.example.klaf.presentation.theme.MainTheme

@Composable
fun ColumnScope.ForeignWordLazyRow(
    letterInfosState: MutableState<List<LetterInfo>>,
    ipaTemplateState: MutableState<String>,
) {
    LazyRow(
        modifier = Modifier
            .weight(0.5F)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        itemsIndexed(items = letterInfosState.value) { index, letterInfo ->
            LetterItem(
                letterInfo = letterInfo,
                onClick = {
                    val updatedIsChecked = if (letterInfo.letter == " ") {
                        false
                    } else {
                        !letterInfo.isChecked
                    }

                    letterInfosState.value = letterInfosState.value
                        .toMutableList()
                        .apply {
                            this[index] = letterInfo.copy(
                                isChecked = updatedIsChecked
                            )
                        }

                    ipaTemplateState.value =
                        letterInfosState.value.convertToUncompletedIpa()
                }
            )
        }
    }
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
fun LetterItem(
    letterInfo: LetterInfo,
    onClick: () -> Unit,
) {
    val cellColor = if (letterInfo.isChecked) {
        MainTheme.colors.checkedLetterCell
    } else {
        MainTheme.colors.uncheckedLetterCell
    }

    Text(
        modifier = Modifier
            .padding(4.dp)
            .clip(shape = RoundedCornerShape(4.dp))
            .background(cellColor)
            .padding(4.dp)
            .clickable { onClick() },
        text = letterInfo.letter,
        fontSize = if (letterInfo.isChecked) 30.sp else 24.sp,
        style = MainTheme.typographies.dialogTextStyle.copy()
    )
}

@Composable
fun BoxScope.CardFields(
    letterInfosState: MutableState<List<LetterInfo>>,
    nativeWordState: MutableState<String>,
    foreignWordState: MutableState<String>,
    ipaTemplate: MutableState<String>,
) {
    Column(
        modifier = Modifier.align(alignment = Alignment.TopCenter),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        WordTextField(
            value = nativeWordState.value,
            labelTextId = R.string.label_native_word,
            textColor = MainTheme.colors.cardNativeWord,
            onValueChange = { nativeWordState.value = it },
        )
        WordTextField(
            value = foreignWordState.value,
            labelTextId = R.string.label_foreign_word,
            textColor = MainTheme.colors.cardForeignWord,
            onValueChange = {
                foreignWordState.value = it
                letterInfosState.value = foreignWordState.value.generateLetterInfos()
                ipaTemplate.value = letterInfosState.value.convertToUncompletedIpa()
            },
        )
        WordTextField(
            value = ipaTemplate.value,
            labelTextId = R.string.label_ipa,
            textColor = MainTheme.colors.cardIpa,
            onValueChange = { ipaTemplate.value = it }
        )
    }
}

@Composable
private fun WordTextField(
    value: String,
    @StringRes labelTextId: Int,
    textColor: Color,
    onValueChange: (String) -> Unit,
) {

    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = stringResource(id = labelTextId)) },
        colors = TextFieldDefaults.textFieldColors(
            backgroundColor = MainTheme.colors.cardTextFieldBackground,
            textColor = textColor
        ),
    )
}

@Composable
fun DialogBox(
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = Modifier
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
    buttonContent: @Composable RowScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onBackgroundClick,
            ),
    ) {
        Box(modifier = Modifier.align(Alignment.Center)) {
            Card(
                modifier = Modifier
                    .defaultMinSize(minHeight = 150.dp, minWidth = 300.dp)
                    .padding(bottom = (DIALOG_BUTTON_SIZE / 2).dp)
            ) {
                Box(
                    modifier = Modifier
                        .padding(MainTheme.dimensions.dialogContentPadding)
                        .padding(bottom = (DIALOG_BUTTON_SIZE / 4).dp),
                    content = mainContent
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth(0.5F)
                    .align(alignment = Alignment.BottomCenter),
                horizontalArrangement = Arrangement.SpaceAround,
                content = buttonContent
            )
        }
    }
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