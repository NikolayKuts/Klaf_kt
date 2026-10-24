package com.kuts.klaf.room.repositoryImplementations

import com.kuts.domain.entities.IgnoredVocabularyWord
import com.kuts.domain.repositories.IIgnoredVocabularyWordRepository
import com.kuts.domain.vocabularySource.VocabularySourceItemKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import com.kuts.klaf.room.databases.KlafRoomDatabase
import com.kuts.klaf.room.databases.RoomDatabaseSource
import com.kuts.klaf.room.databases.StaticRoomDatabaseSource
import com.kuts.klaf.room.toDomainEntity
import com.kuts.klaf.room.toRoomEntity

@OptIn(ExperimentalCoroutinesApi::class)
class IgnoredVocabularyWordRepositoryRoom(
    private val databaseSource: RoomDatabaseSource,
) : IIgnoredVocabularyWordRepository {

    constructor(roomDatabase: KlafRoomDatabase) : this(StaticRoomDatabaseSource(roomDatabase))

    private val roomDatabase: KlafRoomDatabase
        get() = databaseSource.current()

    override suspend fun fetchWords(): List<IgnoredVocabularyWord> = roomDatabase
        .ignoredVocabularyWordDao()
        .getWords()
        .map { word -> word.toDomainEntity() }

    override suspend fun saveWords(words: List<IgnoredVocabularyWord>) {
        if (words.isEmpty()) return

        roomDatabase.ignoredVocabularyWordDao()
            .insertWords(words = words.map { word -> word.toRoomEntity() })
    }

    override fun observeWords(): Flow<List<IgnoredVocabularyWord>> = databaseSource.databases.flatMapLatest { database ->
        database.ignoredVocabularyWordDao().observeWords().map { words -> words.map { it.toDomainEntity() } }
    }

    override suspend fun removeWords(keys: Set<VocabularySourceItemKey>) {
        val dao = roomDatabase.ignoredVocabularyWordDao()
        keys.forEach { key -> dao.deleteWord(key.language, key.foreignWord, key.nativeWord) }
    }
}
