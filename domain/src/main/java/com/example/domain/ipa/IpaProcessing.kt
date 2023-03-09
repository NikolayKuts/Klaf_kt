package com.example.domain.ipa

import com.example.domain.common.ifTrue
import com.example.domain.entities.Card

fun List<LetterInfo>.convertToUncompletedIpa(): String {
    val equalsSign = " = "
    val result = StringBuilder()

    this.forEach { letterInfo ->
        if (letterInfo.isChecked) {
            result.endsWith(equalsSign)
                .ifTrue { result.append("\n") }

            result.append(letterInfo.letter)
        } else if (result.isNotEmpty() && !result.endsWith(equalsSign)) {
            result.append(equalsSign)
        }
    }

    if (result.isNotEmpty() && !result.endsWith(equalsSign)) {
        result.append(equalsSign)
    }

    return result.toString()
}

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

fun String.toFormattedIpa(): String {
    return replace(
        regex = Regex(pattern = "^\\s*"),
        replacement = ""
    ).replace(
        regex = Regex(pattern = "\\n\\s*"),
        replacement = "\n"
    ).replace(
        regex = Regex(pattern = "\\s*$"),
        replacement = ""
    ).replace(
        regex = Regex(pattern = "\\s*\\n"),
        replacement = "\n"
    ).replace(
        regex = Regex(pattern = "\\s*=[^\n\\w]*"),
        replacement = "="
    )
}

fun List<LetterInfo>.toEncodedIpa(ipaTemplate: String): String {
    val result = StringBuilder()
    val ipaTemplateBuilder = StringBuilder(ipaTemplate.toFormattedIpa())
    var pointer = 0

    while (pointer < this.size) {
        val letterInfo = this[pointer]

        if (letterInfo.isChecked) {
            when {
                pointer == 0 -> result.append("<")
                this[pointer - 1].isNotChecked -> result.append("<")
                this[pointer - 1].isChecked -> result.append("><")
            }

            val ipaCouple = when {
                ipaTemplateBuilder.isNotEmpty() && "\n" in ipaTemplateBuilder.substring(1) -> {
                    ipaTemplateBuilder.substring(0, ipaTemplateBuilder.indexOf("\n"))
                }
                else -> ipaTemplateBuilder.substring(0)
            }

            val replacedLetters: String =
                ipaTemplateBuilder.substring(0, ipaTemplateBuilder.indexOf("="))

            pointer += replacedLetters.lastIndex
            result.append(ipaCouple)
            ipaTemplateBuilder.delete(0, ipaCouple.length)

        } else {
            if (pointer > 0 && this[pointer - 1].isChecked) {
                result.append("<")
            }

            result.append(letterInfo.letter)
        }

        pointer++
    }

    return result.toString()
}

fun List<LetterInfo>.convertToEncodedIpa(ipaTemplate: String): String {
    val result = StringBuilder()
    val clearedIpaTemplate: String = ipaTemplate.replace(
        regex = Regex(pattern = "^\\s*"),
        replacement = ""
    ).replace(
        regex = Regex(pattern = "\\n\\s*"),
        replacement = "\n"
    ).replace(
        regex = Regex(pattern = "\\s*$"),
        replacement = ""
    ).replace(
        regex = Regex(pattern = "\\s*\\n"),
        replacement = "\n"
    ).replace(
        regex = Regex(pattern = "\\s*=[^\n\\w]*"),
        replacement = "="
    )

    var ipaTemplateBuilder = StringBuilder(clearedIpaTemplate)
    var index = 0

    while (index < this.size) {
        val letterInfo: LetterInfo = this[index]

        if (letterInfo.isChecked) {
            when {
                index == 0 -> result.append("/")
                this[index - 1].isNotChecked -> result.append("/")
                this[index - 1].isChecked -> result.append("//")
            }

            if (ipaTemplateBuilder.startsWith(prefix = "\n")) {
                ipaTemplateBuilder = StringBuilder(ipaTemplateBuilder.substring(1))
            }

            val ipaCouple = when {
                ipaTemplateBuilder.isNotEmpty() && "\n" in ipaTemplateBuilder.substring(1) -> {
                    ipaTemplateBuilder.substring(0, ipaTemplateBuilder.indexOf("\n"))
                }
                else -> ipaTemplateBuilder.substring(0)
            }

            val replacedLetters: String =
                ipaTemplateBuilder.substring(0, ipaTemplateBuilder.indexOf("="))

            index += replacedLetters.lastIndex
            result.append(ipaCouple)
            ipaTemplateBuilder.delete(0, ipaCouple.length)

        } else {
            if (index > 0 && this[index - 1].isChecked) {
                result.append("/")
            }

            result.append(letterInfo.letter)
        }

        index++
    }

    return result.toString()
}

