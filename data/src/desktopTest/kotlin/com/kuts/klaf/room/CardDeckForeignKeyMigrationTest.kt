package com.kuts.klaf.room

import androidx.room.Room
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.kuts.klaf.room.databases.DeckCardIntegrityMigration
import com.kuts.klaf.room.databases.KlafRoomDatabase
import com.kuts.klaf.room.databases.SyncIdentityMigration
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private const val VERSION_8_SCHEMA_PATH = "schemas/com.kuts.klaf.room.databases.KlafRoomDatabase/8.json"

class CardDeckForeignKeyMigrationTest {

    @Test
    fun `Room opens a complete version 8 database through both migrations without losing deck or card`() = withDatabasePath { path ->
        BundledSQLiteDriver().open(path.toString()).use { connection ->
            createVersion8Schema(connection)
            connection.execSQL(
                "INSERT INTO decks(name, creationDate, repetitionIterationDates, scheduledIterationDates, " +
                    "scheduledDateInterval, repetitionQuantity, cardQuantity, lastFirstRepetitionDuration, " +
                    "lastSecondRepetitionDuration, lastRepetitionIterationDuration, isLastIterationSucceeded, id) " +
                    "VALUES ('Words', 1000, '[]', '[]', 0, 0, 1, 0, 0, 0, 1, 7)",
            )
            insertCard(connection, id = 40, deckId = 7)
            connection.execSQL("PRAGMA user_version = 8")
        }

        val database = Room.databaseBuilder<KlafRoomDatabase>(
            name = path.toString(),
        ).addMigrations(DeckCardIntegrityMigration, SyncIdentityMigration)
            .setDriver(BundledSQLiteDriver())
            .build()

        try {
            runBlocking {
                assertEquals(7, database.deckDao().getAllDecks().single().id)
                assertEquals(40, database.cardDao().getAllCards().single().id)
            }
        } finally {
            database.close()
        }
    }

    @Test
    fun `Room refuses a version 8 database with an orphan without changing its version or rows`() =
        withDatabasePath { path ->
            BundledSQLiteDriver().open(path.toString()).use { connection ->
                createVersion8Schema(connection)
                insertCard(connection, id = 40, deckId = 404)
                connection.execSQL("PRAGMA user_version = 8")
            }

            val database = Room.databaseBuilder<KlafRoomDatabase>(
                name = path.toString(),
            ).addMigrations(DeckCardIntegrityMigration, SyncIdentityMigration)
                .setDriver(BundledSQLiteDriver())
                .build()
            try {
                assertFailsWith<IllegalStateException> {
                    runBlocking { database.cardDao().getAllCards() }
                }
            } finally {
                database.close()
            }

            BundledSQLiteDriver().open(path.toString()).use { connection ->
                connection.prepare("PRAGMA user_version").use { statement ->
                    assertTrue(statement.step())
                    assertEquals(8L, statement.getLong(0))
                }
                connection.prepare("SELECT id, deckId FROM cards").use { statement ->
                    assertTrue(statement.step())
                    assertEquals(40L, statement.getLong(0))
                    assertEquals(404L, statement.getLong(1))
                }
            }
        }

    @Test
    fun `version 8 card rows survive the foreign key migration`() = withLegacyDatabase { connection ->
        connection.execSQL("INSERT INTO decks(id) VALUES (7)")
        insertCard(connection, id = 40, deckId = 7)

        DeckCardIntegrityMigration.migrate(connection)

        connection.prepare("SELECT id, deckId, nativeWord, mnemonicJson FROM cards").use { statement ->
            assertTrue(statement.step())
            assertEquals(40L, statement.getLong(0))
            assertEquals(7L, statement.getLong(1))
            assertEquals("native", statement.getText(2))
            assertEquals("{\"hint\":\"keep\"}", statement.getText(3))
            assertEquals(false, statement.step())
        }
        connection.prepare("PRAGMA foreign_key_list('cards')").use { statement ->
            assertTrue(statement.step())
            assertEquals("decks", statement.getText(2))
            assertEquals("deckId", statement.getText(3))
            assertEquals("CASCADE", statement.getText(6))
        }
    }

    @Test
    fun `orphan card stops migration before old rows are changed`() = withLegacyDatabase { connection ->
        insertCard(connection, id = 40, deckId = 404)

        val failure = assertFailsWith<IllegalStateException> {
            DeckCardIntegrityMigration.migrate(connection)
        }

        assertTrue(failure.message?.contains("40") == true)
        assertTrue(failure.message?.contains("404") == true)
        connection.prepare("SELECT id, deckId FROM cards").use { statement ->
            assertTrue(statement.step())
            assertEquals(40L, statement.getLong(0))
            assertEquals(404L, statement.getLong(1))
        }
        connection.prepare("PRAGMA foreign_key_list('cards')").use { statement ->
            assertEquals(false, statement.step())
        }
    }

    private fun withLegacyDatabase(block: (SQLiteConnection) -> Unit) = withDatabasePath { path ->
        BundledSQLiteDriver().open(path.toString()).use { connection ->
            connection.execSQL("CREATE TABLE decks(id INTEGER PRIMARY KEY NOT NULL)")
            connection.execSQL(
                "CREATE TABLE cards(" +
                    "deckId INTEGER NOT NULL, nativeWord TEXT NOT NULL, foreignWord TEXT NOT NULL, " +
                    "ipa TEXT NOT NULL, wordMeaningInsightsJson TEXT NOT NULL, mnemonicJson TEXT NOT NULL, " +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)",
            )
            block(connection)
        }
    }

    private fun withDatabasePath(block: (Path) -> Unit) {
        val tempRoot = Path.of(System.getProperty("java.io.tmpdir")).toRealPath()
        val directory = Files.createTempDirectory(tempRoot, "klaf-fk-migration-").toRealPath()
        require(directory.parent == tempRoot && directory.fileName.toString().startsWith("klaf-fk-migration-"))
        try {
            block(directory.resolve("legacy.db"))
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    private fun createVersion8Schema(connection: SQLiteConnection) {
        val modulePath = Path.of(VERSION_8_SCHEMA_PATH)
        val schemaPath = if (Files.exists(modulePath)) modulePath else Path.of("data").resolve(modulePath)
        val database = Json.parseToJsonElement(Files.readString(schemaPath))
            .jsonObject.getValue("database").jsonObject
        database.getValue("entities").jsonArray.forEach { item ->
            val entity = item.jsonObject
            val tableName = entity.getValue("tableName").jsonPrimitive.content
            connection.execSQL(entity.getValue("createSql").jsonPrimitive.content.replace("$" + "{TABLE_NAME}", tableName))
            entity["indices"]?.jsonArray?.forEach { index ->
                connection.execSQL(index.jsonObject.getValue("createSql").jsonPrimitive.content.replace("$" + "{TABLE_NAME}", tableName))
            }
        }
    }

    private fun insertCard(connection: SQLiteConnection, id: Int, deckId: Int) {
        connection.execSQL(
            "INSERT INTO cards(deckId, nativeWord, foreignWord, ipa, wordMeaningInsightsJson, mnemonicJson, id) " +
                "VALUES ($deckId, 'native', 'foreign', '[]', '{}', '{\"hint\":\"keep\"}', $id)",
        )
    }
}
