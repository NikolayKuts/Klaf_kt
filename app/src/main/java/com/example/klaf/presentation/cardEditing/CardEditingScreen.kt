package com.example.klaf.presentation.cardEditing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.klaf.domain.common.generateLetterInfos
import com.example.klaf.domain.common.updatedAt
import com.example.klaf.domain.ipa.LetterInfo
import com.example.klaf.domain.ipa.convertToUncompletedIpa
import com.example.klaf.domain.ipa.decodeToCompletedIpa
import com.example.klaf.domain.ipa.decodeToInfos
import com.example.klaf.presentation.common.CardManagementView
import com.example.klaf.presentation.common.rememberAsMutableStateOf

@Composable
fun CardEditingScreen(viewModel: BaseCardEditingViewModel) {
    val deck by viewModel.deck.collectAsState(initial = null)
    val card by viewModel.card.collectAsState(initial = null)
    val autocompleteState by viewModel.autocompleteState.collectAsState()

    deck?.let { receivedDeck ->
        card?.let { receivedCard ->
            var letterInfosState by rememberAsMutableStateOf(
                value = receivedCard.decodeToInfos()
            )
            var nativeWordState by rememberAsMutableStateOf(value = receivedCard.nativeWord)
            var foreignWordState by rememberAsMutableStateOf(value = receivedCard.foreignWord)
            var ipaTemplateState
                    by rememberAsMutableStateOf(value = receivedCard.decodeToCompletedIpa())

            CardManagementView(
                deckName = receivedDeck.name,
                cardQuantity = receivedDeck.cardQuantity,
                letterInfos = letterInfosState,
                nativeWord = nativeWordState,
                foreignWord = foreignWordState,
                ipaTemplate = ipaTemplateState,
                autocompleteState = autocompleteState,
                onDismissRequest = { viewModel.closeAutocompleteMenu() },
                onLetterClick = { index, letterInfo ->
                    val updatedIsChecked = when (letterInfo.letter) {
                        LetterInfo.EMPTY_LETTER -> false
                        else -> !letterInfo.isChecked
                    }

                    letterInfosState = letterInfosState.updatedAt(
                        index = index,
                        updateValue = letterInfo.copy(isChecked = updatedIsChecked)
                    )
                    ipaTemplateState = letterInfosState.convertToUncompletedIpa()
                },
                onNativeWordChange = { nativeWord -> nativeWordState = nativeWord },
                onForeignWordChange = { foreignWord ->
                    foreignWordState = foreignWord
                    letterInfosState = foreignWord.generateLetterInfos()
                    ipaTemplateState = letterInfosState.convertToUncompletedIpa()
                    viewModel.updateAutocompleteState(word = foreignWord)
                },
                onIpaChange = { ipaTemplate -> ipaTemplateState = ipaTemplate },
                onConfirmClick = {
                    viewModel.updateCard(
                        oldCard = receivedCard,
                        deckId = receivedDeck.id,
                        nativeWord = nativeWordState,
                        foreignWord = foreignWordState,
                        letterInfos = letterInfosState,
                        ipaTemplate = ipaTemplateState,
                    )
                },
                onPronounceIconClick = { viewModel.pronounce() },
                onAutocompleteItemClick = { chosenWord ->
                    foreignWordState = chosenWord
                    viewModel.setSelectedAutocomplete(selectedWord = chosenWord)
                }
            )
        }
    }
}