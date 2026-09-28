package com.kuts.klaf.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kuts.klaf.room.dao.IIgnoredVocabularyWordDao.Companion.IGNORED_VOCABULARY_WORD_TABLE_NAME
import com.kuts.klaf.room.entities.RoomIgnoredVocabularyWord

@Dao
interface IIgnoredVocabularyWordDao {

    @Query("SELECT * FROM $IGNORED_VOCABULARY_WORD_TABLE_NAME ORDER BY createdAt ASC, id ASC")
    suspend fun getWords(): List<RoomIgnoredVocabularyWord>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWords(words: List<RoomIgnoredVocabularyWord>)

    companion object {

        const val IGNORED_VOCABULARY_WORD_TABLE_NAME = "ignored_vocabulary_words"
    }
}
