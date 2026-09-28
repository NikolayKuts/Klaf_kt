package com.kuts.klaf.room

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.kuts.klaf.room.databases.BranchIntegrationMigrations
import com.kuts.klaf.room.databases.KlafRoomDatabase
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BranchIntegrationMigrationTest {

    @Test
    fun bothBranchLayoutsPreserveDecksCardsAndStorageIdentity() = runBlocking {
        for (voiceBranch in listOf(true, false)) {
            val directory = Files.createTempDirectory("klaf-branch-migration-").toFile()
            val file = File(directory, "test.db")
            val jsonText = if (voiceBranch) {
                javaClass.getResource("/branchSchemas/voice12.json")!!.readText()
            } else File("schemas/com.kuts.klaf.room.databases.KlafRoomDatabase/14.json").readText()
            val schema = Json.parseToJsonElement(jsonText).jsonObject.getValue("database").jsonObject
            BundledSQLiteDriver().open(file.absolutePath).use { connection ->
                schema.getValue("entities").jsonArray.forEach { entity ->
                    val definition = entity.jsonObject
                    val table = definition.getValue("tableName").jsonPrimitive.content
                    connection.execSQL(definition.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", table))
                    definition["indices"]?.jsonArray?.forEach { index ->
                        connection.execSQL(index.jsonObject.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", table))
                    }
                }
                schema.getValue("setupQueries").jsonArray.forEach { connection.execSQL(it.jsonPrimitive.content) }
                connection.execSQL("PRAGMA user_version = ${if (voiceBranch) 12 else 14}")
                connection.execSQL("INSERT INTO decks (id,name,creationDate,repetitionIterationDates,scheduledIterationDates," +
                    "scheduledDateInterval,repetitionQuantity,cardQuantity,lastFirstRepetitionDuration,lastSecondRepetitionDuration," +
                    "lastRepetitionIterationDuration,isLastIterationSucceeded) VALUES (7,'Kept',42,'[1,2]','[3,4]',5,2,1,6,7,8,1)")
                connection.execSQL("INSERT INTO cards (id,deckId,nativeWord,foreignWord,ipa,wordMeaningInsightsJson,mnemonicJson) " +
                    "VALUES (9,7,'meaning','word','[]','','')")
                if (voiceBranch) connection.execSQL("INSERT INTO vocabulary_sources " +
                    "(id,title,description,url,rawText,cleanText,analysisVersion,createdAt,updatedAt) " +
                    "VALUES (3,'Kept source','','https://example.invalid/source','text','text',1,1,1)")
                if (!voiceBranch) connection.execSQL("UPDATE decks SET syncId='kept-deck',lastChangedServerRevision=8,reviewCurrentDuration=99")
            }
            val database = Room.databaseBuilder<KlafRoomDatabase>(file.absolutePath)
                .addMigrations(*BranchIntegrationMigrations.all).setDriver(BundledSQLiteDriver()).build()
            try {
                val deck = database.deckDao().getAllDecks().single()
                val card = database.cardDao().getAllCards().single()
                assertEquals(7, deck.id)
                assertEquals("Kept", deck.name)
                assertEquals(2, deck.repetitionQuantity)
                assertEquals(9, card.id)
                assertEquals(7, card.deckId)
                assertEquals("word", card.foreignWord)
                assertTrue(deck.syncId.isNotBlank())
                assertTrue(card.syncId.isNotBlank())
                assertEquals(emptyList(), database.pendingSyncOperationDao().allPending())
                assertEquals(emptyList(), database.ignoredVocabularyWordDao().getWords())
                if (voiceBranch) {
                    val source = database.vocabularySourceDao().getSources().single()
                    assertEquals(3, source.id)
                    assertEquals("https://example.invalid/source", source.url)
                }
                if (!voiceBranch) {
                    assertEquals("kept-deck", deck.syncId)
                    assertEquals(8L, deck.lastChangedServerRevision)
                    assertEquals(99L, deck.reviewCurrentDuration)
                }
            } finally {
                database.close()
                check(directory.name.startsWith("klaf-branch-migration-"))
                directory.walkBottomUp().forEach { it.delete() }
            }
        }
    }
}
