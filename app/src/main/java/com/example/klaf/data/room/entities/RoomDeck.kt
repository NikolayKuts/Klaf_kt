package com.example.klaf.data.room.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.example.klaf.data.room.entities.RoomDeck.Companion.DECK_TABLE_NAME
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
@Entity(tableName = DECK_TABLE_NAME)
data class RoomDeck(
    val name: String,
    val creationDate: Long,
    val repetitionDates: List<Long>,
    val scheduledDates: List<Long>,
    val scheduledDateInterval: Long,
    val repetitionQuantity: Int,
    val cardQuantity: Int,
    val lastFirstRepetitionDuration: Long = 0,
    val lastSecondRepetitionDuration: Long = 0,
    val lastRepetitionIterationDuration: Long,
    val isLastIterationSucceeded: Boolean = true,
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
) {

    companion object {

        const val DECK_TABLE_NAME = "decks"
    }

}

//const val DECK_TABLE_NAME = "decks"

//@Entity(tableName = DECK_TABLE_NAME)
//data class RoomDeck(
//    val name: String,
//    val creationDate: Long,
//    @PrimaryKey(autoGenerate = true) var id: Int = 0, // TODO: replace to val
//    val cardQuantity: Int = 0,
//    val repeatDay: Int = 0,
//    val scheduledDate: Long = 0,
//    val lastRepeatDate: Long = 0,
//    val repeatQuantity: Int = 0,
//    val lastRepeatDuration: Long = 0,
//    val isLastRepetitionSucceeded: Boolean = true
//)

