package com.kuts.klaf.room.databases

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.kuts.klaf.room.newSyncId

private const val INTEGRATION_SOURCE_TABLE_SQL = """CREATE TABLE `vocabulary_sources_integrated` (`title` TEXT NOT NULL, `description` TEXT NOT NULL, `url` TEXT NOT NULL DEFAULT '', `rawText` TEXT NOT NULL, `cleanText` TEXT NOT NULL, `analysisVersion` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `lastAnalyzedAt` INTEGER, `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)"""
private const val INTEGRATION_SOURCE_COLUMNS = "title,description,url,rawText,cleanText,analysisVersion,createdAt,updatedAt,lastAnalyzedAt,id"

private val integrationTableStatements = listOf(
    """CREATE TABLE IF NOT EXISTS `pending_sync_operations` (`accountId` TEXT NOT NULL, `operationId` TEXT NOT NULL, `baseRevision` INTEGER NOT NULL, `operationJson` TEXT NOT NULL, `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)""",
    """CREATE INDEX IF NOT EXISTS `index_pending_sync_operations_accountId` ON `pending_sync_operations` (`accountId`)""",
    """CREATE UNIQUE INDEX IF NOT EXISTS `index_pending_sync_operations_operationId` ON `pending_sync_operations` (`operationId`)""",
    """CREATE TABLE IF NOT EXISTS `sync_checkpoint` (`confirmedRevision` INTEGER NOT NULL, `id` INTEGER NOT NULL, PRIMARY KEY(`id`))""",
    """CREATE TABLE IF NOT EXISTS `sync_conflict_snapshot` (`accountId` TEXT NOT NULL, `baseRevision` INTEGER NOT NULL, `responseJson` TEXT NOT NULL, `id` INTEGER NOT NULL, PRIMARY KEY(`id`))""",
    """CREATE TABLE IF NOT EXISTS `ignored_vocabulary_words` (`language` TEXT NOT NULL, `foreignWord` TEXT NOT NULL, `nativeWord` TEXT NOT NULL, `languageKey` TEXT NOT NULL, `foreignWordKey` TEXT NOT NULL, `nativeWordKey` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)""",
    """CREATE UNIQUE INDEX IF NOT EXISTS `index_ignored_vocabulary_words_languageKey_foreignWordKey_nativeWordKey` ON `ignored_vocabulary_words` (`languageKey`, `foreignWordKey`, `nativeWordKey`)""",
)

/** Versions 9-12 were independently used by the storage and voice branches. */
object BranchIntegrationMigrations {

    val all: Array<Migration> = (8..14).map<Int, Migration> { version ->
        object : Migration(version, 15) {
            override fun migrate(connection: SQLiteConnection) {
                normalize(connection)
            }
        }
    }.toTypedArray() + VocabularySourceSyncMigration

    private fun normalize(connection: SQLiteConnection) {
        if ("syncId" !in columns(connection, "cards")) {
            DeckCardIntegrityMigration.migrate(connection)
        }
        for (table in listOf("decks", "cards")) {
            addColumn(connection, table, "syncId", "TEXT NOT NULL DEFAULT ''")
            addColumn(connection, table, "lastChangedServerRevision", "INTEGER NOT NULL DEFAULT 0")
            val ids = mutableListOf<Long>()
            connection.prepare("SELECT id FROM `$table` WHERE syncId = ''").use { statement ->
                while (statement.step()) ids += statement.getLong(0)
            }
            for (id in ids) {
                connection.prepare("UPDATE `$table` SET syncId = ? WHERE id = ?").use { statement ->
                    statement.bindText(1, newSyncId())
                    statement.bindLong(2, id)
                    statement.step()
                }
            }
            connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_${table}_syncId` ON `$table` (`syncId`)")
        }
        for (column in listOf("reviewCurrentDuration", "reviewPreviousDuration", "reviewScheduledDate",
            "reviewPreviousScheduledDate", "reviewLastIterationDate")) {
            addColumn(connection, "decks", column, "INTEGER")
        }
        for (column in listOf("reviewCurrentSuccessMark", "reviewPreviousSuccessMark")) {
            addColumn(connection, "decks", column, "TEXT")
        }
        addColumn(connection, "vocabulary_sources", "url", "TEXT NOT NULL DEFAULT ''")
        normalizeSourceUrlDefault(connection)
        addColumn(connection, "vocabulary_source_items", "transcription", "TEXT NOT NULL DEFAULT ''")
        addColumn(connection, "vocabulary_source_items", "isEdited", "INTEGER NOT NULL DEFAULT 0")
        addColumn(connection, "vocabulary_source_items", "language", "TEXT NOT NULL DEFAULT ''")
        integrationTableStatements.forEach(connection::execSQL)
    }

    private fun addColumn(connection: SQLiteConnection, table: String, column: String, type: String) {
        if (column !in columns(connection, table)) {
            connection.execSQL("ALTER TABLE `$table` ADD COLUMN `$column` $type")
        }
    }

    private fun normalizeSourceUrlDefault(connection: SQLiteConnection) {
        val needsDefault = connection.prepare("PRAGMA table_info(`vocabulary_sources`)").use { statement ->
            var missing = false
            while (statement.step()) {
                if (statement.getText(1) == "url") missing = statement.isNull(4)
            }
            missing
        }
        if (!needsDefault) return
        // The voice branch had the same column, but no SQL default. Room validates both.
        connection.execSQL(INTEGRATION_SOURCE_TABLE_SQL)
        connection.execSQL("INSERT INTO vocabulary_sources_integrated ($INTEGRATION_SOURCE_COLUMNS) " +
            "SELECT $INTEGRATION_SOURCE_COLUMNS FROM vocabulary_sources")
        connection.execSQL("DROP TABLE vocabulary_sources")
        connection.execSQL("ALTER TABLE vocabulary_sources_integrated RENAME TO vocabulary_sources")
    }

    private fun columns(connection: SQLiteConnection, table: String): Set<String> = buildSet {
        connection.prepare("PRAGMA table_info(`$table`)").use { statement ->
            while (statement.step()) add(statement.getText(1))
        }
    }
}
