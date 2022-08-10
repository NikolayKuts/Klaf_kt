package com.example.klaf.presentation.cardEdinting

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.klaf.R
import com.example.klaf.domain.common.generateLetterInfos
import com.example.klaf.domain.ipa.IpaProcessor
import com.example.klaf.domain.ipa.LetterInfo
import com.example.klaf.presentation.common.RoundButton
import com.example.klaf.presentation.common.rememberAsMutableStateOf
import com.example.klaf.presentation.theme.MainTheme

@Composable
fun CardEditingFragmentView(viewModel: CardEditingViewModel) {
    val deck by viewModel.deck.collectAsState(initial = null)
    val card by viewModel.card.collectAsState(initial = null)

    deck?.let { receivedDeck ->
        card?.let { receivedCard ->
            val letterInfos = rememberAsMutableStateOf(
                value = IpaProcessor.getLetterInfos(encodedIpaFromDB = receivedCard.ipa)
            )
            val nativeWordState = rememberAsMutableStateOf(value = receivedCard.nativeWord)
            val foreignWordState = rememberAsMutableStateOf(value = receivedCard.foreignWord)
            val ipaTemplateState = rememberAsMutableStateOf(
                value = IpaProcessor.getDecodedIpa(encodedIpa = receivedCard.ipa)
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight(0.3F)
                            .fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize(),
                        ) {
                            DeckInfo(name = receivedDeck.name,
                                cardQuantity = receivedDeck.cardQuantity)
                            LazyRow(
                                modifier = Modifier
                                    .weight(0.5F)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                itemsIndexed(items = letterInfos.value) { index, letterInfo ->
                                    LetterItem(
                                        letterInfo = letterInfo,
                                        onClick = {
                                            val updatedIsChecked = if (letterInfo.letter == " ") {
                                                false
                                            } else {
                                                !letterInfo.isChecked
                                            }

                                            letterInfos.value = letterInfos.value
                                                .toMutableList()
                                                .apply {
                                                    this[index] = letterInfo.copy(
                                                        isChecked = updatedIsChecked
                                                    )
                                                }

                                            ipaTemplateState.value =
                                                IpaProcessor.getUncompletedIpa(letterInfos.value)
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Box(modifier = Modifier.fillMaxSize()) {
                        CardFields(
                            letterInfosState = letterInfos,
                            nativeWordState = nativeWordState,
                            foreignWordState = foreignWordState,
                            ipaTemplate = ipaTemplateState,
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 32.dp, bottom = 32.dp)
                        ) {
                            RoundButton(
                                background = MainTheme.colors.positiveDialogButton,
                                iconId = R.drawable.ic_comfirmation_24,
                                onClick = {
                                    viewModel.updateCard(
                                        oldCard = receivedCard,
                                        deckId = receivedDeck.id,
                                        nativeWord = nativeWordState.value,
                                        foreignWord = foreignWordState.value,
                                        letterInfos = letterInfos.value,
                                        ipaTemplate = ipaTemplateState.value,
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.DeckInfo(name: String, cardQuantity: Int) {
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
            withStyle(style = MainTheme.typographies.cardAdditionPointer) {
                append("${stringResource(id = pointerTextId)}: ")
            }

            withStyle(style = MainTheme.typographies.cardAdditionPinterValue) {
                append(valueText)
            }
        }
    )
}

@Composable
private fun LetterItem(
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
private fun BoxScope.CardFields(
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
            onValueChange = { nativeWordState.value = it },
        )
        WordTextField(
            value = foreignWordState.value,
            labelTextId = R.string.label_foreign_word,
            onValueChange = {
                foreignWordState.value = it
                letterInfosState.value = foreignWordState.value.generateLetterInfos()
                ipaTemplate.value = IpaProcessor.getUncompletedIpa(letterInfosState.value)
            },
        )
        WordTextField(
            value = ipaTemplate.value,
            labelTextId = R.string.label_ipa,
            onValueChange = { ipaTemplate.value = it }
        )
    }
}

@Composable
private fun WordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    @StringRes labelTextId: Int,
) {

    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = stringResource(id = labelTextId)) },
        colors = TextFieldDefaults.textFieldColors(
            backgroundColor = MainTheme.colors.cardAdditionTextField
        ),
    )
}