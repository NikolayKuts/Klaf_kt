package com.example.klaf.presentation.cardEditing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.domain.common.LoadingState
import com.example.domain.common.generateLetterInfos
import com.example.domain.common.updatedAt
import com.example.domain.ipa.LetterInfo
import com.example.domain.ipa.toRowInfos
import com.example.domain.ipa.toLetterInfos
import com.example.domain.ipa.toRowIpaItemHolders
import com.example.klaf.presentation.common.CardManagementView
import com.example.klaf.presentation.common.rememberAsMutableStateOf

@Composable
fun CardEditingScreen(viewModel: BaseCardEditingViewModel) {
    val deck by viewModel.deck.collectAsState(initial = null)
    val card by viewModel.card.collectAsState(initial = null)
    val autocompleteState by viewModel.autocompleteState.collectAsState()

    deck?.let { receivedDeck ->
        card?.let { receivedCard ->
            var letterInfosState by rememberAsMutableStateOf(value = receivedCard.toLetterInfos())
            var nativeWordState by rememberAsMutableStateOf(value = receivedCard.nativeWord)
            var foreignWordState by rememberAsMutableStateOf(value = receivedCard.foreignWord)
            var ipaHoldersState by rememberAsMutableStateOf(value = receivedCard.ipa)

            CardManagementView(
                deckName = receivedDeck.name,
                cardQuantity = receivedDeck.cardQuantity,
                letterInfos = letterInfosState,
                nativeWord = nativeWordState,
                foreignWord = foreignWordState,
                ipaHolders = ipaHoldersState,
                autocompleteState = autocompleteState,
                loadingState = LoadingState.Non,
                onDismissRequest = { viewModel.closeAutocompleteMenu() },
                onLetterClick = { index, letterInfo ->
                    val updatedIsChecked = when (letterInfo.letter) {
                        LetterInfo.EMPTY_LETTER -> false
                        else -> letterInfo.isNotChecked
                    }

                    letterInfosState = letterInfosState.updatedAt(
                        index = index,
                        newValue = letterInfo.copy(isChecked = updatedIsChecked)
                    )
                    ipaHoldersState = letterInfosState.toRowIpaItemHolders()
                },
                onNativeWordChange = { nativeWord -> nativeWordState = nativeWord },
                onForeignWordChange = { foreignWord ->
                    foreignWordState = foreignWord
                    letterInfosState = foreignWord.generateLetterInfos()
                    ipaHoldersState = emptyList()
                    viewModel.updateAutocompleteState(word = foreignWord)
                },
                onIpaChange = { letterGroupIndex: Int, ipa: String ->
                    ipaHoldersState =
                        ipaHoldersState.updatedAt(index = letterGroupIndex) { oldValue ->
                            oldValue.copy(ipa = ipa.trim())
                        }
                },
                onConfirmClick = {
                    viewModel.updateCard(
                        oldCard = receivedCard,
                        deckId = receivedDeck.id,
                        nativeWord = nativeWordState,
                        foreignWord = foreignWordState,
                        letterInfos = letterInfosState,
                        ipaHolders = ipaHoldersState,
                    )
                },
                onPronounceIconClick = { viewModel.pronounce() },
                onAutocompleteItemClick = { chosenWord ->
                    foreignWordState = chosenWord
                    letterInfosState = chosenWord.toRowInfos()
                    viewModel.setSelectedAutocomplete(selectedWord = chosenWord)
                }
            )
        }
    }
}