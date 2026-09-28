package com.kuts.klaf.room

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.kuts.klaf.room.databases.KlafRoomDatabase
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private const val PREVIOUS_SCHEMA = "schemas/com.kuts.klaf.room.databases.KlafRoomDatabase/12.json"

class RoomSyncCheckpointMigrationTest {

    @Test
    fun `version twelve outbox survives checkpoint migration`() {
        val tempRoot = Path.of(System.getProperty("java.io.tmpdir")).toRealPath()
        val directory = Files.createTempDirectory(tempRoot, "klaf-checkpoint-migration-").toRealPath()
        require(directory.parent == tempRoot && directory.fileName.toString().startsWith("klaf-checkpoint-migration-"))
        val databaseFile = directory.resolve("legacy.db")
        try {
            BundledSQLiteDriver().open(databaseFile.toString()).use { connection ->
                val schemaPath = Path.of(PREVIOUS_SCHEMA).let { path ->
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
                    "INSERT INTO pending_sync_operations(accountId, operationId, baseRevision, operationJson, id) " +
                        "VALUES ('alice@example.test', 'operation-a', 2, " +
                        "'{\"type\":\"DELETE_DECK\",\"operationId\":\"operation-a\",\"deckSyncId\":\"deck-a\"}', 1)",
                )
                connection.execSQL("PRAGMA user_version = 12")
            }

            val database = Room.databaseBuilder<KlafRoomDatabase>(name = databaseFile.toString())
                .setDriver(BundledSQLiteDriver())
                .build()
            try {
                runBlocking {
                    assertEquals("operation-a", database.pendingSyncOperationDao().allPending().single().operationId)
                    assertEquals(null, database.syncCheckpointDao().current())
                }
            } finally {
                database.close()
            }
        } finally {
            directory.toFile().deleteRecursively()
        }
    }
}
