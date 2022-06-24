package com.example.klaf.data.room.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

const val DECK_TABLE_NAME = "decks"

@Entity(tableName = DECK_TABLE_NAME)
data class RoomDeck(
    val name: String,
    val creationDate: Long,
    @PrimaryKey(autoGenerate = true) var id: Int = 0, // TODO: replace to val
    val cardQuantity: Int = 0,
    val repeatDay: Int = 0,
    val scheduledDate: Long = 0,
    val lastRepeatDate: Long = 0,
    val repeatQuantity: Int = 0,
    val lastRepeatDuration: Long = 0,
    val isLastRepetitionSucceeded: Boolean = true
)