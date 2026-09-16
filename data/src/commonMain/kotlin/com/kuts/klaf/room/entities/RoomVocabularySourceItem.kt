package com.kuts.klaf.room.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kuts.klaf.room.entities.RoomVocabularySourceItem.Companion.VOCABULARY_SOURCE_ITEM_TABLE_NAME

@Entity(
    tableName = VOCABULARY_SOURCE_ITEM_TABLE_NAME,
    indices = [
        Index(value = ["sourceId"]),
        Index(value = ["createdCardId"]),
    ],
)
data class RoomVocabularySourceItem(
    val sourceId: Int,
    val foreignWord: String,
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
    val createdAt: Long,
    val updatedAt: Long,
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
) {

    companion object {

        const val VOCABULARY_SOURCE_ITEM_TABLE_NAME = "vocabulary_source_items"
    }
}
