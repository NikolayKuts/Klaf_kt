package com.kuts.klaf.presentation.cardManagement.cardAddition

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.kuts.klaf.presentation.cardManagement.cardAddition.CardAdditionEvent.*
import com.kuts.klaf.presentation.cardManagement.common.CardManagementView

@Composable
fun CardAdditionScreen(viewModel: BaseCardAdditionViewModel) {
    val deck = viewModel.deck.collectAsState(initial = null)
    val cardState by viewModel.cardAdditionState.collectAsState()
    val letterInfos = cardState.letterInfos
    val foreignWord = cardState.foreignWord
    val nativeWord = cardState.nativeWord
    val ipaHolders = cardState.ipaHolders
    val autocompleteState by viewModel.autocompleteState.collectAsState()
    val loadingState by viewModel.pronunciationLoadingState.collectAsState()
    val nativeWordSuggestions by viewModel.nativeWordSuggestionsState.collectAsState()

    deck.value?.let { receivedDeck ->
        CardManagementView(
            deckName = receivedDeck.name,
            cardQuantity = receivedDeck.cardQuantity,
            letterInfos = letterInfos,
            nativeWord = nativeWord,
            foreignWord = foreignWord,
            ipaHolders = ipaHolders,
            autocompleteState = autocompleteState,
            pronunciationLoadingState = loadingState,
            nativeWordSuggestionsState = nativeWordSuggestions,
            closeAutocompletePopupMenu = { viewModel.sendEvent(event = CloseAutocompleteMenu) },
            closeNativeWordSuggestionsPopupMenu = {
                viewModel.sendEvent(event = CloseNativeWordSuggestionsMenu)
            },
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
            onPronounceIconClick = { viewModel.sendEvent(event = PronounceForeignWordClicked) },

            onAutocompleteItemClick = { autocompleteWord ->
                viewModel.sendEvent(event = UpdateDataOnAutocompleteSelected(word = autocompleteWord))
            },
            onNativeWordFieldArrowIconClick = {
                viewModel.sendEvent(event = ManageNativeWordSuggestionsMenuState)
            },
            onNativeWordSuggestionItemClick = { chosenWordIndex ->
                viewModel.sendEvent(event = NativeWordSelected(wordIndex = chosenWordIndex))
            },
            onConfirmSuggestionsSelection = {
                viewModel.sendEvent(event = ConfirmSuggestionsSelection)
            },
            onClearNativeWordSuggestionsSelectionClick = {
                viewModel.sendEvent(event = ClearNativeWordSuggestionsSelectionClicked)
            }
        )
    }
}