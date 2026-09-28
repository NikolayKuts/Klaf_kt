package com.kuts.domain.entities

data class IgnoredVocabularyWord(
    val language: String,
    val foreignWord: String,
    val nativeWord: String,
    val createdAt: Long,
    val id: Int = 0,
)
