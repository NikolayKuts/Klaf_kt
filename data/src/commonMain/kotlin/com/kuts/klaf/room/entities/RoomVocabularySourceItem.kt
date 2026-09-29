package com.kuts.klaf.room.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kuts.klaf.room.newSyncId
import com.kuts.klaf.room.entities.RoomVocabularySourceItem.Companion.VOCABULARY_SOURCE_ITEM_TABLE_NAME

@Entity(
    tableName = VOCABULARY_SOURCE_ITEM_TABLE_NAME,
    indices = [
        Index(value = ["sourceId"]),
        Index(value = ["createdCardId"]),
        Index(value = ["syncId"], unique = true),
    ],
)
data class RoomVocabularySourceItem(
    val sourceId: Int,
    @ColumnInfo(defaultValue = "''")
    val language: String,
    val foreignWord: String,
    @ColumnInfo(defaultValue = "''")
    val transcription: String,
    val nativeWord: String,
    val originalText: String,
    val partOfSpeech: String,
    val cefrLevel: String?,
    val confidence: String,
    val category: String,
    val status: String,
    val sourceExample: String,
    val explanation: String,
    val knownMeaningsSnapshot: String,
    val alreadyExists: Boolean,
    val occurrencesJson: String,
    val createdCardId: Int?,
    val targetDeckId: Int?,
    val firstOccurrenceOrder: Int,
    @ColumnInfo(defaultValue = "0")
    val isEdited: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(defaultValue = "''")
    val syncId: String = newSyncId(),
) {

    companion object {

        const val VOCABULARY_SOURCE_ITEM_TABLE_NAME = "vocabulary_source_items"
    }
}
