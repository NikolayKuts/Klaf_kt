package com.kuts.klaf.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kuts.klaf.room.entities.RoomCard
import com.kuts.klaf.room.entities.RoomCard.Companion.CARD_TABLE_NAME
import kotlinx.coroutines.flow.Flow

@Dao
interface ICardDao {

    @Query("SELECT * FROM $CARD_TABLE_NAME WHERE deckId = :deckId")
    fun getObservableCardsByDeckId(deckId: Int): Flow<List<RoomCard>>

    @Query("SELECT * FROM $CARD_TABLE_NAME WHERE deckId = :deckId")
    suspend fun getCardsByDeckId(deckId: Int): List<RoomCard>

    @Query("SELECT * FROM $CARD_TABLE_NAME")
    suspend fun getAllCards(): List<RoomCard>

    @Query("SELECT * FROM $CARD_TABLE_NAME WHERE id = :cardId")
    fun getObservableCardById(cardId: Int): Flow<RoomCard?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insetCard(card: RoomCard)

    @Query("SELECT COUNT(*) FROM $CARD_TABLE_NAME WHERE deckId = :deckId")
    suspend fun getCardQuantityInDeckAsInt(deckId: Int): Int

    @Query("DELETE FROM $CARD_TABLE_NAME WHERE id = :cardId")
    suspend fun deleteCard(cardId: Int)

    @Query("DELETE FROM $CARD_TABLE_NAME WHERE deckId = :deckId")
    suspend fun deleteCardsByDeckId(deckId: Int)

    @Query("SELECT COUNT(*) FROM $CARD_TABLE_NAME WHERE deckId = :deckId")
    suspend fun getCardQuantityInDeck(deckId: Int): Int

    @Query("SELECT * FROM $CARD_TABLE_NAME WHERE foreignWord = :foreignWord")
    suspend fun getCardsByForeignWord(foreignWord: String): List<RoomCard>
}
