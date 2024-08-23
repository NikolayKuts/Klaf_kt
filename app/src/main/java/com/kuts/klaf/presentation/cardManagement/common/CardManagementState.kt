package com.kuts.klaf.presentation.cardManagement.common

import com.kuts.domain.ipa.IpaHolder
import com.kuts.domain.ipa.LetterInfo

sealed class CardManagementState(
    val letterInfos: List<LetterInfo>,
    val nativeWord: String,
    val foreignWord: String,
    val ipaHolders: List<IpaHolder>,
) {

    class InProgress(
        letterInfos: List<LetterInfo> = emptyList(),
        nativeWord: String = "",
        foreignWord: String = "",
        ipaHolders: List<IpaHolder> = emptyList(),
    ) : CardManagementState(
        letterInfos = letterInfos,
        nativeWord = nativeWord,
        foreignWord = foreignWord,
        ipaHolders = ipaHolders,
    )

    data object Finished : CardManagementState(
        letterInfos = emptyList(),
        nativeWord = "",
        foreignWord = "",
        ipaHolders = emptyList(),
    )
}