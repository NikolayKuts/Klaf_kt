package com.kuts.klaf.room

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.kuts.klaf.room.databases.KlafRoomDatabase
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private const val REVIEW_PREVIOUS_SCHEMA = "schemas/com.kuts.klaf.room.databases.KlafRoomDatabase/11.json"

class RoomReviewSummaryMigrationTest {

    @Test
    fun `version eleven deck and card survive review summary migration`() {
        val tempRoot = Path.of(System.getProperty("java.io.tmpdir")).toRealPath()
        val directory = Files.createTempDirectory(tempRoot, "klaf-review-migration-").toRealPath()
        require(directory.parent == tempRoot && directory.fileName.toString().startsWith("klaf-review-migration-"))
        val databaseFile = directory.resolve("legacy.db")
        try {
            BundledSQLiteDriver().open(databaseFile.toString()).use { connection ->
                val schemaPath = Path.of(REVIEW_PREVIOUS_SCHEMA).let { path ->
                    if (Files.exists(path)) path else Path.of("data").resolve(path)
                }
                val schema = Json.parseToJsonElement(Files.readString(schemaPath))
                    .jsonObject.getValue("database").jsonObject
                schema.getValue("entities").jsonArray.forEach { item ->
                    val entity = item.jsonObject
                    val name = entity.getValue("tableName").jsonPrimitive.content
                    connection.execSQL(entity.getValue("createSql").jsonPrimitive.content
                        .replace("${'$'}{TABLE_NAME}", name))
                    entity["indices"]?.jsonArray?.forEach { index ->
                        connection.execSQL(index.jsonObject.getValue("createSql").jsonPrimitive.content
                            .replace("${'$'}{TABLE_NAME}", name))
                    }
                }
                schema.getValue("setupQueries").jsonArray.forEach { query ->
                    connection.execSQL(query.jsonPrimitive.content)
                }
                connection.execSQL(
                    "INSERT INTO decks(name, creationDate, repetitionIterationDates, scheduledIterationDates, " +
                        "scheduledDateInterval, repetitionQuantity, cardQuantity, lastFirstRepetitionDuration, " +
                        "lastSecondRepetitionDuration, lastRepetitionIterationDuration, isLastIterationSucceeded, " +
                        "id, syncId, lastChangedServerRevision) VALUES " +
                        "('existing', 1, '[]', '[200]', 300, 2, 1, 11, 12, 13, 1, 1, 'deck-a', 0)",
                )
                connection.execSQL(
                    "INSERT INTO cards(deckId, nativeWord, foreignWord, ipa, wordMeaningInsightsJson, " +
                        "mnemonicJson, id, syncId, lastChangedServerRevision) VALUES " +
                        "(1, 'native', 'foreign', '[]', '{}', '{}', 1, 'card-a', 0)",
                )
                connection.execSQL("PRAGMA user_version = 11")
            }

            val database = Room.databaseBuilder<KlafRoomDatabase>(name = databaseFile.toString())
                .addMigrations(*com.kuts.klaf.room.databases.BranchIntegrationMigrations.all)
                .setDriver(BundledSQLiteDriver())
                .build()
            try {
                runBlocking {
                    val deck = database.deckDao().getAllDecks().single()
                    assertEquals("deck-a", deck.syncId)
                    assertEquals(listOf(200L), deck.scheduledIterationDates)
                    assertNull(deck.toReviewInfo())
                    assertEquals("card-a", database.cardDao().getAllCards().single().syncId)
                }
            } finally {
                database.close()
            }
        } finally {
            directory.toFile().deleteRecursively()
        }
    }
}