fun Card.toInfos(): List<LetterInfo> {
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

fun Card.decodeToCompletedViewingIpa(): String {
    return decodeToCompletedIpa().replace("\n", ", ")
}

fun Card.decodeToCompletedIpa(): String {
    // TODO("refactore - there is a problem")
    val result = java.lang.StringBuilder()
//    val ipa = StringBuilder(this.ipa)
    val ipa = StringBuilder() // remove

    while (ipa.isNotEmpty()) {
        if (ipa.substring(0, 1) == "/") {
            ipa.delete(0, 1)
            var couple: String
            if (ipa.contains("/")) {
                val index = ipa.indexOf("/")
                couple = "${ipa.substring(0, index)}\n"
                ipa.delete(0, index + 1)
            } else {
                couple = ipa.substring(0, ipa.length)
                ipa.delete(0, ipa.length)
            }
            result.append(couple)
        } else {
            ipa.delete(0, 1)
        }
    }
    if (result.endsWith("\n")) {
        val resultLength = result.length
        result.delete(resultLength - 1, resultLength)
    }

    return result.toString().replace("=", " = ")
}


fun Card.decodeToIpaPrompts(): List<LetterInfo> {
//    val ipa = java.lang.StringBuilder(this.ipa)
    val ipa = StringBuilder() // remove
    val result: MutableList<LetterInfo> = ArrayList()

    while (ipa.toString() != "") {
        if (ipa.startsWith("/")) {
            var codedLetters: String
            val sound: String = if (ipa.substring(1).contains("/")) {
                codedLetters = ipa.substring(0, ipa.indexOf("/", 1) + 1)
                ipa.delete(0, ipa.indexOf("/", 1) + 1)
                codedLetters.substring(
                    codedLetters.indexOf("=") + 1,
                    codedLetters.indexOf("/", 1)
                )
            } else {
                codedLetters = ipa.substring(0)
                ipa.delete(0, ipa.length + 1)
                codedLetters.substring(codedLetters.indexOf("=") + 1)
            }
            val letters: String = codedLetters.substring(1, codedLetters.indexOf("="))
            val letterLength = letters.length
            val soundLength = sound.length

            if (letterLength > soundLength) {
                val d = (letterLength.toDouble() - soundLength.toDouble()) / 2
                val n = (letterLength - soundLength) / 2
                val indexForPutting = if (d > n) n + 1 else n
                var j = 0
                var q = 0

                while (j < letterLength) {
                    if (j < indexForPutting || soundLength <= q) {
                        result.add(LetterInfo(letters.substring(j, j + 1), false))
                    } else {
                        result.add(LetterInfo(sound.substring(q, q + 1), true))
                        q++
                    }
                    j++
                }
            } else {
                for (w in 0 until soundLength) {
                    result.add(LetterInfo(sound.substring(w, w + 1), true))
                }
            }
        } else {
            result.add(LetterInfo(ipa.substring(0, 1), false))
            ipa.delete(0, 1)
        }
    }
    return result
}

fun List<LetterInfo>.toWord(): String {
    return this.joinToString(separator = "") { letterInfo -> letterInfo.letter }
}
