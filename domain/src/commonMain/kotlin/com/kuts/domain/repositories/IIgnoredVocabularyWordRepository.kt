package com.kuts.domain.repositories

import com.kuts.domain.entities.IgnoredVocabularyWord
import com.kuts.domain.vocabularySource.VocabularySourceItemKey
import kotlinx.coroutines.flow.Flow

interface IIgnoredVocabularyWordRepository {

    suspend fun fetchWords(): List<IgnoredVocabularyWord>

    fun observeWords(): Flow<List<IgnoredVocabularyWord>>

    suspend fun saveWords(words: List<IgnoredVocabularyWord>)

    suspend fun removeWords(keys: Set<VocabularySourceItemKey>)
}
