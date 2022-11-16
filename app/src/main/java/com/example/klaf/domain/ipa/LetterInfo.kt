package com.example.klaf.domain.ipa

data class LetterInfo(
    val letter: String,
    val isChecked: Boolean
) {
    val isNotChecked get() = !isChecked
}