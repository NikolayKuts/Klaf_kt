package com.kuts.klaf.cardManagement.cardAddition

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import com.kuts.domain.common.ifTrue
import com.kuts.klaf.cardManagement.common.BaseCardManagementViewModel
import com.kuts.klaf.cardManagement.common.CambridgeMeaning
import com.kuts.klaf.cardManagement.common.CambridgePartOfSpeech
import com.kuts.klaf.cardManagement.common.CambridgePhrase
import com.kuts.klaf.cardManagement.common.CambridgeWordData
import com.kuts.klaf.cardManagement.common.CardManagementView
import com.kuts.klaf.cardManagement.common.ICambridgeDataState
import com.kuts.klaf.cardManagement.common.ICardManagementAction
import com.kuts.klaf.common.BaseMainViewModel
import com.kuts.klaf.navigation.CollectFlowWithLifecycle
import com.kuts.klaf.navigation.ObserveAudioLifecycle
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

private const val MIME_TYPE_TEXT_PLAIN = "text/plain"

@Composable
internal fun CardAdditionScreen(
    backStackEntry: NavBackStackEntry,
    sharedViewModel: BaseMainViewModel,
    context: Context,
    deckId: Int,
) {
    val viewModel: CardAdditionViewModel = koinViewModel(
        viewModelStoreOwner = backStackEntry,
        parameters = {
            parametersOf(
                deckId,
                context.retrieveSmartSelectedWord(),
            )
        },
    )

    ObserveAudioLifecycle(
        onCreate = viewModel.audioPlayer::onCreate,
        onResume = viewModel.audioPlayer::onResume,
        onStop = viewModel.audioPlayer::onStop,
        onDestroy = viewModel.audioPlayer::onDestroy,
    )

    CollectFlowWithLifecycle(flow = viewModel.eventMessage, onEach = sharedViewModel::notify)

    Surface {
        CardManagementContent(viewModel = viewModel)
    }
}

