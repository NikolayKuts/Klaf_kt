package com.kuts.domain.ipa

data class LetterInfo(
    val letter: String,
    val isChecked: Boolean = false,
) {

    val isNotChecked get() = !isChecked

    companion object {

        const val EMPTY_LETTER = " "
    }
}