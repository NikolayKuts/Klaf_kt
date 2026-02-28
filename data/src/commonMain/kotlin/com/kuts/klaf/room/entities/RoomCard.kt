package com.kuts.klaf.room.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kuts.domain.entities.WordMeaningInsights
import com.kuts.klaf.room.entities.RoomCard.Companion.CARD_TABLE_NAME

@Entity(tableName = CARD_TABLE_NAME)
data class RoomCard (
    val deckId: Int,
    val nativeWord: String,
    val foreignWord: String,
    val ipa: String,
    @ColumnInfo(name = "wordMeaningInsightsJson")
    val wordMeaningInsights: WordMeaningInsights,
    @PrimaryKey(autoGenerate = true) val id: Int = 0
) {

    companion object {

        const val CARD_TABLE_NAME = "cards"
    }
}
