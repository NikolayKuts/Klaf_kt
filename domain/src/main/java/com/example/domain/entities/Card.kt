package com.example.domain.entities

import com.example.domain.ipa.IpaHolder

data class Card(
    val deckId: Int,
    val nativeWord: String,
    val foreignWord: String,
    val ipa: List<IpaHolder>,
    val id: Int = 0
)