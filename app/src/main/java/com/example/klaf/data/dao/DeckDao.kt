package com.example.klaf.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.klaf.domain.pojo.Deck
import com.example.klaf.domain.pojo.DECK_TABLE_NAME
import kotlinx.coroutines.flow.Flow

@Dao
interface DeckDao {

    @Query("SELECT * FROM $DECK_TABLE_NAME")
    fun getAllDecks(): Flow<List<Deck>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDeck(deck: Deck)

    @Query("DELETE FROM $DECK_TABLE_NAME WHERE id = :deckId")
    fun deleteDeck(deckId: Int)

    @Query("SELECT * FROM $DECK_TABLE_NAME WHERE id = :deckId")
    fun getDeckById(deckId: Int): Deck?

    @Query("SELECT * FROM $DECK_TABLE_NAME WHERE id = :deckId")
    fun getObservableDeckById(deckId: Int): LiveData<Deck?>
}