package com.example.data.room.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.room.entities.RoomDeck.Companion.DECK_TABLE_NAME
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = DECK_TABLE_NAME)
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
) {

    companion object {

        const val DECK_TABLE_NAME = "decks"
    }
}