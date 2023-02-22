package com.example.klaf.presentation.cardAddition

import com.example.domain.ipa.LetterInfo

sealed class CardAdditionState(
    val letterInfos: List<LetterInfo>,
    val nativeWord: String,
    val foreignWord: String,
    val ipaTemplate: String,
) {

    class Adding(
        letterInfos: List<LetterInfo>,
        nativeWord: String = "",
        foreignWord: String = "",
        ipaTemplate: String = "",
    ) : CardAdditionState(
        letterInfos = letterInfos,
        nativeWord = nativeWord,
        foreignWord = foreignWord,
        ipaTemplate = ipaTemplate,
    )

    object Finished : CardAdditionState(
        letterInfos = emptyList(),
        nativeWord = "",
        foreignWord = "",
        ipaTemplate = "",
    )
}