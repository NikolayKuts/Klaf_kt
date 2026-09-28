package com.kuts.klaf.room.databases

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.io.File
import kotlinx.coroutines.Dispatchers

object KlafRoomDatabaseProvider {

    private const val DB_NAME = "klaf_kt.db"
    private val lock = Any()
    private var database: KlafRoomDatabase? = null

    fun getInstance(): KlafRoomDatabase = synchronized(lock) {
        database ?: Room.databaseBuilder<KlafRoomDatabase>(
            name = databaseFilePath(),
        )
            .addMigrations(
                Migrations.from6To7,
                Migrations.from7To8,
                Migrations.from8To9,
                Migrations.from9To10,
                Migrations.from10To11,
                Migrations.from11To12,
            )
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(context = Dispatchers.IO)
            .build()
            .also { createdDatabase ->
                database = createdDatabase
            }
    }

    private fun databaseFilePath(): String {
        val appDir = File(System.getProperty("user.home"), ".klaf_kt")
        if (!appDir.exists()) {
            appDir.mkdirs()
        }

        return File(appDir, DB_NAME).absolutePath
    }
}
