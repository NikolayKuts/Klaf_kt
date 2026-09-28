package com.kuts.domain.repositories

import com.kuts.domain.entities.IgnoredVocabularyWord

interface IIgnoredVocabularyWordRepository {

    suspend fun fetchWords(): List<IgnoredVocabularyWord>

    suspend fun saveWords(words: List<IgnoredVocabularyWord>)
}
