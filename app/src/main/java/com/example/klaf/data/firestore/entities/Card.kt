package com.example.klaf.data.firestore.entities

data class Card(
    val deckId: Int = DEFAULT_INT_VALUE,
    val nativeWord: String = DEFAULT_STRING_VALUE,
    val foreignWord: String = DEFAULT_STRING_VALUE,
    val ipa: String = DEFAULT_STRING_VALUE,
    val id: Int = DEFAULT_INT_VALUE
)