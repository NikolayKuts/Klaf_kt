package com.kuts.klaf.room

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.kuts.domain.common.ICoroutineContextProvider
import com.kuts.domain.entities.IgnoredVocabularyWord
import com.kuts.domain.entities.VocabularySource
import com.kuts.domain.entities.VocabularySourceItem
import com.kuts.domain.entities.VocabularySourceItemCategory
import com.kuts.domain.entities.VocabularySourceItemStatus
import com.kuts.domain.repositories.IIgnoredVocabularyWordRepository
import com.kuts.domain.useCases.SaveVocabularySourceChangesUseCase
import com.kuts.klaf.room.databases.KlafRoomDatabase
import com.kuts.klaf.room.databases.Migrations
import com.kuts.klaf.room.repositoryImplementations.IgnoredVocabularyWordRepositoryRoom
import com.kuts.klaf.room.repositoryImplementations.StorageSaveVersionRepositoryRoom
import com.kuts.klaf.room.repositoryImplementations.StorageTransactionRepositoryRoom
import com.kuts.klaf.room.repositoryImplementations.VocabularySourceRepositoryRoom
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

private val persistenceTestSource = VocabularySource("Source", url = "https://example.com/source",
    createdAt = 1, updatedAt = 2, id = 1)

private fun persistenceTestItem(status: VocabularySourceItemStatus, id: Int) = VocabularySourceItem(
    sourceId = 1, language = "en", foreignWord = "word$id", transcription = "ipa", nativeWord = "meaning$id",
    category = VocabularySourceItemCategory.NEW, status = status, createdAt = 1, updatedAt = 2, id = id,
    createdCardId = if (status == VocabularySourceItemStatus.ADDED) 10 else null,
)

private suspend fun withTemporaryRoomDatabase(
    startingVersion: Int? = null,
    block: suspend (KlafRoomDatabase) -> Unit,
) {
    val directory = Files.createTempDirectory("klaf-room-review-").toFile()
    val databaseFile = File(directory, "test.db")
    if (startingVersion != null) {
        val schema = Json.parseToJsonElement(File("schemas/com.kuts.klaf.room.databases.KlafRoomDatabase/$startingVersion.json")
            .readText()).jsonObject.getValue("database").jsonObject
        BundledSQLiteDriver().open(databaseFile.absolutePath).use { connection ->
            schema.getValue("entities").jsonArray.forEach { entity ->
                val definition = entity.jsonObject
                val name = definition.getValue("tableName").jsonPrimitive.content
                connection.execSQL(definition.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", name))
                definition["indices"]?.jsonArray?.forEach { index ->
                    connection.execSQL(index.jsonObject.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", name))
                }
            }
            schema.getValue("setupQueries").jsonArray.forEach { query -> connection.execSQL(query.jsonPrimitive.content) }
            connection.execSQL("PRAGMA user_version = $startingVersion")
            connection.execSQL("INSERT INTO vocabulary_sources (id,title,description,rawText,cleanText,analysisVersion,createdAt,updatedAt) " +
                "VALUES (1,'Legacy','','raw','clean',1,1,2)")
        }
    }
    val database = Room.databaseBuilder<KlafRoomDatabase>(databaseFile.absolutePath)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .addMigrations(Migrations.from8To9, Migrations.from9To10, Migrations.from10To11, Migrations.from11To12)
        .build()
    try {
        block(database)
    } finally {
        database.close()
        // Only files created in this dedicated test directory are removed.
        directory.listFiles()?.forEach { it.delete() }
        directory.delete()
    }
}

class VocabularySourcePersistenceTest {

    @Test
    fun everyNewMigrationPathPreservesExistingSources() = runBlocking {
        for (version in 8..11) {
            withTemporaryRoomDatabase(version) { database ->
                val source = VocabularySourceRepositoryRoom(database).fetchSourceById(1)!!
                assertEquals("Legacy", source.title)
                assertEquals("raw", source.rawText)
                assertEquals("", source.url)
                assertEquals(emptyList(), IgnoredVocabularyWordRepositoryRoom(database).fetchWords())
            }
        }
    }

    @Test
    fun saveRemovesIgnoredItemsPreservesAddedLinksAndDeduplicatesRules() = runBlocking {
        withTemporaryRoomDatabase { database ->
            val sources = VocabularySourceRepositoryRoom(database)
            val ignored = IgnoredVocabularyWordRepositoryRoom(database)
            val added = persistenceTestItem(VocabularySourceItemStatus.ADDED, 3)
            val pending = persistenceTestItem(VocabularySourceItemStatus.PENDING, 1)
            val ignoredItem = persistenceTestItem(VocabularySourceItemStatus.IGNORED, 2)
            sources.saveSource(persistenceTestSource)
            sources.saveItems(listOf(added, pending, ignoredItem))
            val save = saveUseCase(database, ignored)
            repeat(2) { save(persistenceTestSource, listOf(pending, ignoredItem), true) }
            assertEquals(listOf(pending, added), sources.fetchItemsBySourceId(1))
            assertEquals(1, ignored.fetchWords().size)
            assertEquals(2L, StorageSaveVersionRepositoryRoom(database).fetchVersion()!!.version)
        }
    }

    @Test
    fun savingStaleAddedDraftCannotOverwriteThePreservedCardLink() = runBlocking {
        withTemporaryRoomDatabase { database ->
            val sources = VocabularySourceRepositoryRoom(database)
            sources.saveSource(persistenceTestSource)
            val staleAdded = persistenceTestItem(VocabularySourceItemStatus.ADDED, 3)
            val currentAdded = staleAdded.copy(createdCardId = 99, targetDeckId = 7)
            sources.saveItems(listOf(currentAdded))
            saveUseCase(database, IgnoredVocabularyWordRepositoryRoom(database))(
                persistenceTestSource, listOf(staleAdded), true,
            )
            assertEquals(listOf(currentAdded), sources.fetchItemsBySourceId(1))
        }
    }

    @Test
    fun ignoredRuleFailureRollsBackSourceItemsAndVersionTogether() = runBlocking {
        withTemporaryRoomDatabase { database ->
            val sources = VocabularySourceRepositoryRoom(database)
            sources.saveSource(persistenceTestSource)
            val failureRepository = object : IIgnoredVocabularyWordRepository {
                override suspend fun fetchWords() = emptyList<IgnoredVocabularyWord>()
                override suspend fun saveWords(words: List<IgnoredVocabularyWord>) { error("Injected write failure") }
            }
            assertFailsWith<IllegalStateException> {
                saveUseCase(database, failureRepository)(persistenceTestSource.copy(title = "Unsaved"),
                    listOf(persistenceTestItem(VocabularySourceItemStatus.IGNORED, 2)), true)
            }
            assertEquals(persistenceTestSource, sources.fetchSourceById(1))
            assertNull(StorageSaveVersionRepositoryRoom(database).fetchVersion())
        }
    }

    private fun saveUseCase(database: KlafRoomDatabase, ignored: IIgnoredVocabularyWordRepository) = SaveVocabularySourceChangesUseCase(
        VocabularySourceRepositoryRoom(database), ignored, StorageSaveVersionRepositoryRoom(database),
        StorageTransactionRepositoryRoom(database), object : ICoroutineContextProvider { override val io = Dispatchers.IO },
    )
}
