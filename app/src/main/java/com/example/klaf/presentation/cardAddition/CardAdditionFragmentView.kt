package com.example.klaf.presentation.cardAddition

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.klaf.R
import com.example.klaf.domain.common.generateLetterInfos
import com.example.klaf.presentation.common.*
import com.example.klaf.presentation.theme.MainTheme

@Composable
fun CardAdditionFragmentView(viewModel: CardAdditionViewModel, smartSelectedWord: String? = null) {
    val deck = viewModel.deck.collectAsState(initial = null)

    deck.value?.let { receivedDeck ->
        val foreignWord = smartSelectedWord ?: ""
        val letterInfosState = rememberAsMutableStateOf(value = foreignWord.generateLetterInfos())
        val nativeWordState = rememberAsMutableStateOf(value = "")
        val foreignWordState = rememberAsMutableStateOf(value = foreignWord)
        val ipaTemplateState = rememberAsMutableStateOf(value = "")

        val additionState = viewModel.cardAdditionState.collectAsState()

        when (additionState.value) {
            CardAdditionState.NOT_ADDED -> {}
            CardAdditionState.ADDED -> {
                letterInfosState.value = emptyList()
                nativeWordState.value = ""
                foreignWordState.value = ""
                ipaTemplateState.value = ""

                viewModel.resetCardAdditionState()
            }
        }

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
                        DeckInfo(name = receivedDeck.name, cardQuantity = receivedDeck.cardQuantity)
                        ForeignWordLazyRow(
                            letterInfosState = letterInfosState,
                            ipaTemplateState = ipaTemplateState
                        )
                    }
                }
                Box(modifier = Modifier.fillMaxSize()) {
                    CardFields(
                        letterInfosState = letterInfosState,
                        nativeWordState = nativeWordState,
                        foreignWordState = foreignWordState,
                        ipaTemplate = ipaTemplateState,
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 16.dp)
                    ) {
                        RoundButton(
                            background = MainTheme.colors.positiveDialogButton,
                            iconId = R.drawable.ic_confirmation_24,
                            onClick = {
                                viewModel.addNewCard(
                                    deckId = receivedDeck.id,
                                    nativeWord = nativeWordState.value,
                                    foreignWord = foreignWordState.value,
                                    letterInfos = letterInfosState.value,
                                    ipaTemplate = ipaTemplateState.value
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}