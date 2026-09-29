package com.kuts.klaf.room.databases

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** Assign identities without resetting source, word, account or sync data. */
object VocabularySourceSyncMigration : Migration(15, 16) {

    override fun migrate(connection: SQLiteConnection) {
        for (table in listOf("vocabulary_sources", "vocabulary_source_items")) {
            connection.execSQL("ALTER TABLE `$table` ADD COLUMN `syncId` TEXT NOT NULL DEFAULT ''")
            connection.execSQL("UPDATE `$table` SET syncId = lower(hex(randomblob(16))) WHERE syncId = ''")
            connection.execSQL("CREATE UNIQUE INDEX `index_${table}_syncId` ON `$table` (`syncId`)")
        }
        connection.execSQL("ALTER TABLE `vocabulary_sources` ADD COLUMN `lastChangedServerRevision` INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("ALTER TABLE `ignored_vocabulary_words` ADD COLUMN `lastChangedServerRevision` INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("ALTER TABLE `sync_checkpoint` ADD COLUMN `vocabularySyncInitialized` INTEGER NOT NULL DEFAULT 0")
    }
}
