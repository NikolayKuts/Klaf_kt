package com.example.klaf.domain.common

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.klaf.R
import com.example.klaf.domain.ipa.LetterInfo
import com.example.klaf.domain.ipa.convertUncompletedIpa
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
                        letterInfosState.value.convertUncompletedIpa()
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
private fun Pointer(
    pointerTextId: Int,
    valueText: String,
) {
    Text(
        text = buildAnnotatedString {
            withStyle(style = MainTheme.typographies.cardPointer) {
                append("${stringResource(id = pointerTextId)}: ")
            }

            withStyle(style = MainTheme.typographies.cardAdditionPinterValue) {
                append(valueText)
            }
        }
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
                ipaTemplate.value = letterInfosState.value.convertUncompletedIpa()
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