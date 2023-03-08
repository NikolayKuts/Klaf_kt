package com.example.domain.ipa

data class LetterInfo(
    val letter: String,
    val isChecked: Boolean
) {

    val isNotChecked get() = !isChecked

    companion object {

        const val EMPTY_LETTER = " "
    }
}