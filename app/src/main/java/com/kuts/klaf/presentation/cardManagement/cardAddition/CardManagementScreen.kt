package com.kuts.klaf.presentation.cardManagement.cardAddition

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.BottomSheetScaffoldState
import androidx.compose.material.BottomSheetValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.rememberBottomSheetScaffoldState
import androidx.compose.material.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cambridge.dictionary.core.Meaning
import com.cambridge.dictionary.core.PartsOfSpeech
import com.cambridge.dictionary.core.Phrase
import com.cambridge.dictionary.core.Word
import com.kuts.klaf.presentation.cardManagement.common.BaseCardManagementViewModel
import com.kuts.klaf.presentation.cardManagement.common.CambridgeDataState
import com.kuts.klaf.presentation.cardManagement.common.CardManagementAction
import com.kuts.klaf.presentation.cardManagement.common.CardManagementView
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CardManagementScreen(viewModel: BaseCardManagementViewModel) {
    val deck = viewModel.deck.collectAsState(initial = null)
    val cardState by viewModel.cardManagementState.collectAsState()
    val letterInfos = cardState.letterInfos
    val foreignWordFieldValue = cardState.foreignWordFieldValue
    val nativeWordFieldValue = cardState.nativeWordFieldValue
    val textFieldValueIpaHolders = cardState.textFieldValueIpaHolders
    val autocompleteState by viewModel.autocompleteState.collectAsState()
    val pronunciationLoadingState by viewModel.pronunciationLoadingState.collectAsState()
    val nativeWordSuggestionsState by viewModel.nativeWordSuggestionsState.collectAsState()
    val transcription by viewModel.transcriptionState.collectAsState()
    val cambridgeDataState by viewModel.cambridgeDataState.collectAsState()
    val scope = rememberCoroutineScope()
    val bottomSheetState = rememberBottomSheetState(initialValue = BottomSheetValue.Collapsed)
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = bottomSheetState
    )

    val ipaKeyboardState by viewModel.ipaKeyboardState.collectAsState()

    deck.value?.let { receivedDeck ->
       BottomSheet(
           scaffoldState = scaffoldState,
           cambridgeDataState = cambridgeDataState,
       ) {
            CardManagementView(
                deckName = receivedDeck.name,
                cardQuantity = receivedDeck.cardQuantity,
                letterInfos = letterInfos,
                nativeWordFieldValue = nativeWordFieldValue,
                foreignWordFieldValue = foreignWordFieldValue,
                textFieldValueIpaHolders = textFieldValueIpaHolders,
                autocompleteState = autocompleteState,
                pronunciationLoadingState = pronunciationLoadingState,
                nativeWordSuggestionsState = nativeWordSuggestionsState,
                cambridgeDataAvailable = cambridgeDataState is CambridgeDataState.Fetched,
                ipaKeyboardState = ipaKeyboardState,
                onIpaTextFieldFocusChanged = { focusList ->
                    viewModel.sendAction(
                        action = CardManagementAction.IpaTextFieldFocusChanged(focusList = focusList)
                    )
                },
                onBottomSheetAction = {
                    scope.launch {
                        if (scaffoldState.bottomSheetState.isExpanded) {
                            scaffoldState.bottomSheetState.collapse()
                        } else {
                            scaffoldState.bottomSheetState.expand()
                        }
                    }
                },
                onForeignWordTextFieldClick = {
                    viewModel.sendAction(action = CardManagementAction.CloseNativeWordSuggestionsMenu)
                },
                closeAutocompletePopupMenu = {
                    viewModel.sendAction(action = CardManagementAction.CloseAutocompleteMenu)
                },
                closeNativeWordSuggestionsPopupMenu = {
                    viewModel.sendAction(action = CardManagementAction.CloseNativeWordSuggestionsMenu)
                },
                onLetterClick = { index, letterInfo ->
                    viewModel.sendAction(
                        action = CardManagementAction.ChangeLetterSelectionWithIpaTemplate(
                            index = index,
                            letterInfo = letterInfo
                        )
                    )
                },
                onNativeWordFieldValueChange = { wordFieldValue ->
                    viewModel.sendAction(action = CardManagementAction.UpdateNativeWord(wordFieldValue = wordFieldValue))
                },
                onForeignWordFieldValueChange = { wordFieldValue ->
                    viewModel.sendAction(
                        action = CardManagementAction.UpdateDataOnForeignWordChanged(wordFieldValue = wordFieldValue)
                    )
                },
                onIpaTextFieldValueChange = { letterGroupIndex, ipa ->
                    viewModel.sendAction(
                        action = CardManagementAction.UpdateIpa(
                            letterGroupIndex = letterGroupIndex,
                            ipa = ipa
                        )
                    )
                },
                onConfirmClick = {
                    viewModel.sendAction(action = CardManagementAction.CardManagementConfirmed)
                },
                onPronounceIconClick = {
                    viewModel.sendAction(action = CardManagementAction.PronounceForeignWordClicked)
                },
                onAutocompleteItemClick = { autocompleteWord ->
                    viewModel.sendAction(
                        action = CardManagementAction.UpdateDataOnAutocompleteSelected(
                            word = autocompleteWord
                        )
                    )
                },
                transcription = transcription,
                onNativeWordFieldArrowIconClick = {
                    viewModel.sendAction(action = CardManagementAction.NativeWordFieldIconClicked)
                },
                onNativeWordSuggestionItemClick = { chosenWordIndex ->
                    viewModel.sendAction(action = CardManagementAction.NativeWordSelected(wordIndex = chosenWordIndex))
                },
                onConfirmSuggestionsSelection = {
                    viewModel.sendAction(action = CardManagementAction.ConfirmSuggestionsSelection)
                },
                onClearNativeWordSuggestionsSelectionClick = {
                    viewModel.sendAction(action = CardManagementAction.ClearNativeWordSuggestionsSelectionClicked)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun BottomSheet(
    cambridgeDataState: CambridgeDataState,
    scaffoldState: BottomSheetScaffoldState,
    content: @Composable (PaddingValues) -> Unit,
) {
    BottomSheetScaffold(
        modifier = Modifier,
        scaffoldState = scaffoldState,
        sheetPeekHeight = 0.dp,
        sheetContent = {
            (cambridgeDataState as? CambridgeDataState.Fetched)?.let {
                WordDetailsScreen(word = it.word)
            }
        },
        content = content
    )
}

@Composable
private fun WordDetailsScreen(word: Word) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 50.dp, top = 8.dp)
            .padding(horizontal = 16.dp),
    ) {
        item {
            Text(
                text = word.text.uppercase(),
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        word.partsOfSpeech.forEach { part ->
            item {
                PartOfSpeechSection(part)
            }
        }
    }
}

@Composable
private fun PartOfSpeechSection(pos: PartsOfSpeech) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = pos.text + if (pos.label.isNotBlank()) " (${pos.label})" else "",
            style = MaterialTheme.typography.body2,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFFDD462)
        )

        if (pos.ipas.isNotEmpty()) {
            Text(
                modifier = Modifier.padding(start = 8.dp),
                text = pos.ipas.joinToString(" / ", prefix = "[", postfix = "]"),
                style = MaterialTheme.typography.body2,
                color = Color(0xFFAFDA7F)
            )
        }

        pos.meanings.forEach { meaning ->
            MeaningItem(meaning)
            Spacer(modifier = Modifier.height(3.dp))
        }

        if (pos.phrases.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Phrases:",
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.body2,
                color = Color(0xFFF6754B)
            )
            pos.phrases.forEach { phrase ->
                PhraseItem(phrase)
                Spacer(modifier = Modifier.height(3.dp))
            }
        }
    }
}

@Composable
private fun MeaningItem(meaning: Meaning) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(Color(0x0fffffff))
            .padding(all = 4.dp)
    ) {
        Text(
            text = meaning.explanation,
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.body2
        )
        Text(
            text = meaning.translation,
            style = MaterialTheme.typography.body2,
            color = Color(0xFF59BEF3)
        )
        meaning.examples.forEach { example ->
            Text(
                text = "• $example",
                style = MaterialTheme.typography.body2,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}

@Composable
private fun PhraseItem(phrase: Phrase) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(Color(0x113ef300))
            .padding(all = 4.dp)
            .padding(start = 8.dp)
    ) {
        Text(
            text = phrase.text,
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.body2
        )
        Text(
            text = phrase.translation,
            style = MaterialTheme.typography.body2
        )
        phrase.examples.forEach {
            Text(
                text = "• $it",
                style = MaterialTheme.typography.body2,
                fontStyle = FontStyle.Italic
            )
        }
    }
}