package com.example.klaf.domain.pojo

import androidx.room.Entity
import androidx.room.PrimaryKey

const val DECK_TABLE_NAME = "decks"

@Entity(tableName = DECK_TABLE_NAME)
data class Deck(
    val name: String,
    @PrimaryKey(autoGenerate = true) var id: Int = 0,
    var cardQuantity: Int = 0,
    val creationData: Long = 0,
    val repeatDay: Long = 0,
    val scheduledDate: Long = 0,
    val lastRepeatDate: Long = 0,
    val repeatQuantity: Int = 0,
    val lastRepeatDuration: Long = 0,
    val isLastRepetitionSucceeded: Boolean = true
)