package com.kuts.klaf.room.databases

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

object KlafRoomDatabaseProvider {

    private const val DB_NAME = "klaf_kt.db"
    private val lock = Any()
    private var database: KlafRoomDatabase? = null

    fun getInstance(context: Context): KlafRoomDatabase = synchronized(lock) {
        database ?: Room.databaseBuilder<KlafRoomDatabase>(
            context = context.applicationContext,
            name = DB_NAME,
        )
            .addMigrations(Migrations.from3To4)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(context = Dispatchers.IO)
            .build()
            .also { createdDatabase ->
                database = createdDatabase
            }
    }
}
