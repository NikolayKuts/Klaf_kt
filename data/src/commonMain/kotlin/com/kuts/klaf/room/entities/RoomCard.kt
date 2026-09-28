package com.kuts.klaf.room.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kuts.domain.entities.WordMeaningInsights
import com.kuts.klaf.room.newSyncId
import com.kuts.klaf.room.entities.RoomCard.Companion.CARD_TABLE_NAME

@Entity(
    tableName = CARD_TABLE_NAME,
    foreignKeys = [
        ForeignKey(
            entity = RoomDeck::class,
            parentColumns = ["id"],
            childColumns = ["deckId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("deckId"), Index(value = ["syncId"], unique = true)],
)
data class RoomCard (
    val deckId: Int,
    val nativeWord: String,
    val foreignWord: String,
    val ipa: String,
    @ColumnInfo(name = "wordMeaningInsightsJson")
    val wordMeaningInsights: WordMeaningInsights,
    @ColumnInfo(name = "mnemonicJson")
    val mnemonicJson: String,
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(defaultValue = "''") val syncId: String = newSyncId(),
    @ColumnInfo(defaultValue = "0") val lastChangedServerRevision: Long = 0L,
) {

    companion object {

        const val CARD_TABLE_NAME = "cards"
    }
}
