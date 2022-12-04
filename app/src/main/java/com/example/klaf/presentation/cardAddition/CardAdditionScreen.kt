package com.example.klaf.presentation.cardAddition

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.klaf.R
import com.example.klaf.domain.common.generateLetterInfos
import com.example.klaf.domain.ipa.convertToUncompletedIpa
import com.example.klaf.presentation.common.*
import com.example.klaf.presentation.theme.MainTheme

@Composable
fun CardAdditionScreen(viewModel: CardAdditionViewModel, smartSelectedWord: String? = null) {
    val deck = viewModel.deck.collectAsState(initial = null)

    deck.value?.let { receivedDeck ->
        val foreignWord = smartSelectedWord ?: ""
        var letterInfosState by rememberAsMutableStateOf(value = foreignWord.generateLetterInfos())
        var nativeWordState by rememberAsMutableStateOf(value = "")
        var foreignWordState by rememberAsMutableStateOf(value = foreignWord)
        var ipaTemplateState by rememberAsMutableStateOf(value = "")

        val additionState = viewModel.cardAdditionState.collectAsState()

        when (additionState.value) {
            CardAdditionState.NOT_ADDED -> {}
            CardAdditionState.ADDED -> {
                letterInfosState = emptyList()
                nativeWordState = ""
                foreignWordState = ""
                ipaTemplateState = ""

                viewModel.resetCardAdditionState()
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.3F),
            ) {
                DeckInfo(name = receivedDeck.name, cardQuantity = receivedDeck.cardQuantity)
                ForeignWordSelector(
                    letterInfos = letterInfosState,
                    onLetterClick = { index, letterInfo ->
                        val updatedIsChecked = when (letterInfo.letter) {
                            " " -> false
                            else -> !letterInfo.isChecked
                        }

                        letterInfosState = letterInfosState.toMutableList()
                            .apply { this[index] = letterInfo.copy(isChecked = updatedIsChecked) }

                        ipaTemplateState = letterInfosState.convertToUncompletedIpa()
                    }
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                CardFields(
                    modifier = Modifier.align(alignment = Alignment.TopCenter),
                    nativeWord = nativeWordState,
                    foreignWord = foreignWordState,
                    ipaTemplate = ipaTemplateState,
                    onNativeWordChange = { nativeWordState = it },
                    onForeignWordChange = {
                        foreignWordState = it
                        letterInfosState = foreignWordState.generateLetterInfos()
                        ipaTemplateState = letterInfosState.convertToUncompletedIpa()
                    },
                    onIpaChange = { ipaTemplateState = it },
                )
                RoundButton(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 16.dp),
                    background = MainTheme.colors.positiveDialogButton,
                    iconId = R.drawable.ic_confirmation_24,
                    onClick = {
                        viewModel.addNewCard(
                            deckId = receivedDeck.id,
                            nativeWord = nativeWordState,
                            foreignWord = foreignWordState,
                            letterInfos = letterInfosState,
                            ipaTemplate = ipaTemplateState
                        )
                    }
                )
            }
        }
    }
}