private fun Context.retrieveSmartSelectedWord(): String? {
    val activity = findActivity() ?: return null

    val intent = activity.intent ?: return null
    if (intent.action != Intent.ACTION_PROCESS_TEXT) {
        return null
    }

    val selectedWord = intent.type
        ?.startsWith(MIME_TYPE_TEXT_PLAIN)
        ?.ifTrue { intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString() }

    if (selectedWord != null) {
        intent.removeExtra(Intent.EXTRA_PROCESS_TEXT)
        intent.action = Intent.ACTION_MAIN
    }

    return selectedWord
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CardManagementContent(
    viewModel: BaseCardManagementViewModel,
    isCambridgeBottomSheetEnabled: Boolean = true,
) {
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
    val ipaKeyboardState by viewModel.ipaKeyboardState.collectAsState()
    val scaffoldState = rememberModalBottomSheetState()
    val showBottomSheet = remember { mutableStateOf(false) }

    deck.value?.let { receivedDeck ->
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
            cambridgeDataAvailable = isCambridgeBottomSheetEnabled
                && cambridgeDataState is ICambridgeDataState.Fetched,
            ipaKeyboardState = ipaKeyboardState,
            onIpaTextFieldFocusChanged = { focusList ->
                viewModel.sendAction(
                    action = ICardManagementAction.IpaTextFieldFocusChanged(focusList = focusList)
                )
            },
            onBottomSheetAction = {
                scope.launch { showBottomSheet.value = showBottomSheet.value.not() }
            },
            onForeignWordTextFieldClick = {
                viewModel.sendAction(action = ICardManagementAction.CloseNativeWordSuggestionsMenu)
            },
            closeAutocompletePopupMenu = {
                viewModel.sendAction(action = ICardManagementAction.CloseAutocompleteMenu)
            },
            closeNativeWordSuggestionsPopupMenu = {
                viewModel.sendAction(action = ICardManagementAction.CloseNativeWordSuggestionsMenu)
            },
            onLetterClick = { index, letterInfo ->
                viewModel.sendAction(
                    action = ICardManagementAction.ChangeLetterSelectionWithIpaTemplate(
                        index = index,
                        letterInfo = letterInfo
                    )
                )
            },
            onNativeWordFieldValueChange = { wordFieldValue ->
                viewModel.sendAction(
                    action = ICardManagementAction.UpdateNativeWord(
                        wordFieldValue = wordFieldValue
                    )
                )
            },
            onForeignWordFieldValueChange = { wordFieldValue ->
                viewModel.sendAction(
                    action = ICardManagementAction.UpdateDataOnForeignWordChanged(wordFieldValue = wordFieldValue)
                )
            },
            onIpaTextFieldValueChange = { letterGroupIndex, ipa ->
                viewModel.sendAction(
                    action = ICardManagementAction.UpdateIpa(
                        letterGroupIndex = letterGroupIndex,
                        ipa = ipa
                    )
                )
            },
            onConfirmClick = {
                viewModel.sendAction(action = ICardManagementAction.CardManagementConfirmed)
            },
            onPronounceIconClick = {
                viewModel.sendAction(action = ICardManagementAction.PronounceForeignWordClicked)
            },
            onAutocompleteItemClick = { autocompleteWord ->
                viewModel.sendAction(
                    action = ICardManagementAction.UpdateDataOnAutocompleteSelected(
                        word = autocompleteWord
                    )
                )
            },
            transcription = transcription,
            onNativeWordFieldArrowIconClick = {
                viewModel.sendAction(action = ICardManagementAction.NativeWordFieldIconClicked)
            },
            onNativeWordSuggestionItemClick = { chosenWordIndex ->
                viewModel.sendAction(action = ICardManagementAction.NativeWordSelected(wordIndex = chosenWordIndex))
            },
            onConfirmSuggestionsSelection = {
                viewModel.sendAction(action = ICardManagementAction.ConfirmSuggestionsSelection)
            },
            onClearNativeWordSuggestionsSelectionClick = {
                viewModel.sendAction(action = ICardManagementAction.ClearNativeWordSuggestionsSelectionClicked)
            }
        )

        if (isCambridgeBottomSheetEnabled) {
            BottomSheet(
                scaffoldState = scaffoldState,
                showBottomSheetState = showBottomSheet,
                cambridgeDataState = cambridgeDataState,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottomSheet(
    cambridgeDataState: ICambridgeDataState,
    scaffoldState: SheetState = rememberModalBottomSheetState(),
    showBottomSheetState: MutableState<Boolean>,
) {
    if (showBottomSheetState.value) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheetState.value = false },
            sheetState = scaffoldState,
            content = {
                (cambridgeDataState as? ICambridgeDataState.Fetched)?.let {
                    WordDetailsScreen(word = it.word)
                }
            }
        )
    }
}

@Composable
private fun WordDetailsScreen(word: CambridgeWordData) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 50.dp, top = 8.dp)
            .padding(horizontal = 16.dp),
    ) {
        item {
            Text(
                text = word.text.uppercase(),
                style = MaterialTheme.typography.titleLarge,
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
private fun PartOfSpeechSection(pos: CambridgePartOfSpeech) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = pos.text + if (pos.label.isNotBlank()) " (${pos.label})" else "",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFFDD462)
        )

        if (pos.ipas.isNotEmpty()) {
            Text(
                modifier = Modifier.padding(start = 8.dp),
                text = pos.ipas.joinToString(" / ", prefix = "[", postfix = "]"),
                style = MaterialTheme.typography.bodyLarge,
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
                style = MaterialTheme.typography.bodyLarge,
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
private fun MeaningItem(meaning: CambridgeMeaning) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(Color(0x0fffffff))
            .padding(all = 4.dp)
    ) {
        Text(
            text = meaning.explanation,
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = meaning.translation,
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF59BEF3)
        )
        meaning.examples.forEach { example ->
            Text(
                text = "• $example",
                style = MaterialTheme.typography.bodyLarge,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}

@Composable
private fun PhraseItem(phrase: CambridgePhrase) {
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
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = phrase.translation,
            style = MaterialTheme.typography.bodyLarge
        )
        phrase.examples.forEach {
            Text(
                text = "• $it",
                style = MaterialTheme.typography.bodyLarge,
                fontStyle = FontStyle.Italic
            )
        }
    }
}
