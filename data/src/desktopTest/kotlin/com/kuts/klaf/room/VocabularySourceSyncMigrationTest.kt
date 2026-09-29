package com.kuts.klaf.room

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.kuts.klaf.room.databases.BranchIntegrationMigrations
import com.kuts.klaf.room.databases.KlafRoomDatabase
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class VocabularySourceSyncMigrationTest {

    @Test
    fun `version 15 sources survive migration and receive durable unique sync identities`() = runBlocking {
        val tempRoot = java.nio.file.Path.of(System.getProperty("java.io.tmpdir")).toRealPath()
        val root = Files.createTempDirectory(tempRoot, "klaf-source-sync-migration-").toRealPath()
        val file = root.resolve("test.db").toFile()
        try {
            val schema = Json.parseToJsonElement(java.io.File(
                "schemas/com.kuts.klaf.room.databases.KlafRoomDatabase/15.json",
            ).readText()).jsonObject.getValue("database").jsonObject
            BundledSQLiteDriver().open(file.absolutePath).use { connection ->
                schema.getValue("entities").jsonArray.forEach { entry ->
                    val entity = entry.jsonObject
                    val table = entity.getValue("tableName").jsonPrimitive.content
                    connection.execSQL(entity.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", table))
                    entity["indices"]?.jsonArray?.forEach { index ->
                        connection.execSQL(index.jsonObject.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", table))
                    }
                }
                schema.getValue("setupQueries").jsonArray.forEach { connection.execSQL(it.jsonPrimitive.content) }
                connection.execSQL("PRAGMA user_version = 15")
                repeat(2) { index ->
                    connection.execSQL("INSERT INTO vocabulary_sources " +
                        "(id,title,description,url,rawText,cleanText,analysisVersion,createdAt,updatedAt) " +
                        "VALUES (${index + 1},'Kept $index','','https://example.test/source','raw','clean',1,1,2)")
                    connection.execSQL("INSERT INTO vocabulary_source_items " +
                        "(sourceId,language,foreignWord,transcription,nativeWord,originalText,partOfSpeech,confidence,category,status," +
                        "sourceExample,explanation,knownMeaningsSnapshot,alreadyExists,occurrencesJson,firstOccurrenceOrder,isEdited,createdAt,updatedAt,id) " +
                        "VALUES (${index + 1},'en','keptword$index','','meaning','','UNKNOWN','MEDIUM','NEW','PENDING'," +
                        "'','','',0,'[]',0,0,1,2,${index + 10})")
                }
            }
            var database = Room.databaseBuilder<KlafRoomDatabase>(file.absolutePath)
                .addMigrations(*BranchIntegrationMigrations.all).setDriver(BundledSQLiteDriver()).build()
            try {
                assertEquals(setOf(1, 2), database.vocabularySourceDao().getSources().map { it.id }.toSet())
                assertEquals(setOf("Kept 0", "Kept 1"), database.vocabularySourceDao().getSources().map { it.title }.toSet())
                val items = database.vocabularySourceItemDao().getItems()
                assertEquals(setOf(10, 11), items.map { it.id }.toSet())
                assertEquals(setOf(1, 2), items.map { it.sourceId }.toSet())
                assertTrue(items.all { it.syncId.isNotBlank() })
                assertEquals(2, items.map { it.syncId }.distinct().size)
            } finally {
                database.close()
            }
            fun identities(): List<String> = BundledSQLiteDriver().open(file.absolutePath).use { connection ->
                connection.prepare("SELECT syncId FROM vocabulary_sources ORDER BY id").use { statement ->
                    buildList { while (statement.step()) add(statement.getText(0)) }
                }
            }
            val before = identities()
            assertTrue(before.all(String::isNotBlank))
            assertEquals(2, before.distinct().size)
            database = Room.databaseBuilder<KlafRoomDatabase>(file.absolutePath)
                .addMigrations(*BranchIntegrationMigrations.all).setDriver(BundledSQLiteDriver()).build()
            try {
                assertEquals(2, database.vocabularySourceDao().getSources().size)
            } finally {
                database.close()
            }
            assertEquals(before, identities())
        } finally {
            check(root.parent == tempRoot && root.fileName.toString().startsWith("klaf-source-sync-migration-"))
            root.toFile().deleteRecursively()
        }
    }
}
