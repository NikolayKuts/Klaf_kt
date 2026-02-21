package com.kuts.klaf.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kuts.klaf.room.entities.RoomDeck
import com.kuts.klaf.room.entities.RoomDeck.Companion.DECK_TABLE_NAME
import kotlinx.coroutines.flow.Flow

@Dao
interface IDeckDao {

    @Query("SELECT * FROM $DECK_TABLE_NAME")
    fun getObservableDecks(): Flow<List<RoomDeck>>

    @Query("SELECT * FROM $DECK_TABLE_NAME")
    suspend fun getAllDecks(): List<RoomDeck>

    @Query("SELECT * FROM $DECK_TABLE_NAME WHERE id = :deckId")
    fun getObservableDeckById(deckId: Int): Flow<RoomDeck?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeck(deck: RoomDeck)

    @Query("DELETE FROM $DECK_TABLE_NAME WHERE id = :deckId")
    suspend fun deleteDeck(deckId: Int)

    @Query("SELECT * FROM $DECK_TABLE_NAME WHERE id = :deckId")
    suspend fun getDeckById(deckId: Int): RoomDeck?
}
