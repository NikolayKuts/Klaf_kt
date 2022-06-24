package com.example.klaf.data.room.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.klaf.data.room.entities.DECK_TABLE_NAME
import com.example.klaf.data.room.entities.RoomDeck
import kotlinx.coroutines.flow.Flow

@Dao
interface DeckDao {

    @Query("SELECT * FROM $DECK_TABLE_NAME")
    fun getAllDecks(): Flow<List<RoomDeck>>

    @Query("SELECT * FROM $DECK_TABLE_NAME WHERE id = :deckId")
    fun getObservableDeckById(deckId: Int): Flow<RoomDeck?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDeck(deck: RoomDeck)

    @Query("DELETE FROM $DECK_TABLE_NAME WHERE id = :deckId")
    fun deleteDeck(deckId: Int)

    @Query("SELECT * FROM $DECK_TABLE_NAME WHERE id = :deckId")
    fun getDeckById(deckId: Int): RoomDeck?
}