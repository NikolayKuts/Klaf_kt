package com.example.klaf.data.room.databases

import android.content.Context
import androidx.room.*
import com.example.klaf.data.room.converters.RoomDateConverter
import com.example.klaf.data.room.dao.CardDao
import com.example.klaf.data.room.dao.DeckDao
import com.example.klaf.data.room.dao.StorageSaveVersionDao
import com.example.klaf.data.room.entities.RoomCard
import com.example.klaf.data.room.entities.RoomDeck
import com.example.klaf.data.room.entities.RoomStorageSaveVersion

@Database(
    entities = [
        RoomDeck::class,
        RoomCard::class,
        RoomStorageSaveVersion::class
    ],
    version = 4,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)]
)
@TypeConverters(RoomDateConverter::class)
abstract class KlafRoomDatabase : RoomDatabase() {

    companion object {
        private const val DB_NAME = "klaf_kt.db"
        private var database: KlafRoomDatabase? = null
        private val LOCK = Any()

        fun getInstance(context: Context): KlafRoomDatabase = synchronized(LOCK) {
            database ?: Room.databaseBuilder(context, KlafRoomDatabase::class.java, DB_NAME)
                .addMigrations(Migrations.from3To4)
                .build()
                .also { database = it }
        }
    }

    abstract fun deckDao(): DeckDao
    abstract fun cardDao(): CardDao
    abstract fun storageSaveVersionDao(): StorageSaveVersionDao
}