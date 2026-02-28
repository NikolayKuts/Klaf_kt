package com.kuts.klaf.cardManagement.common

import androidx.compose.ui.text.input.TextFieldValue
import com.kuts.domain.ipa.LetterInfo

sealed interface ICardManagementAction {

    data class UpdateNativeWord(val wordFieldValue: TextFieldValue) : ICardManagementAction

    data class UpdateDataOnForeignWordChanged(
        val wordFieldValue: TextFieldValue
    ) : ICardManagementAction

    data class UpdateDataOnAutocompleteSelected(val word: String) : ICardManagementAction

    data class NativeWordSelected(val wordIndex: Int) : ICardManagementAction

    data object ConfirmSuggestionsSelection : ICardManagementAction

    data object ClearNativeWordSuggestionsSelectionClicked : ICardManagementAction

    data class UpdateIpa(val letterGroupIndex: Int, val ipa: TextFieldValue) : ICardManagementAction

    data class ChangeLetterSelectionWithIpaTemplate(
        val index: Int,
        val letterInfo: LetterInfo
    ) : ICardManagementAction

    data object PronounceForeignWordClicked : ICardManagementAction

    data object FetchGeminiInsightsClicked : ICardManagementAction

    data object NativeWordFieldIconClicked : ICardManagementAction

    data object CloseAutocompleteMenu : ICardManagementAction

    data object CloseNativeWordSuggestionsMenu : ICardManagementAction

    data object CardManagementConfirmed : ICardManagementAction

    data class IpaTextFieldFocusChanged(
        val focusList: List<IpaTextFieldFocusState>,
    ) : ICardManagementAction
}
