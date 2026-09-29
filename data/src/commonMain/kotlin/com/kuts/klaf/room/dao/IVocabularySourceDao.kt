package com.kuts.klaf.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kuts.klaf.room.entities.RoomVocabularySource
import com.kuts.klaf.room.entities.RoomVocabularySource.Companion.VOCABULARY_SOURCE_TABLE_NAME
import kotlinx.coroutines.flow.Flow

@Dao
interface IVocabularySourceDao {

    @Query("SELECT * FROM $VOCABULARY_SOURCE_TABLE_NAME ORDER BY updatedAt DESC")
    fun getObservableSources(): Flow<List<RoomVocabularySource>>

    @Query("SELECT * FROM $VOCABULARY_SOURCE_TABLE_NAME WHERE id = :sourceId")
    fun getObservableSourceById(sourceId: Int): Flow<RoomVocabularySource?>

    @Query("SELECT * FROM $VOCABULARY_SOURCE_TABLE_NAME ORDER BY updatedAt DESC")
    suspend fun getSources(): List<RoomVocabularySource>

    @Query("SELECT * FROM $VOCABULARY_SOURCE_TABLE_NAME WHERE id = :sourceId")
    suspend fun getSourceById(sourceId: Int): RoomVocabularySource?

    @Query("SELECT * FROM $VOCABULARY_SOURCE_TABLE_NAME WHERE syncId = :syncId")
    suspend fun getSourceBySyncId(syncId: String): RoomVocabularySource?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSource(source: RoomVocabularySource): Long

    @Query("DELETE FROM $VOCABULARY_SOURCE_TABLE_NAME WHERE id = :sourceId")
    suspend fun deleteSource(sourceId: Int)
}

