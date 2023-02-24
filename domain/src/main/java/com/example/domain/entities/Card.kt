package com.example.domain.entities

data class Card(
    val deckId: Int,
    val nativeWord: String,
    val foreignWord: String,
    val ipa: String,
    val id: Int = 0
)