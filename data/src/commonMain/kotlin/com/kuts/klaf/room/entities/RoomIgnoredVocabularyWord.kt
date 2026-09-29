package com.kuts.klaf.room.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import com.kuts.klaf.room.entities.RoomIgnoredVocabularyWord.Companion.IGNORED_VOCABULARY_WORD_TABLE_NAME

@Entity(
    tableName = IGNORED_VOCABULARY_WORD_TABLE_NAME,
    indices = [
        Index(
            value = ["languageKey", "foreignWordKey", "nativeWordKey"],
            unique = true,
        ),
    ],
)
data class RoomIgnoredVocabularyWord(
    val language: String,
    val foreignWord: String,
    val nativeWord: String,
    val languageKey: String,
    val foreignWordKey: String,
    val nativeWordKey: String,
    val createdAt: Long,
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val lastChangedServerRevision: Long = 0L,
) {

    companion object {

        const val IGNORED_VOCABULARY_WORD_TABLE_NAME = "ignored_vocabulary_words"
    }
}
