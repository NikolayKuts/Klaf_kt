package com.example.klaf.presentation.cardAddition

import com.example.domain.ipa.IpaHolder
import com.example.domain.ipa.LetterInfo

sealed class CardAdditionState(
    val letterInfos: List<LetterInfo>,
    val nativeWord: String,
    val foreignWord: String,
    val ipaHolders: List<IpaHolder>,
) {

    class Adding(
        letterInfos: List<LetterInfo>,
        nativeWord: String = "",
        foreignWord: String = "",
        ipaTemplate: List<IpaHolder> = emptyList(),
    ) : CardAdditionState(
        letterInfos = letterInfos,
        nativeWord = nativeWord,
        foreignWord = foreignWord,
        ipaHolders = ipaTemplate,
    )

    object Finished : CardAdditionState(
        letterInfos = emptyList(),
        nativeWord = "",
        foreignWord = "",
        ipaHolders = emptyList(),
    )
}