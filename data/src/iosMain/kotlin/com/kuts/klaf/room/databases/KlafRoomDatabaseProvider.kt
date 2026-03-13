package com.kuts.klaf.room.databases

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

object KlafRoomDatabaseProvider {

    private const val DB_NAME = "klaf_kt.db"
    private var database: KlafRoomDatabase? = null

    fun getInstance(): KlafRoomDatabase {
        return database ?: Room.databaseBuilder<KlafRoomDatabase>(
            name = databaseFilePath(),
        )
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(context = Dispatchers.Default)
            .build()
            .also { createdDatabase ->
                database = createdDatabase
            }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun databaseFilePath(): String {
        val documentsUrl = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = null,
        )
        val documentsPath = requireNotNull(documentsUrl?.path) {
            "Unable to resolve iOS documents directory for database path."
        }

        return "$documentsPath/$DB_NAME"
    }
}
