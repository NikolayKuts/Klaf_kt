package com.example.klaf.domain.ipa

data class LetterInfo(
    val letter: String,
    var isChecked: Boolean
) {
    val isNotChecked get() = !isChecked
}