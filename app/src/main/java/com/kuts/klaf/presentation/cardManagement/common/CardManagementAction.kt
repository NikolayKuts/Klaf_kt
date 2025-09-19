package com.kuts.klaf.presentation.cardManagement.common

import androidx.compose.ui.text.input.TextFieldValue
import com.kuts.domain.ipa.LetterInfo

sealed interface CardManagementAction {

    data class UpdateNativeWord(val wordFieldValue: TextFieldValue) : CardManagementAction

    data class UpdateDataOnForeignWordChanged(
        val wordFieldValue: TextFieldValue
    ) : CardManagementAction

    data class UpdateDataOnAutocompleteSelected(val word: String) : CardManagementAction

    data class NativeWordSelected(val wordIndex: Int) : CardManagementAction

    data object ConfirmSuggestionsSelection : CardManagementAction

    data object ClearNativeWordSuggestionsSelectionClicked : CardManagementAction

    data class UpdateIpa(val letterGroupIndex: Int, val ipa: TextFieldValue) : CardManagementAction

    data class ChangeLetterSelectionWithIpaTemplate(
        val index: Int,
        val letterInfo: LetterInfo
    ) : CardManagementAction

    data object PronounceForeignWordClicked : CardManagementAction

    data object NativeWordFieldIconClicked : CardManagementAction

    data object CloseAutocompleteMenu : CardManagementAction

    data object CloseNativeWordSuggestionsMenu : CardManagementAction

    data object CardManagementConfirmed : CardManagementAction

    data class IpaTextFieldFocusChanged(
        val focusList: List<IpaTextFieldFocusState>,
    ) : CardManagementAction
}