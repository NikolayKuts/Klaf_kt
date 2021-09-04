package com.example.klaf.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.klaf.domain.pojo.Deck
import com.example.klaf.domain.pojo.TABLE_NAME

@Dao
interface DeckDao {

    @Query("SELECT * FROM $TABLE_NAME")
    fun getAllDecks(): List<Deck>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDeck(deck: Deck)

    @Query("DELETE FROM $TABLE_NAME WHERE id = :deckId")
    fun deleteDeck(deckId: Int)
}