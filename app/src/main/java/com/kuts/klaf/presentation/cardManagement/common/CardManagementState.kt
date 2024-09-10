package com.kuts.klaf.presentation.cardManagement.common

import androidx.compose.ui.text.input.TextFieldValue
import com.kuts.domain.ipa.IpaHolder
import com.kuts.domain.ipa.LetterInfo

sealed class CardManagementState(
    val letterInfos: List<LetterInfo>,
    val nativeWordFieldValue: TextFieldValue,
    val foreignWordFieldValue: TextFieldValue,
    val ipaHolders: List<IpaHolder>,
) {

    class InProgress(
        letterInfos: List<LetterInfo> = emptyList(),
        nativeWord: TextFieldValue = TextFieldValue(),
        foreignWord: TextFieldValue = TextFieldValue(),
        ipaHolders: List<IpaHolder> = emptyList(),
    ) : CardManagementState(
        letterInfos = letterInfos,
        nativeWordFieldValue = nativeWord,
        foreignWordFieldValue = foreignWord,
        ipaHolders = ipaHolders,
    )

    data object Finished : CardManagementState(
        letterInfos = emptyList(),
        nativeWordFieldValue = TextFieldValue(),
        foreignWordFieldValue = TextFieldValue(),
        ipaHolders = emptyList(),
    )
}