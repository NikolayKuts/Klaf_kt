package com.example.klaf.presentation.cardAddition

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.klaf.presentation.cardAddition.CardAdditionEvent.*
import com.example.klaf.presentation.common.CardManagementView

@Composable
fun CardAdditionScreen(viewModel: CardAdditionViewModel) {
    val deck = viewModel.deck.collectAsState(initial = null)
    val cardState by viewModel.cardAdditionState.collectAsState()
    val letterInfos = cardState.letterInfos
    val foreignWord = cardState.foreignWord
    val nativeWord = cardState.nativeWord
    val ipaTemplate = cardState.ipaTemplate

    deck.value?.let { receivedDeck ->
        CardManagementView(
            deckName = receivedDeck.name,
            cardQuantity = receivedDeck.cardQuantity,
            letterInfos = letterInfos,
            nativeWord = nativeWord,
            foreignWord = foreignWord,
            ipaTemplate = ipaTemplate,
            onLetterClick = { index, letterInfo ->
                viewModel.sendEvent(
                    event = ChangeLetterSelectionWithIpaTemplate(
                        index = index,
                        letterInfo = letterInfo
                    )
                )
            },
            onNativeWordChange = { word ->
                viewModel.sendEvent(event = UpdateNativeWord(word = word))
            },
            onForeignWordChange = { word ->
                viewModel.sendEvent(event = UpdateForeignWordWithIpaTemplate(word = word))
            },
            onIpaChange = { ipa ->
                viewModel.sendEvent(event = UpdateIpaTemplate(ipa = ipa))
            },
            onConfirmClick = {
                viewModel.sendEvent(
                    event = AddNewCard(
                        deckId = receivedDeck.id,
                        nativeWord = nativeWord,
                        foreignWord = foreignWord,
                        letterInfos = letterInfos,
                        ipaTemplate = ipaTemplate
                    )
                )
            }
        )
    }
}