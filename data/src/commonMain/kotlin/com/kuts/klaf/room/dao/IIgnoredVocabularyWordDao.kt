package com.kuts.klaf.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kuts.klaf.room.dao.IIgnoredVocabularyWordDao.Companion.IGNORED_VOCABULARY_WORD_TABLE_NAME
import com.kuts.klaf.room.entities.RoomIgnoredVocabularyWord
import kotlinx.coroutines.flow.Flow

@Dao
interface IIgnoredVocabularyWordDao {

    @Query("SELECT * FROM $IGNORED_VOCABULARY_WORD_TABLE_NAME ORDER BY createdAt ASC, id ASC")
    suspend fun getWords(): List<RoomIgnoredVocabularyWord>

    @Query("SELECT * FROM $IGNORED_VOCABULARY_WORD_TABLE_NAME ORDER BY createdAt ASC, id ASC")
    fun observeWords(): Flow<List<RoomIgnoredVocabularyWord>>

    @Query("UPDATE $IGNORED_VOCABULARY_WORD_TABLE_NAME SET lastChangedServerRevision = :revision " +
        "WHERE languageKey = :language AND foreignWordKey = :foreign AND nativeWordKey = :native")
    suspend fun updateRevision(language: String, foreign: String, native: String, revision: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWords(words: List<RoomIgnoredVocabularyWord>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun replaceSyncedWords(words: List<RoomIgnoredVocabularyWord>)

    @Query("DELETE FROM $IGNORED_VOCABULARY_WORD_TABLE_NAME")
    suspend fun deleteWords()

    companion object {

        const val IGNORED_VOCABULARY_WORD_TABLE_NAME = "ignored_vocabulary_words"
    }
}
