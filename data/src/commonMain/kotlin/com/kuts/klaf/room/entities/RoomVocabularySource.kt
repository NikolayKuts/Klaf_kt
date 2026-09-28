package com.kuts.klaf.room.entities

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey
import com.kuts.klaf.room.entities.RoomVocabularySource.Companion.VOCABULARY_SOURCE_TABLE_NAME

@Entity(tableName = VOCABULARY_SOURCE_TABLE_NAME)
data class RoomVocabularySource(
    val title: String,
    val description: String,
    @ColumnInfo(defaultValue = "''")
    val url: String = "",
    val rawText: String,
    val cleanText: String,
    val analysisVersion: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val lastAnalyzedAt: Long?,
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
) {

    companion object {

        const val VOCABULARY_SOURCE_TABLE_NAME = "vocabulary_sources"
    }
}

