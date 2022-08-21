package com.example.klaf.presentation.cardEdinting

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.klaf.R
import com.example.klaf.domain.ipa.decodeIpa
import com.example.klaf.domain.ipa.decodeToInfos
import com.example.klaf.presentation.common.*
import com.example.klaf.presentation.theme.MainTheme

@Composable
fun CardEditingFragmentView(viewModel: CardEditingViewModel) {
    val deck by viewModel.deck.collectAsState(initial = null)
    val card by viewModel.card.collectAsState(initial = null)

    deck?.let { receivedDeck ->
        card?.let { receivedCard ->
            val letterInfosState = rememberAsMutableStateOf(
                value = receivedCard.decodeToInfos()
            )
            val nativeWordState = rememberAsMutableStateOf(value = receivedCard.nativeWord)
            val foreignWordState = rememberAsMutableStateOf(value = receivedCard.foreignWord)
            val ipaTemplateState = rememberAsMutableStateOf(value = receivedCard.decodeIpa())

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
                            DeckInfo(
                                name = receivedDeck.name,
                                cardQuantity = receivedDeck.cardQuantity
                            )
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
                                iconId = R.drawable.ic_comfirmation_24,
                                onClick = {
                                    viewModel.updateCard(
                                        oldCard = receivedCard,
                                        deckId = receivedDeck.id,
                                        nativeWord = nativeWordState.value,
                                        foreignWord = foreignWordState.value,
                                        letterInfos = letterInfosState.value,
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