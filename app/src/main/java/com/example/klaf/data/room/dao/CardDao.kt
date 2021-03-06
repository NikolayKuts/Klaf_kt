package com.example.klaf.data.room.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.klaf.data.room.entities.CARD_TABLE_NAME
import com.example.klaf.data.room.entities.RoomCard
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {

    @Query("SELECT * FROM $CARD_TABLE_NAME WHERE deckId = :deckId")
    fun getCardsByDeckId(deckId: Int): Flow<List<RoomCard>>

    @Query("SELECT * FROM $CARD_TABLE_NAME WHERE deckId = :deckId")
    fun getCardListByDeckId(deckId: Int): List<RoomCard>

    @Query("SELECT * FROM $CARD_TABLE_NAME WHERE id = :cardId")
    fun getObservableCardById(cardId: Int): Flow<RoomCard?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insetCard(card: RoomCard)

    @Query("SELECT COUNT(*) FROM $CARD_TABLE_NAME WHERE deckId = :deckId")
    fun getCardQuantityByDeckId(deckId: Int): LiveData<Int>

    @Query("SELECT COUNT(*) FROM $CARD_TABLE_NAME WHERE deckId = :deckId")
    fun getCardQuantityInDeckAsInt(deckId: Int): Int

    @Query("DELETE FROM $CARD_TABLE_NAME WHERE id = :cardId")
    fun deleteCard(cardId: Int)

    @Query("DELETE FROM $CARD_TABLE_NAME WHERE deckId = :deckId")
    fun deleteCardsByDeckId(deckId: Int)

    @Query("SELECT COUNT(*) FROM $CARD_TABLE_NAME WHERE deckId = :deckId")
    fun getCardQuantityInDeck(deckId: Int): Int
}