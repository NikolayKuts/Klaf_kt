package com.kuts.klaf.room

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.kuts.klaf.room.databases.SyncIdentityMigration
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

class RoomSyncIdentityMigrationTest {

    @Test
    fun `old rows get distinct IDs and zero server revision without using legacy save version`() {
        val tempRoot = Path.of(System.getProperty("java.io.tmpdir")).toRealPath()
        val directory = Files.createTempDirectory(tempRoot, "klaf-id-migration-").toRealPath()
        require(directory.parent == tempRoot && directory.fileName.toString().startsWith("klaf-id-migration-"))
        try {
            BundledSQLiteDriver().open(directory.resolve("legacy.db").toString()).use { connection ->
                connection.execSQL("CREATE TABLE decks(id INTEGER PRIMARY KEY NOT NULL)")
                connection.execSQL("CREATE TABLE cards(id INTEGER PRIMARY KEY NOT NULL)")
                connection.execSQL("CREATE TABLE storage_save_version_table_name(save_version INTEGER NOT NULL)")
                connection.execSQL("INSERT INTO decks(id) VALUES (1), (2)")
                connection.execSQL("INSERT INTO cards(id) VALUES (11), (12)")
                connection.execSQL("INSERT INTO storage_save_version_table_name(save_version) VALUES (4000)")

                SyncIdentityMigration.migrate(connection)

                val syncIds = mutableSetOf<String>()
                listOf("decks", "cards").forEach { table ->
                    connection.prepare("SELECT syncId, lastChangedServerRevision FROM $table ORDER BY id").use { statement ->
                        repeat(2) {
                            assertTrue(statement.step())
                            assertTrue(statement.getText(0).isNotBlank())
                            assertTrue(syncIds.add(statement.getText(0)))
                            assertEquals(0L, statement.getLong(1))
                        }
                        assertEquals(false, statement.step())
                    }
                }
                assertEquals(4, syncIds.size)
                assertFails {
                    connection.execSQL("UPDATE decks SET syncId = '${syncIds.first()}' WHERE id = 2")
                }
            }
        } finally {
            directory.toFile().deleteRecursively()
        }
    }
}
