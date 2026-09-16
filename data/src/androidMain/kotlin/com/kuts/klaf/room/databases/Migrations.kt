package com.kuts.klaf.room.databases

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.execSQL
import com.kuts.klaf.room.entities.RoomCard.Companion.CARD_TABLE_NAME

object Migrations {

    private const val INSIGHTS_JSON_COLUMN = "wordMeaningInsightsJson"
    private const val LEGACY_INSIGHTS_JSON_COLUMN = "geminiWordMeaningInsightsJson"
    private const val MNEMONIC_JSON_COLUMN = "mnemonicJson"
    private const val VOCABULARY_SOURCE_TABLE = "vocabulary_sources"
    private const val VOCABULARY_SOURCE_ITEM_TABLE = "vocabulary_source_items"

    val from3To4 = object : Migration(3, 4) {

        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("UPDATE $CARD_TABLE_NAME SET ipa = '[]'")
        }

        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("UPDATE $CARD_TABLE_NAME SET ipa = '[]'")
        }
    }

    val from4To5 = object : Migration(4, 5) {

        override fun migrate(database: SupportSQLiteDatabase) {
            if (!database.hasColumn(tableName = CARD_TABLE_NAME, columnName = INSIGHTS_JSON_COLUMN)) {
                database.execSQL(
                    "ALTER TABLE $CARD_TABLE_NAME " +
                        "ADD COLUMN $INSIGHTS_JSON_COLUMN TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        override fun migrate(connection: SQLiteConnection) {
            if (!connection.hasColumn(tableName = CARD_TABLE_NAME, columnName = INSIGHTS_JSON_COLUMN)) {
                connection.execSQL(
                    "ALTER TABLE $CARD_TABLE_NAME " +
                        "ADD COLUMN $INSIGHTS_JSON_COLUMN TEXT NOT NULL DEFAULT ''"
                )
            }
        }
    }

    val from5To6 = object : Migration(5, 6) {

        override fun migrate(database: SupportSQLiteDatabase) {
            val hasLegacyColumn = database.hasColumn(
                tableName = CARD_TABLE_NAME,
                columnName = LEGACY_INSIGHTS_JSON_COLUMN,
            )
            val hasNewColumn = database.hasColumn(
                tableName = CARD_TABLE_NAME,
                columnName = INSIGHTS_JSON_COLUMN,
            )

            if (!hasNewColumn) {
                database.execSQL(
                    "ALTER TABLE $CARD_TABLE_NAME " +
                        "ADD COLUMN $INSIGHTS_JSON_COLUMN TEXT NOT NULL DEFAULT ''"
                )
            }

            if (hasLegacyColumn) {
                database.execSQL(
                    "UPDATE $CARD_TABLE_NAME SET $INSIGHTS_JSON_COLUMN = $LEGACY_INSIGHTS_JSON_COLUMN " +
                        "WHERE TRIM($INSIGHTS_JSON_COLUMN) = '' " +
                        "AND $LEGACY_INSIGHTS_JSON_COLUMN IS NOT NULL " +
                        "AND TRIM($LEGACY_INSIGHTS_JSON_COLUMN) != ''"
                )
            }
        }

        override fun migrate(connection: SQLiteConnection) {
            val hasLegacyColumn = connection.hasColumn(
                tableName = CARD_TABLE_NAME,
                columnName = LEGACY_INSIGHTS_JSON_COLUMN,
            )
            val hasNewColumn = connection.hasColumn(
                tableName = CARD_TABLE_NAME,
                columnName = INSIGHTS_JSON_COLUMN,
            )

            if (!hasNewColumn) {
                connection.execSQL(
                    "ALTER TABLE $CARD_TABLE_NAME " +
                        "ADD COLUMN $INSIGHTS_JSON_COLUMN TEXT NOT NULL DEFAULT ''"
                )
            }

            if (hasLegacyColumn) {
                connection.execSQL(
                    "UPDATE $CARD_TABLE_NAME SET $INSIGHTS_JSON_COLUMN = $LEGACY_INSIGHTS_JSON_COLUMN " +
                        "WHERE TRIM($INSIGHTS_JSON_COLUMN) = '' " +
                        "AND $LEGACY_INSIGHTS_JSON_COLUMN IS NOT NULL " +
                        "AND TRIM($LEGACY_INSIGHTS_JSON_COLUMN) != ''"
                )
            }
        }
    }

    val from6To7 = object : Migration(6, 7) {

        override fun migrate(database: SupportSQLiteDatabase) {
            if (!database.hasColumn(tableName = CARD_TABLE_NAME, columnName = MNEMONIC_JSON_COLUMN)) {
                database.execSQL(
                    "ALTER TABLE $CARD_TABLE_NAME " +
                        "ADD COLUMN $MNEMONIC_JSON_COLUMN TEXT NOT NULL DEFAULT ''"
                )
            }
        }

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

        override fun migrate(database: SupportSQLiteDatabase) {
            createVocabularySourceTables { sql -> database.execSQL(sql) }
        }

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
