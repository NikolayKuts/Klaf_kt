package com.example.klaf.domain.ipa

import com.example.klaf.domain.entities.Card

fun List<LetterInfo>.convertUncompletedIpa(): String {
    val ipaTemplate: MutableList<String> = ArrayList()
    val result = StringBuilder()

    this.forEach { letterInfo ->
        if (letterInfo.isChecked) {
            if (ipaTemplate.isNotEmpty() && ipaTemplate.last() == " = ") {
                ipaTemplate.add("\n")
            }
            ipaTemplate.add(letterInfo.letter)
        } else if (ipaTemplate.isNotEmpty() && ipaTemplate.last() != " = ") {
            ipaTemplate.add(" = ")
        }
    }
    if (ipaTemplate.isNotEmpty() && ipaTemplate.last() != " = ") {
        ipaTemplate.add(" = ")
    }
    ipaTemplate.forEach { item -> result.append(item) }

    return result.toString()
}

fun Card.decodeToInfos(): List<LetterInfo> {
    return ArrayList<LetterInfo>().apply {

        val foreignWordBuilder = java.lang.StringBuilder(ipa)
        while (foreignWordBuilder.isNotEmpty()) {

            if (foreignWordBuilder.substring(0, 1) == "/") {
                foreignWordBuilder.delete(0, 1)
                val equalsIndex = foreignWordBuilder.indexOf("=")
                val letters = foreignWordBuilder.substring(0, equalsIndex)

                for (index in letters.indices) {
                    this.add(LetterInfo(letters.substring(index, index + 1), true))
                }

                if (foreignWordBuilder.toString().contains("/")) {
                    foreignWordBuilder.delete(0, foreignWordBuilder.indexOf("/") + 1)
                } else {
                    foreignWordBuilder.delete(0, foreignWordBuilder.length)
                }

            } else {
                this.add(LetterInfo(foreignWordBuilder.substring(0, 1), false))
                foreignWordBuilder.delete(0, 1)
            }
        }
    }
}

fun Card.decodeIpa(): String {
    // TODO("refactore - there is a problem")
    val result = java.lang.StringBuilder()
    val ipa = java.lang.StringBuilder(this.ipa)

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

fun List<LetterInfo>.convertToEncodedIpa(ipaTemplate: String): String {

    val result = java.lang.StringBuilder()
    val clearedIpaTemplate: String = ipaTemplate.replace(
        regex = Regex(pattern = "\\s*=[^\n\\w]*"),
        replacement = "="
    )

    var ipaTemplateBuilder = java.lang.StringBuilder(clearedIpaTemplate)

    var index = 0
    while (index < this.size) {
        val letterInfo: LetterInfo = this[index]

        if (letterInfo.isChecked) {
            when {
                index == 0 -> result.append("/")
                index > 0 && this[index - 1].isNotChecked -> result.append("/")
                index > 0 && this[index - 1].isChecked -> result.append("//")
            }

            val ipaCouple = when {
                ipaTemplateBuilder.isNotEmpty()
                        && ipaTemplateBuilder.substring(1).contains("\n") -> {
                    if (ipaTemplateBuilder.startsWith("\n")) {
                        ipaTemplateBuilder =
                            java.lang.StringBuilder(ipaTemplateBuilder.substring(1))
                    }
                    ipaTemplateBuilder.substring(0, ipaTemplateBuilder.indexOf("\n"))
                }
                else -> {
                    if (ipaTemplateBuilder.startsWith("\n")) {
                        ipaTemplateBuilder =
                            java.lang.StringBuilder(ipaTemplateBuilder.substring(1))
                    }
                    ipaTemplateBuilder.substring(0)
                }
            }
//
            val replacedLetters: String =
                ipaTemplateBuilder.substring(0, ipaTemplateBuilder.indexOf("="))
            index += replacedLetters.length - 1
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

fun Card.decodeToIpaPrompts(): List<LetterInfo> {
    val ipa = java.lang.StringBuilder(this.ipa)
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