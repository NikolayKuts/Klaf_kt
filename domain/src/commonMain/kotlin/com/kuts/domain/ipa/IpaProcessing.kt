package com.kuts.domain.ipa

import com.kuts.domain.entities.Card

fun List<LetterInfo>.toRowIpaItemHolders(): List<IpaHolder> {
    val result = mutableMapOf<Int, String>()
    var pointer = 0
    var lastInputIndex = 0

    while (pointer < this.size) {
        if (this[pointer].isChecked) {
            val letters = when {
                pointer == 0 -> this[pointer].letter
                this[pointer - 1].isChecked -> result[lastInputIndex] + this[pointer].letter
                else -> {
                    lastInputIndex = pointer
                    this[pointer].letter
                }
            }
            result[lastInputIndex] = letters
        }

        pointer++
    }

    return result.map {
        IpaHolder(
            letterGroup = it.value,
            ipa = "",
            groupIndex = it.key
        )
    }
}

fun Card.toLetterInfos(): List<LetterInfo> {
    val result = foreignWord.toList()
        .map { LetterInfo(letter = it.toString(), isChecked = false) }
        .toMutableList()

    ipa.forEach { ipaHolder ->
        ipaHolder.letterGroup.forEachIndexed { index, letter ->
            result[ipaHolder.groupIndex + index] =
                LetterInfo(letter = letter.toString(), isChecked = true)
        }
    }

    return result
}

fun String.toRowInfos(): List<LetterInfo> = toList().map { letterChar ->
    LetterInfo(letter = letterChar.toString(), isChecked = false)
}

fun Card.toCompletedViewingIpa(): String = ipa.joinToString(separator = " ") { ipaHolder ->
    "${ipaHolder.letterGroup} = ${ipaHolder.ipa}"
}

fun Card.toIpaPrompts(): List<LetterInfo> {
    val result = foreignWord.toList()
        .map { letterChar -> LetterInfo(letter = letterChar.toString()) }
        .toMutableList()
    val initialResultSize = result.size
    val indexesForDeleting = mutableListOf<Int>()

    ipa.forEach { ipaHolder ->
        with(ipaHolder) {
            result[groupIndex] = LetterInfo(letter = "[$ipa]", isChecked = true)

            if (letterGroup.length > 1) {
                for (index in groupIndex + 1 until  groupIndex + letterGroup.length) {
                    indexesForDeleting.add(index)
                }
            }
        }
    }

    indexesForDeleting.forEach {
        result.removeAt(it - (initialResultSize - result.size))
    }

    return result
}

fun List<LetterInfo>.toWord(): String {
    return this.joinToString(separator = "") { info -> info.letter }
}