package com.kuts.domain.vocabularySource

private val whitespaceRegex = Regex(pattern = "\\s+")

fun String.toVocabularyKey(): String = toMeaningKey()

fun String.toMeaningKey(): String = trim()
    .lowercase()
    .replace(regex = whitespaceRegex, replacement = " ")

fun String.toLanguageKey(): String = trim().lowercase()
