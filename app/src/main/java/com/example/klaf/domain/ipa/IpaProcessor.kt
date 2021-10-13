package com.example.klaf.domain.ipa

import java.lang.StringBuilder

class IpaProcessor {

    /**
     * first word
     * /f=f/ir/s=s/t w/o=o/r/d=d
     */

    fun getUncompletedCouples(letterInfos: List<LetterInfo>): String {
        val ipaTemplate: MutableList<String> = ArrayList()
        val result = StringBuilder()

        for (letterInfo in letterInfos) {
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

        for (item in ipaTemplate) {
            result.append(item)
        }
        return result.toString()
    }


    fun getCodedIpaForDB(letterInfos: List<LetterInfo>, ipaTemplate: String): String {

        val result = StringBuilder()
        val clearedIpaTemplate: String = ipaTemplate.replace(Regex("\\s*=[^\n\\w]*"), "=")

        var sbIpaTemplate = StringBuilder(clearedIpaTemplate)

        var index = 0
        while (index < letterInfos.size) {
            val letterInfo: LetterInfo = letterInfos.get(index)
            if (letterInfo.isChecked) {
                if (index == 0) {
                    result.append("/")
                } else if (index > 0 && letterInfos[index - 1].isChecked) {
                    result.append("/")
                } else if (index > 0 && letterInfos.get(index - 1).isChecked) {
                    result.append("//")
                }
                var ipaCouple: String
                if (sbIpaTemplate.toString().substring(1).contains("\n")) {
                    if (sbIpaTemplate.toString().startsWith("\n")) {
                        sbIpaTemplate = StringBuilder(sbIpaTemplate.substring(1))
                    }
                    ipaCouple = sbIpaTemplate.substring(0, sbIpaTemplate.indexOf("\n"))
                } else {
                    if (sbIpaTemplate.toString().startsWith("\n")) {
                        sbIpaTemplate = StringBuilder(sbIpaTemplate.substring(1))
                    }
                    ipaCouple = sbIpaTemplate.substring(0)
                }
                val replacedLetters: String =
                    sbIpaTemplate.substring(0, sbIpaTemplate.indexOf("="))
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


//        val clearedIpaTemplate = ipaTemplate.replace(Regex("\\s*=[^\n\\w]*"), "=")

//        return StringBuilder().also { result ->
//            Log.i("ipa_test", "getCodedIpaForDB: $letterInfos")
//            Log.i("ipa_test", "getCodedIpaForDB: ${letterInfos.size}")
//
//
//            for ((index, letterInfo) in letterInfos.withIndex()) {
//                when {
//                    letterInfo.isNotChecked -> {
//                        when {
//                            index == 0
//                                    || letterInfos[index - 1].isNotChecked
//                                    || index == letterInfos.size - 1 -> {
//                                result.append(letterInfo.letter)
//                            }
//                            letterInfos[index - 1].isChecked -> {
//                                result.append(">${letterInfo.letter}")
//                            }
//                        }
//                    }
//
//                    letterInfo.isChecked -> {
//                        when {
//                            index == 0 -> {
//                                result.append("<${letterInfo.letter}")
//                            }
//                            index == letterInfos.size - 1 -> {
//                                if (letterInfos[index - 1].isNotChecked) {
//                                    result.append("<${letterInfo.letter}>")
//                                } else {
//                                    result.append("${letterInfo.letter}>")
//                                }
//                            }
//                            letterInfos[index - 1].isChecked -> {
//                                result.append(letterInfo.letter)
//                            }
//                            letterInfos[index - 1].isNotChecked -> {
//                                result.append("<${letterInfo.letter}")
//                            }
//                        }
//
//
//                    }
//                }
//            }
//        }.toString()

//    }
}