package com.kuts.domain.entities

import com.kuts.domain.ipa.IpaHolder

data class Card(
    val deckId: Int,
    val nativeWord: String,
    val foreignWord: String,
    val ipa: List<IpaHolder>,
    val id: Int = 0
)