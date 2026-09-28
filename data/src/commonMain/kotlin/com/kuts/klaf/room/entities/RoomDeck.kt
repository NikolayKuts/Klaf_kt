package com.kuts.klaf.room.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kuts.klaf.room.newSyncId
import com.kuts.klaf.room.entities.RoomDeck.Companion.DECK_TABLE_NAME
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = DECK_TABLE_NAME, indices = [Index(value = ["syncId"], unique = true)])
data class RoomDeck(
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
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(defaultValue = "''")
    val syncId: String = newSyncId(),
    @ColumnInfo(defaultValue = "0")
    val lastChangedServerRevision: Long = 0L,
    val reviewCurrentDuration: Long? = null,
    val reviewPreviousDuration: Long? = null,
    val reviewScheduledDate: Long? = null,
    val reviewPreviousScheduledDate: Long? = null,
    val reviewLastIterationDate: Long? = null,
    val reviewCurrentSuccessMark: String? = null,
    val reviewPreviousSuccessMark: String? = null,
) {

    companion object {

        const val DECK_TABLE_NAME = "decks"
    }
}
