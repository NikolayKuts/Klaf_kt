package com.example.klaf.data.dao

import androidx.room.*
import com.example.klaf.domain.pojo.Deck
import com.example.klaf.domain.pojo.DECK_TABLE_NAME

@Dao
interface DeckDao {

    @Query("SELECT * FROM $DECK_TABLE_NAME")
    fun getAllDecks(): List<Deck>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDeck(deck: Deck)

    @Query("DELETE FROM $DECK_TABLE_NAME WHERE id = :deckId")
    fun deleteDeck(deckId: Int)

    @Query("SELECT * FROM $DECK_TABLE_NAME WHERE id = :deckId")
    fun getDeckById(deckId: Int): Deck
}