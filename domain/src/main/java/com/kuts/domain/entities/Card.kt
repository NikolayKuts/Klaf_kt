package com.kuts.domain.entities

import com.kuts.domain.ipa.IpaHolder
import kotlinx.serialization.Serializable

@Serializable
data class Card(
    val deckId: Int,
    val nativeWord: String,
    val foreignWord: String,
    val ipa: List<IpaHolder>,
    val id: Int = 0
)