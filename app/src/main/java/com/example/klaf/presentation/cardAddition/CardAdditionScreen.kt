package com.example.klaf.presentation.cardAddition

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.domain.ipa.LetterInfo
import com.example.klaf.R
import com.example.klaf.presentation.cardAddition.CardAdditionEvent.*
import com.example.klaf.presentation.common.*
import com.example.klaf.presentation.theme.MainTheme

@Composable
fun CardAdditionScreen(viewModel: BaseCardAdditionViewModel) {
    val deck = viewModel.deck.collectAsState(initial = null)
    val cardState by viewModel.cardAdditionState.collectAsState()
    val letterInfos = cardState.letterInfos
    val foreignWord = cardState.foreignWord
    val nativeWord = cardState.nativeWord
    val ipaHolders = cardState.ipaHolders
    val autocompleteState by viewModel.autocompleteState.collectAsState()

    deck.value?.let { receivedDeck ->
        CardManagementView(
            deckName = receivedDeck.name,
            cardQuantity = receivedDeck.cardQuantity,
            letterInfos = letterInfos,
            nativeWord = nativeWord,
            foreignWord = foreignWord,
            ipaHolders = ipaHolders,
            autocompleteState = autocompleteState,
            onDismissRequest = { viewModel.sendEvent(event = CloseAutocompleteMenu) },
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
                viewModel.sendEvent(event = UpdateDataOnForeignWordChanged(word = word))
            },
            onIpaChange = { letterGroupIndex, ipa ->
                viewModel.sendEvent(
                    event = UpdateIpa(letterGroupIndex = letterGroupIndex, ipa = ipa)
                )
            },
            onConfirmClick = {
                viewModel.sendEvent(
                    event = AddNewCard(
                        deckId = receivedDeck.id,
                        nativeWord = nativeWord,
                        foreignWord = foreignWord,
                        letterInfos = letterInfos,
                        ipaHolders = ipaHolders,
                    )
                )
            },
            onPronounceIconClick = { viewModel.sendEvent(event = PronounceForeignWord) },
            onAutocompleteItemClick = { autocompleteWord ->
                viewModel.sendEvent(event = UpdateDataOnAutocompleteSelected(word = autocompleteWord))
            }
        )
    }
}