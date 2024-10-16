package com.kuts.klaf.presentation.cardManagement.common

import androidx.compose.ui.text.input.TextFieldValue
import com.kuts.domain.ipa.LetterInfo

sealed interface CardManagementEvent {

    data class UpdateNativeWord(val wordFieldValue: TextFieldValue) : CardManagementEvent

    data class UpdateDataOnForeignWordChanged(
        val wordFieldValue: TextFieldValue
    ) : CardManagementEvent

    data class UpdateDataOnAutocompleteSelected(val word: String) : CardManagementEvent

    data class NativeWordSelected(val wordIndex: Int) : CardManagementEvent

    data object ConfirmSuggestionsSelection : CardManagementEvent

    data object ClearNativeWordSuggestionsSelectionClicked : CardManagementEvent

    data class UpdateIpa(val letterGroupIndex: Int, val ipa: TextFieldValue) : CardManagementEvent

    data class ChangeLetterSelectionWithIpaTemplate(
        val index: Int,
        val letterInfo: LetterInfo
    ) : CardManagementEvent

    data object PronounceForeignWordClicked : CardManagementEvent

    data object NativeWordFeildIconClicked : CardManagementEvent

    data object CloseAutocompleteMenu : CardManagementEvent

    data object CloseNativeWordSuggestionsMenu : CardManagementEvent

    data object CardManagementConfirmed : CardManagementEvent
}