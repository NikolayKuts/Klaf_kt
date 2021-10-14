package com.example.klaf.domain.ipa

import java.lang.StringBuilder

class IpaProcessor {

    /**
     * first word
     * /f=f/ir/s=s/t w/o=o/r/d=d
     */

    fun getUncompletedIpa(letterInfos: List<LetterInfo>): String {
        val ipaTemplate: MutableList<String> = ArrayList()
        val result = StringBuilder()

        letterInfos.forEach { letterInfo ->
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


    fun getCodedIpaForDB(letterInfos: List<LetterInfo>, ipaTemplate: String): String {

        val result = StringBuilder()
        val clearedIpaTemplate: String = ipaTemplate.replace(
            regex = Regex(pattern = "\\s*=[^\n\\w]*"),
            replacement = "="
        )

        var sbIpaTemplate = StringBuilder(clearedIpaTemplate)

        var index = 0
        while (index < letterInfos.size) {
            val letterInfo: LetterInfo = letterInfos[index]
            if (letterInfo.isChecked) {
                when {
                    index == 0 -> result.append("/")
                    index > 0 && letterInfos[index - 1].isNotChecked -> result.append("/")
                    index > 0 && letterInfos[index - 1].isChecked -> result.append("//")
                }

                var ipaCouple: String
                when {
                    sbIpaTemplate.substring(1).contains("\n") -> {
                        if (sbIpaTemplate.startsWith("\n")) {
                            sbIpaTemplate = StringBuilder(sbIpaTemplate.substring(1))
                        }
                        ipaCouple = sbIpaTemplate.substring(0, sbIpaTemplate.indexOf("\n"))
                    }
                    else -> {
                        if (sbIpaTemplate.startsWith("\n")) {
                            sbIpaTemplate = StringBuilder(sbIpaTemplate.substring(1))
                        }
                        ipaCouple = sbIpaTemplate.substring(0)
                    }
                }
//                
                val replacedLetters: String = sbIpaTemplate.substring(0, sbIpaTemplate.indexOf("="))
                index += replacedLetters.length - 1
                result.append(ipaCouple)
                sbIpaTemplate.delete(0, ipaCouple.length)

            } else {
                if (index > 0 && letterInfos[index - 1].isChecked) {
                    result.append("/")
                }
                result.append(letterInfo.letter)
            }
            index++
        }
        return result.toString()
    }
}