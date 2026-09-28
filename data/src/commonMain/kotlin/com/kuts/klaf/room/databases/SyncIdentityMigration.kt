package com.kuts.klaf.room.databases

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.kuts.klaf.room.newSyncId

internal object SyncIdentityMigration : Migration(9, 10) {

    override fun migrate(connection: SQLiteConnection) {
        addIdentityColumns(connection, "decks")
        addIdentityColumns(connection, "cards")
        assignSyncIds(connection, "decks")
        assignSyncIds(connection, "cards")
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_decks_syncId` ON `decks` (`syncId`)")
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_cards_syncId` ON `cards` (`syncId`)")
    }

    private fun addIdentityColumns(connection: SQLiteConnection, table: String) {
        connection.execSQL("ALTER TABLE `$table` ADD COLUMN `syncId` TEXT NOT NULL DEFAULT ''")
        connection.execSQL("ALTER TABLE `$table` ADD COLUMN `lastChangedServerRevision` INTEGER NOT NULL DEFAULT 0")
    }

    private fun assignSyncIds(connection: SQLiteConnection, table: String) {
        val ids = mutableListOf<Long>()
        connection.prepare("SELECT id FROM `$table`").use { statement ->
            while (statement.step()) ids += statement.getLong(0)
        }
        connection.prepare("UPDATE `$table` SET `syncId` = ? WHERE id = ?").use { statement ->
            ids.forEach { id ->
                statement.bindText(1, newSyncId())
                statement.bindLong(2, id)
                statement.step()
                statement.reset()
            }
        }
    }
}
