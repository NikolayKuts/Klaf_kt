package com.kuts.klaf.presentation.cardManagement.cardEditing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.kuts.domain.common.generateLetterInfos
import com.kuts.domain.common.updatedAt
import com.kuts.domain.ipa.LetterInfo
import com.kuts.domain.ipa.toLetterInfos
import com.kuts.domain.ipa.toRowInfos
import com.kuts.domain.ipa.toRowIpaItemHolders
import com.kuts.klaf.presentation.cardManagement.common.MAX_IPA_LENGTH
import com.kuts.klaf.presentation.cardManagement.common.MAX_WORD_LENGTH
import com.kuts.klaf.presentation.cardManagement.common.CardManagementView
import com.kuts.klaf.presentation.common.rememberAsMutableStateOf

@Composable
fun CardEditingScreen(viewModel: BaseCardEditingViewModel) {
    val deck by viewModel.deck.collectAsState(initial = null)
    val card by viewModel.card.collectAsState(initial = null)
    val autocompleteState by viewModel.autocompleteState.collectAsState()
    val pronunciationLoadingState by viewModel.pronunciationLoadingState.collectAsState()

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
                pronunciationLoadingState = pronunciationLoadingState,
                onCloseAutocompletePopupMenuClick = { viewModel.closeAutocompleteMenu() },
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
                onNativeWordChange = { nativeWord ->
                    if (nativeWord.length < MAX_WORD_LENGTH) {
                        nativeWordState = nativeWord
                    }
                },
                onForeignWordChange = { foreignWord ->
                    if (foreignWord.length < MAX_WORD_LENGTH) {
                        foreignWordState = foreignWord
                        letterInfosState = foreignWord.generateLetterInfos()
                        ipaHoldersState = emptyList()
                        viewModel.updateAutocompleteState(word = foreignWord)
                    }
                },
                onIpaChange = { letterGroupIndex: Int, ipa: String ->
                    if (ipa.length < MAX_IPA_LENGTH) {
                        ipaHoldersState =
                            ipaHoldersState.updatedAt(index = letterGroupIndex) { oldValue ->
                                oldValue.copy(ipa = ipa)
                            }
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