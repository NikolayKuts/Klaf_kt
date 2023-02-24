package com.example.klaf.presentation.cardAddition

import com.example.domain.ipa.LetterInfo


sealed interface CardAdditionEvent {

    data class UpdateNativeWord(val word: String) : CardAdditionEvent

    data class UpdateDataOnForeignWordChaneged(val word: String) : CardAdditionEvent

    data class UpdateDataOnAutocompleteSelected(val word: String) : CardAdditionEvent

    data class UpdateIpaTemplate(val ipa: String) : CardAdditionEvent

    data class ChangeLetterSelectionWithIpaTemplate(
        val index: Int,
        val letterInfo: LetterInfo
    ) : CardAdditionEvent

    data class  AddNewCard(
        val deckId: Int,
        val nativeWord: String,
        val foreignWord: String,
        val letterInfos: List<LetterInfo>,
        val ipaTemplate: String,
    ) : CardAdditionEvent

    object PronounceForeignWord : CardAdditionEvent

    object CloseAutocompleteMenu : CardAdditionEvent
}