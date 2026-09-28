package com.kuts.klaf.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kuts.klaf.room.entities.RoomVocabularySourceItem
import com.kuts.klaf.room.entities.RoomVocabularySourceItem.Companion.VOCABULARY_SOURCE_ITEM_TABLE_NAME
import kotlinx.coroutines.flow.Flow

@Dao
interface IVocabularySourceItemDao {

    @Query(
        "SELECT * FROM $VOCABULARY_SOURCE_ITEM_TABLE_NAME " +
            "WHERE sourceId = :sourceId " +
            "ORDER BY firstOccurrenceOrder ASC, id ASC"
    )
    fun getObservableItemsBySourceId(sourceId: Int): Flow<List<RoomVocabularySourceItem>>

    @Query(
        "SELECT * FROM $VOCABULARY_SOURCE_ITEM_TABLE_NAME " +
            "ORDER BY sourceId ASC, firstOccurrenceOrder ASC, id ASC"
    )
    fun getObservableItems(): Flow<List<RoomVocabularySourceItem>>

    @Query(
        "SELECT * FROM $VOCABULARY_SOURCE_ITEM_TABLE_NAME " +
            "WHERE sourceId = :sourceId " +
            "ORDER BY firstOccurrenceOrder ASC, id ASC"
    )
    suspend fun getItemsBySourceId(sourceId: Int): List<RoomVocabularySourceItem>

    @Query("SELECT * FROM $VOCABULARY_SOURCE_ITEM_TABLE_NAME ORDER BY id ASC")
    suspend fun getItems(): List<RoomVocabularySourceItem>

    @Query("SELECT * FROM $VOCABULARY_SOURCE_ITEM_TABLE_NAME WHERE id = :itemId")
    suspend fun getItemById(itemId: Int): RoomVocabularySourceItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<RoomVocabularySourceItem>)

    @Query(
        "DELETE FROM $VOCABULARY_SOURCE_ITEM_TABLE_NAME " +
            "WHERE sourceId = :sourceId AND status != :status"
    )
    suspend fun deleteItemsBySourceIdExceptStatus(sourceId: Int, status: String)

    @Query("DELETE FROM $VOCABULARY_SOURCE_ITEM_TABLE_NAME WHERE sourceId = :sourceId")
    suspend fun deleteItemsBySourceId(sourceId: Int)

    @Query("DELETE FROM $VOCABULARY_SOURCE_ITEM_TABLE_NAME WHERE id = :itemId")
    suspend fun deleteItem(itemId: Int)
}
