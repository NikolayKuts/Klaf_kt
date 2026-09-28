package com.kuts.klaf.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kuts.klaf.room.entities.RoomDeck
import com.kuts.klaf.room.entities.RoomDeck.Companion.DECK_TABLE_NAME
import kotlinx.coroutines.flow.Flow

data class RoomDeckLocalUpdate(
    val id: Int,
    val name: String,
    val creationDate: Long,
    val repetitionIterationDates: List<Long>,
    val scheduledIterationDates: List<Long>,
    val scheduledDateInterval: Long,
    val repetitionQuantity: Int,
    val cardQuantity: Int,
    val lastFirstRepetitionDuration: Long,
    val lastSecondRepetitionDuration: Long,
    val lastRepetitionIterationDuration: Long,
    val isLastIterationSucceeded: Boolean,
    val reviewCurrentDuration: Long?,
    val reviewPreviousDuration: Long?,
    val reviewScheduledDate: Long?,
    val reviewPreviousScheduledDate: Long?,
    val reviewLastIterationDate: Long?,
    val reviewCurrentSuccessMark: String?,
    val reviewPreviousSuccessMark: String?,
) {
    constructor(deck: RoomDeck) : this(
        id = deck.id,
        name = deck.name,
        creationDate = deck.creationDate,
        repetitionIterationDates = deck.repetitionIterationDates,
        scheduledIterationDates = deck.scheduledIterationDates,
        scheduledDateInterval = deck.scheduledDateInterval,
        repetitionQuantity = deck.repetitionQuantity,
        cardQuantity = deck.cardQuantity,
        lastFirstRepetitionDuration = deck.lastFirstRepetitionDuration,
        lastSecondRepetitionDuration = deck.lastSecondRepetitionDuration,
        lastRepetitionIterationDuration = deck.lastRepetitionIterationDuration,
        isLastIterationSucceeded = deck.isLastIterationSucceeded,
        reviewCurrentDuration = deck.reviewCurrentDuration,
        reviewPreviousDuration = deck.reviewPreviousDuration,
        reviewScheduledDate = deck.reviewScheduledDate,
        reviewPreviousScheduledDate = deck.reviewPreviousScheduledDate,
        reviewLastIterationDate = deck.reviewLastIterationDate,
        reviewCurrentSuccessMark = deck.reviewCurrentSuccessMark,
        reviewPreviousSuccessMark = deck.reviewPreviousSuccessMark,
    )
}

@Dao
interface IDeckDao {

    @Query("SELECT * FROM $DECK_TABLE_NAME")
    fun getObservableDecks(): Flow<List<RoomDeck>>

    @Query("SELECT * FROM $DECK_TABLE_NAME")
    suspend fun getAllDecks(): List<RoomDeck>

    @Query("SELECT * FROM $DECK_TABLE_NAME WHERE id = :deckId")
    fun getObservableDeckById(deckId: Int): Flow<RoomDeck?>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertNewDeck(deck: RoomDeck): Long

    @Update(entity = RoomDeck::class)
    suspend fun updateDeckRow(deck: RoomDeckLocalUpdate): Int

    suspend fun insertDeck(deck: RoomDeck): Long {
        val previous = if (deck.id != 0) getDeckById(deck.id) else null
        return if (previous == null) {
            insertNewDeck(deck)
        } else {
            check(updateDeckRow(RoomDeckLocalUpdate(deck)) == 1)
            deck.id.toLong()
        }
    }

    @Query("UPDATE $DECK_TABLE_NAME SET lastChangedServerRevision = :revision WHERE id = :deckId AND syncId = :syncId")
    suspend fun updateServerRevision(deckId: Int, syncId: String, revision: Long): Int

    @Query("DELETE FROM $DECK_TABLE_NAME WHERE id = :deckId")
    suspend fun deleteDeck(deckId: Int)

    @Query("SELECT * FROM $DECK_TABLE_NAME WHERE id = :deckId")
    suspend fun getDeckById(deckId: Int): RoomDeck?

    @Query("SELECT * FROM $DECK_TABLE_NAME WHERE syncId = :syncId")
    suspend fun getDeckBySyncId(syncId: String): RoomDeck?

    @Query(
        "UPDATE $DECK_TABLE_NAME SET reviewCurrentDuration = :currentDuration, " +
            "reviewPreviousDuration = :previousDuration, reviewScheduledDate = :scheduledDate, " +
            "reviewPreviousScheduledDate = :previousScheduledDate, reviewLastIterationDate = :lastIterationDate, " +
            "reviewCurrentSuccessMark = :currentMark, reviewPreviousSuccessMark = :previousMark " +
            "WHERE id = :deckId",
    )
    suspend fun updateReviewInfo(
        deckId: Int,
        currentDuration: Long?,
        previousDuration: Long?,
        scheduledDate: Long?,
        previousScheduledDate: Long?,
        lastIterationDate: Long?,
        currentMark: String?,
        previousMark: String?,
    ): Int
}
