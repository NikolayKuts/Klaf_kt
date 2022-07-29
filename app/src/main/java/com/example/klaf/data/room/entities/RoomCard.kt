package com.example.klaf.data.room.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

const val CARD_TABLE_NAME = "cards"

@Entity(tableName = CARD_TABLE_NAME)
data class RoomCard (
    val deckId: Int,
    val nativeWord: String,
    val foreignWord: String,
    val ipa: String,
    @PrimaryKey(autoGenerate = true) val id: Int = 0
)