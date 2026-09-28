package com.kuts.klaf.room.repositoryImplementations

import com.kuts.domain.entities.IgnoredVocabularyWord
import com.kuts.domain.repositories.IIgnoredVocabularyWordRepository
import com.kuts.klaf.room.databases.KlafRoomDatabase
import com.kuts.klaf.room.toDomainEntity
import com.kuts.klaf.room.toRoomEntity

class IgnoredVocabularyWordRepositoryRoom(
    private val roomDatabase: KlafRoomDatabase,
) : IIgnoredVocabularyWordRepository {

    override suspend fun fetchWords(): List<IgnoredVocabularyWord> = roomDatabase
        .ignoredVocabularyWordDao()
        .getWords()
        .map { word -> word.toDomainEntity() }

    override suspend fun saveWords(words: List<IgnoredVocabularyWord>) {
        if (words.isEmpty()) return

        roomDatabase.ignoredVocabularyWordDao()
            .insertWords(words = words.map { word -> word.toRoomEntity() })
    }
}
