package com.example.klaf.domain.pojo

data class Card(
    private val id: Int,
    private val deckId: Int,
    private val nativeWord: String,
    private val foreignWord: String,
    private val ipa: String
) {

}