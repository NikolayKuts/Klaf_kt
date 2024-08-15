package com.kuts.klaf.presentation.cardManagement.cardAddition

import com.kuts.domain.ipa.IpaHolder
import com.kuts.domain.ipa.LetterInfo


sealed interface CardAdditionEvent {

    data class UpdateNativeWord(val word: String) : CardAdditionEvent

    data class UpdateDataOnForeignWordChanged(val word: String) : CardAdditionEvent

    data class UpdateDataOnAutocompleteSelected(val word: String) : CardAdditionEvent

    data class NativeWordSelected(val wordIndex: Int) : CardAdditionEvent

    data object ConfirmSuggestionsSelection : CardAdditionEvent

    data object ClearNativeWordSuggestionsSelectionClicked : CardAdditionEvent

    data class UpdateIpa(val letterGroupIndex: Int, val ipa: String) : CardAdditionEvent

    data class ChangeLetterSelectionWithIpaTemplate(
        val index: Int,
        val letterInfo: LetterInfo
    ) : CardAdditionEvent

    data class  AddNewCard(
        val deckId: Int,
        val nativeWord: String,
        val foreignWord: String,
        val letterInfos: List<LetterInfo>,
        val ipaHolders: List<IpaHolder>,
    ) : CardAdditionEvent

    data object PronounceForeignWordClicked : CardAdditionEvent

    data object ManageNativeWordSuggestionsMenuState : CardAdditionEvent

    data object CloseAutocompleteMenu : CardAdditionEvent

    data object CloseNativeWordSuggestionsMenu : CardAdditionEvent
}