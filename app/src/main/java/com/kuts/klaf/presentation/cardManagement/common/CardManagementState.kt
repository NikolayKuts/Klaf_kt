package com.kuts.klaf.presentation.cardManagement.common

import androidx.compose.ui.text.input.TextFieldValue
import com.kuts.domain.ipa.LetterInfo

sealed class CardManagementState(
    val letterInfos: List<LetterInfo>,
    val nativeWordFieldValue: TextFieldValue,
    val foreignWordFieldValue: TextFieldValue,
    val textFieldValueIpaHolders: List<TextFieldValueIpaHolder>,
) {

    class InProgress(
        letterInfos: List<LetterInfo> = emptyList(),
        nativeWord: TextFieldValue = TextFieldValue(),
        foreignWord: TextFieldValue = TextFieldValue(),
        ipaHolders: List<TextFieldValueIpaHolder> = emptyList(),
    ) : CardManagementState(
        letterInfos = letterInfos,
        nativeWordFieldValue = nativeWord,
        foreignWordFieldValue = foreignWord,
        textFieldValueIpaHolders = ipaHolders,
    )

    data object Finished : CardManagementState(
        letterInfos = emptyList(),
        nativeWordFieldValue = TextFieldValue(),
        foreignWordFieldValue = TextFieldValue(),
        textFieldValueIpaHolders = emptyList(),
    )
}