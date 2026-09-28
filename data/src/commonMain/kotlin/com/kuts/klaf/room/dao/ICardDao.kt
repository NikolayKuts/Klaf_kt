package com.kuts.klaf.room.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kuts.domain.entities.WordMeaningInsights
import com.kuts.klaf.room.entities.RoomCard
import com.kuts.klaf.room.entities.RoomCard.Companion.CARD_TABLE_NAME
import kotlinx.coroutines.flow.Flow

data class RoomCardLocalUpdate(
    val id: Int,
    val deckId: Int,
    val nativeWord: String,
    val foreignWord: String,
    val ipa: String,
    @ColumnInfo(name = "wordMeaningInsightsJson")
    val wordMeaningInsights: WordMeaningInsights,
    @ColumnInfo(name = "mnemonicJson")
    val mnemonicJson: String,
) {
    constructor(card: RoomCard) : this(
        id = card.id,
        deckId = card.deckId,
        nativeWord = card.nativeWord,
        foreignWord = card.foreignWord,
        ipa = card.ipa,
        wordMeaningInsights = card.wordMeaningInsights,
        mnemonicJson = card.mnemonicJson,
    )
}

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

    @Query("SELECT * FROM $CARD_TABLE_NAME WHERE id = :cardId")
    suspend fun getCardById(cardId: Int): RoomCard?

    @Query("SELECT * FROM $CARD_TABLE_NAME WHERE syncId = :syncId")
    suspend fun getCardBySyncId(syncId: String): RoomCard?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertNewCard(card: RoomCard): Long

    @Update(entity = RoomCard::class)
    suspend fun updateCardRow(card: RoomCardLocalUpdate): Int

    suspend fun insetCard(card: RoomCard): Long {
        val previous = if (card.id != 0) getCardById(card.id) else null
        return if (previous == null) {
            insertNewCard(card)
        } else {
            check(updateCardRow(RoomCardLocalUpdate(card)) == 1)
            card.id.toLong()
        }
    }

    @Query("UPDATE $CARD_TABLE_NAME SET lastChangedServerRevision = :revision WHERE id = :cardId AND syncId = :syncId")
    suspend fun updateServerRevision(cardId: Int, syncId: String, revision: Long): Int

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
