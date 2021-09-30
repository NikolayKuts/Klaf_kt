package com.example.klaf.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.klaf.domain.pojo.CARD_TABLE_NAME
import com.example.klaf.domain.pojo.Card

@Dao
interface CardDao {

    @Query("SELECT * FROM $CARD_TABLE_NAME WHERE deckId = :deckId")
    fun getAllCardByDeckId(deckId: Int): List<Card>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insetCard(card: Card)

    @Query("SELECT COUNT(*) FROM $CARD_TABLE_NAME WHERE deckId = :deckId")
    fun getCardQuantityByDeckId(deckId: Int): LiveData<Int>

    @Query("SELECT COUNT(*) FROM $CARD_TABLE_NAME WHERE deckId = :deckId")
    fun getCardQuantityAsInt(deckId: Int): Int
}