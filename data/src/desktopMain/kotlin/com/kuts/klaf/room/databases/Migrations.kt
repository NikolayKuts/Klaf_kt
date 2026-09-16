package com.kuts.klaf.room.databases

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.kuts.klaf.room.entities.RoomCard.Companion.CARD_TABLE_NAME

object Migrations {

    private const val MNEMONIC_JSON_COLUMN = "mnemonicJson"
    private const val VOCABULARY_SOURCE_TABLE = "vocabulary_sources"
    private const val VOCABULARY_SOURCE_ITEM_TABLE = "vocabulary_source_items"

    val from6To7 = object : Migration(6, 7) {

        override fun migrate(connection: SQLiteConnection) {
            if (!connection.hasColumn(tableName = CARD_TABLE_NAME, columnName = MNEMONIC_JSON_COLUMN)) {
                connection.execSQL(
                    "ALTER TABLE $CARD_TABLE_NAME " +
                        "ADD COLUMN $MNEMONIC_JSON_COLUMN TEXT NOT NULL DEFAULT ''"
                )
            }
        }
    }

    val from7To8 = object : Migration(7, 8) {

        override fun migrate(connection: SQLiteConnection) {
            createVocabularySourceTables { sql -> connection.execSQL(sql) }
        }
    }

    private fun createVocabularySourceTables(execSql: (String) -> Unit) {
        execSql(
            "CREATE TABLE IF NOT EXISTS `$VOCABULARY_SOURCE_TABLE` (" +
                "`title` TEXT NOT NULL, " +
                "`description` TEXT NOT NULL, " +
                "`rawText` TEXT NOT NULL, " +
                "`cleanText` TEXT NOT NULL, " +
                "`analysisVersion` INTEGER NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, " +
                "`updatedAt` INTEGER NOT NULL, " +
                "`lastAnalyzedAt` INTEGER, " +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)"
        )
        execSql(
            "CREATE TABLE IF NOT EXISTS `$VOCABULARY_SOURCE_ITEM_TABLE` (" +
                "`sourceId` INTEGER NOT NULL, " +
                "`foreignWord` TEXT NOT NULL, " +
                "`nativeWord` TEXT NOT NULL, " +
                "`originalText` TEXT NOT NULL, " +
                "`partOfSpeech` TEXT NOT NULL, " +
                "`cefrLevel` TEXT, " +
                "`confidence` TEXT NOT NULL, " +
                "`category` TEXT NOT NULL, " +
                "`status` TEXT NOT NULL, " +
                "`sourceExample` TEXT NOT NULL, " +
                "`explanation` TEXT NOT NULL, " +
                "`knownMeaningsSnapshot` TEXT NOT NULL, " +
                "`alreadyExists` INTEGER NOT NULL, " +
                "`occurrencesJson` TEXT NOT NULL, " +
                "`createdCardId` INTEGER, " +
                "`targetDeckId` INTEGER, " +
                "`firstOccurrenceOrder` INTEGER NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, " +
                "`updatedAt` INTEGER NOT NULL, " +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)"
        )
        execSql(
            "CREATE INDEX IF NOT EXISTS `index_${VOCABULARY_SOURCE_ITEM_TABLE}_sourceId` " +
                "ON `$VOCABULARY_SOURCE_ITEM_TABLE` (`sourceId`)"
        )
        execSql(
            "CREATE INDEX IF NOT EXISTS `index_${VOCABULARY_SOURCE_ITEM_TABLE}_createdCardId` " +
                "ON `$VOCABULARY_SOURCE_ITEM_TABLE` (`createdCardId`)"
        )
    }
}
