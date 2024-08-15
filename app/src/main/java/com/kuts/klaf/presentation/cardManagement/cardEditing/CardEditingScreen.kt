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
import com.kuts.klaf.R
import com.kuts.klaf.presentation.cardManagement.common.CardManagementView
import com.kuts.klaf.presentation.cardManagement.common.MAX_FOREIGN_WORD_LENGTH
import com.kuts.klaf.presentation.cardManagement.common.MAX_IPA_LENGTH
import com.kuts.klaf.presentation.cardManagement.common.MAX_NATIVE_WORD_LENGTH
import com.kuts.klaf.presentation.common.EventMessage
import com.kuts.klaf.presentation.common.rememberAsMutableStateOf

@Composable
fun CardEditingScreen(viewModel: BaseCardEditingViewModel) {
    val deck by viewModel.deck.collectAsState(initial = null)
    val card by viewModel.card.collectAsState(initial = null)
    val autocompleteState by viewModel.autocompleteState.collectAsState()
    val pronunciationLoadingState by viewModel.pronunciationLoadingState.collectAsState()
    val nativeWordSuggestionsState by viewModel.nativeWordSuggestionsState.collectAsState()

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
                closeAutocompletePopupMenu = { viewModel.closeAutocompleteMenu() },
                nativeWordSuggestionsState = nativeWordSuggestionsState,
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
                    if (nativeWord.length < MAX_NATIVE_WORD_LENGTH) {
                        nativeWordState = nativeWord
                    } else {
                        viewModel.sendEventMessage(
                            message = EventMessage(
                                resId = R.string.native_word_is_too_long,
                                type = EventMessage.Type.Negative,
                                duration = EventMessage.Duration.Long
                            )
                        )
                    }
                },
                onForeignWordChange = { foreignWord ->
                    if (foreignWord.length < MAX_FOREIGN_WORD_LENGTH) {
                        foreignWordState = foreignWord
                        letterInfosState = foreignWord.generateLetterInfos()
                        ipaHoldersState = emptyList()
                        nativeWordState = ""
                        viewModel.updateEditingState(word = foreignWord)
                    } else {
                        viewModel.sendEventMessage(
                            message = EventMessage(
                                resId = R.string.foreign_word_is_too_long,
                                type = EventMessage.Type.Negative,
                                duration = EventMessage.Duration.Long
                            )
                        )
                    }
                },
                onIpaChange = { letterGroupIndex: Int, ipa: String ->
                    if (ipa.length < MAX_IPA_LENGTH) {
                        ipaHoldersState =
                            ipaHoldersState.updatedAt(index = letterGroupIndex) { oldValue ->
                                oldValue.copy(ipa = ipa)
                            }
                    } else {
                        viewModel.sendEventMessage(
                            message = EventMessage(
                                resId = R.string.ipa_is_too_long,
                                type = EventMessage.Type.Negative,
                                duration = EventMessage.Duration.Long
                            )
                        )
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
                },
                closeNativeWordSuggestionsPopupMenu = {
                    viewModel.manageNativeWordSuggestionsState(
                        state = nativeWordSuggestionsState.copy(isActive = false)
                    )
                },
                onNativeWordSuggestionItemClick = { chosenWordIndex ->
                    val updatedSuggestions = nativeWordSuggestionsState.suggestions
                        .updatedAt(index = chosenWordIndex) { oldValue ->
                            oldValue.copy(isSelected = !oldValue.isSelected)
                        }

                    viewModel.manageNativeWordSuggestionsState(
                        state = nativeWordSuggestionsState.copy(suggestions = updatedSuggestions)
                    )
                },
                onNativeWordFieldArrowIconClick = {
                    val invertedSuggestionsMenuState = nativeWordSuggestionsState.isActive.not()

                    viewModel.manageNativeWordSuggestionsState(
                        state = nativeWordSuggestionsState.copy(isActive = invertedSuggestionsMenuState)
                    )
                },
                onConfirmSuggestionsSelection = {
                    viewModel.manageNativeWordSuggestionsState(
                        state = nativeWordSuggestionsState.copy(isActive = false)
                    )
                    val selectedNativeWordSuggestions = nativeWordSuggestionsState.suggestions
                        .filter { suggestionItem -> suggestionItem.isSelected }
                        .joinToString(separator = ", ") { suggestionItem -> suggestionItem.word }

                    if (selectedNativeWordSuggestions.length <= MAX_NATIVE_WORD_LENGTH) {
                        nativeWordState = selectedNativeWordSuggestions
                    } else {
                        viewModel.sendEventMessage(
                            message = EventMessage(
                                resId = R.string.native_word_is_too_long,
                                type = EventMessage.Type.Negative,
                                duration = EventMessage.Duration.Long
                            )
                        )
                    }
                },
                onClearNativeWordSuggestionsSelectionClick = {
                    val updatedSuggestions = nativeWordSuggestionsState.suggestions.map { suggestionItem ->
                        suggestionItem.copy(isSelected = false)
                    }

                    viewModel.manageNativeWordSuggestionsState(
                        state = nativeWordSuggestionsState.copy(
                            suggestions = updatedSuggestions,
                            isActive = false
                        )
                    )

                    nativeWordState = ""
                }
            )
        }
    }
}