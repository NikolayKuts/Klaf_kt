package com.example.klaf.domain.pojo

import androidx.room.Entity
import androidx.room.PrimaryKey

const val CARD_TABLE_NAME = "cards"

@Entity(tableName = CARD_TABLE_NAME)
data class Card(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val deckId: Int,
    val nativeWord: String,
    val foreignWord: String,
    val ipa: String
) {

}