package com.kuts.klaf.presentation.cardManagement.cardAddition

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.kuts.klaf.presentation.cardManagement.common.BaseCardManagementViewModel
import com.kuts.klaf.presentation.cardManagement.common.CardManagementEvent
import com.kuts.klaf.presentation.cardManagement.common.CardManagementView

@Composable
fun CardManagementScreen(viewModel: BaseCardManagementViewModel) {
    val deck = viewModel.deck.collectAsState(initial = null)
    val cardState by viewModel.cardManagementState.collectAsState()
    val letterInfos = cardState.letterInfos
    val foreignWordFieldValue = cardState.foreignWordFieldValue
    val nativeWordFieldValue = cardState.nativeWordFieldValue
    val ipaHolders = cardState.ipaHolders
    val autocompleteState by viewModel.autocompleteState.collectAsState()
    val pronunciationLoadingState by viewModel.pronunciationLoadingState.collectAsState()
    val nativeWordSuggestionsState by viewModel.nativeWordSuggestionsState.collectAsState()
    val transcription by viewModel.transcriptionState.collectAsState()

    deck.value?.let { receivedDeck ->
        CardManagementView(
            deckName = receivedDeck.name,
            cardQuantity = receivedDeck.cardQuantity,
            letterInfos = letterInfos,
            nativeWordFieldValue = nativeWordFieldValue,
            foreignWordFieldValue = foreignWordFieldValue,
            ipaHolders = ipaHolders,
            autocompleteState = autocompleteState,
            pronunciationLoadingState = pronunciationLoadingState,
            nativeWordSuggestionsState = nativeWordSuggestionsState,
            onForeignWordTextFieldClick = {
                viewModel.sendEvent(event = CardManagementEvent.CloseNativeWordSuggestionsMenu)
            },
            closeAutocompletePopupMenu = {
                viewModel.sendEvent(event = CardManagementEvent.CloseAutocompleteMenu)
            },
            closeNativeWordSuggestionsPopupMenu = {
                viewModel.sendEvent(event = CardManagementEvent.CloseNativeWordSuggestionsMenu)
            },
            onLetterClick = { index, letterInfo ->
                viewModel.sendEvent(
                    event = CardManagementEvent.ChangeLetterSelectionWithIpaTemplate(
                        index = index,
                        letterInfo = letterInfo
                    )
                )
            },
            onNativeWordFieldValueChange = { wordFieldValue ->
                viewModel.sendEvent(event = CardManagementEvent.UpdateNativeWord(wordFieldValue = wordFieldValue))
            },
            onForeignWordFieldValueChange = { wordFieldValue ->
                viewModel.sendEvent(
                    event = CardManagementEvent.UpdateDataOnForeignWordChanged(wordFieldValue = wordFieldValue)
                )
            },
            onIpaChange = { letterGroupIndex, ipa ->
                viewModel.sendEvent(
                    event = CardManagementEvent.UpdateIpa(
                        letterGroupIndex = letterGroupIndex,
                        ipa = ipa
                    )
                )
            },
            onConfirmClick = {
                viewModel.sendEvent(event = CardManagementEvent.CardManagementConfirmed)
            },
            onPronounceIconClick = {
                viewModel.sendEvent(event = CardManagementEvent.PronounceForeignWordClicked)
            },
            onAutocompleteItemClick = { autocompleteWord ->
                viewModel.sendEvent(
                    event = CardManagementEvent.UpdateDataOnAutocompleteSelected(
                        word = autocompleteWord
                    )
                )
            },
            transcription = transcription,
            onNativeWordFieldArrowIconClick = {
                viewModel.sendEvent(event = CardManagementEvent.NativeWordFeildIconClicked)
            },
            onNativeWordSuggestionItemClick = { chosenWordIndex ->
                viewModel.sendEvent(event = CardManagementEvent.NativeWordSelected(wordIndex = chosenWordIndex))
            },
            onConfirmSuggestionsSelection = {
                viewModel.sendEvent(event = CardManagementEvent.ConfirmSuggestionsSelection)
            },
            onClearNativeWordSuggestionsSelectionClick = {
                viewModel.sendEvent(event = CardManagementEvent.ClearNativeWordSuggestionsSelectionClicked)
            }
        )
    }
}