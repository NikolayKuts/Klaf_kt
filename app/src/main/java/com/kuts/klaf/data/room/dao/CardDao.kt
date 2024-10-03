package com.kuts.klaf.data.room.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kuts.klaf.data.room.entities.RoomCard
import com.kuts.klaf.data.room.entities.RoomCard.Companion.CARD_TABLE_NAME
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {

    @Query("SELECT * FROM $CARD_TABLE_NAME WHERE deckId = :deckId")
    fun getObservableCardsByDeckId(deckId: Int): Flow<List<RoomCard>>

    @Query("SELECT * FROM $CARD_TABLE_NAME WHERE deckId = :deckId")
    fun getCardsByDeckId(deckId: Int): List<RoomCard>

    @Query("SELECT * FROM $CARD_TABLE_NAME")
    fun getAllCards(): List<RoomCard>

    @Query("SELECT * FROM $CARD_TABLE_NAME WHERE id = :cardId")
    fun getObservableCardById(cardId: Int): Flow<RoomCard?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insetCard(card: RoomCard)

    @Query("SELECT COUNT(*) FROM $CARD_TABLE_NAME WHERE deckId = :deckId")
    fun getObservableCardQuantityByDeckId(deckId: Int): LiveData<Int>

    @Query("SELECT COUNT(*) FROM $CARD_TABLE_NAME WHERE deckId = :deckId")
    fun getCardQuantityInDeckAsInt(deckId: Int): Int

    @Query("DELETE FROM $CARD_TABLE_NAME WHERE id = :cardId")
    fun deleteCard(cardId: Int)

    @Query("DELETE FROM $CARD_TABLE_NAME WHERE deckId = :deckId")
    fun deleteCardsByDeckId(deckId: Int)

    @Query("SELECT COUNT(*) FROM $CARD_TABLE_NAME WHERE deckId = :deckId")
    fun getCardQuantityInDeck(deckId: Int): Int

    @Query("SELECT * FROM $CARD_TABLE_NAME WHERE foreignWord = :foreignWord")
    fun getCardsByForeignWord(foreignWord: String): List<RoomCard>
}