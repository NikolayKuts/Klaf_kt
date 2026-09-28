package com.kuts.klaf.room.databases

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.kuts.klaf.room.entities.RoomCard.Companion.CARD_TABLE_NAME
import com.kuts.klaf.room.entities.RoomIgnoredVocabularyWord.Companion.IGNORED_VOCABULARY_WORD_TABLE_NAME

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

    val from8To9 = object : Migration(8, 9) {

        override fun migrate(connection: SQLiteConnection) {
            addVocabularySourceItemTranscriptionColumn { sql -> connection.execSQL(sql) }
        }
    }

    val from9To10 = object : Migration(9, 10) {

        override fun migrate(connection: SQLiteConnection) {
            addVocabularySourceItemIsEditedColumn { sql -> connection.execSQL(sql) }
        }
    }

    val from10To11 = object : Migration(10, 11) {

        override fun migrate(connection: SQLiteConnection) {
            addVocabularySourceItemLanguageColumn { sql -> connection.execSQL(sql) }
            createIgnoredVocabularyWordsTable { sql -> connection.execSQL(sql) }
        }
    }

    val from11To12 = object : Migration(11, 12) {

        override fun migrate(connection: SQLiteConnection) {
            addVocabularySourceUrlColumn { sql -> connection.execSQL(sql) }
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

    private fun addVocabularySourceItemTranscriptionColumn(execSql: (String) -> Unit) {
        execSql(
            "ALTER TABLE `$VOCABULARY_SOURCE_ITEM_TABLE` " +
                "ADD COLUMN `transcription` TEXT NOT NULL DEFAULT ''"
        )
    }

    private fun addVocabularySourceItemIsEditedColumn(execSql: (String) -> Unit) {
        execSql(
            "ALTER TABLE `$VOCABULARY_SOURCE_ITEM_TABLE` " +
                "ADD COLUMN `isEdited` INTEGER NOT NULL DEFAULT 0"
        )
    }

    private fun addVocabularySourceItemLanguageColumn(execSql: (String) -> Unit) {
        execSql(
            "ALTER TABLE `$VOCABULARY_SOURCE_ITEM_TABLE` " +
                "ADD COLUMN `language` TEXT NOT NULL DEFAULT ''"
        )
    }

    private fun createIgnoredVocabularyWordsTable(execSql: (String) -> Unit) {
        execSql(
            "CREATE TABLE IF NOT EXISTS `$IGNORED_VOCABULARY_WORD_TABLE_NAME` (" +
                "`language` TEXT NOT NULL, " +
                "`foreignWord` TEXT NOT NULL, " +
                "`nativeWord` TEXT NOT NULL, " +
                "`languageKey` TEXT NOT NULL, " +
                "`foreignWordKey` TEXT NOT NULL, " +
                "`nativeWordKey` TEXT NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, " +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)"
        )
        execSql(
            "CREATE UNIQUE INDEX IF NOT EXISTS " +
                "`index_${IGNORED_VOCABULARY_WORD_TABLE_NAME}_languageKey_foreignWordKey_nativeWordKey` " +
                "ON `$IGNORED_VOCABULARY_WORD_TABLE_NAME` " +
                "(`languageKey`, `foreignWordKey`, `nativeWordKey`)"
        )
    }

    private fun addVocabularySourceUrlColumn(execSql: (String) -> Unit) {
        execSql(
            "ALTER TABLE `$VOCABULARY_SOURCE_TABLE` " +
                "ADD COLUMN `url` TEXT NOT NULL DEFAULT ''"
        )
    }
}
