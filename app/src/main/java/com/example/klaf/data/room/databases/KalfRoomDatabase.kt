package com.example.klaf.data.room.databases

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.klaf.data.room.converters.RoomDateConverter
import com.example.klaf.data.room.dao.CardDao
import com.example.klaf.data.room.dao.DeckDao
import com.example.klaf.data.room.entities.RoomCard
import com.example.klaf.data.room.entities.RoomDeck

@Database(entities = [RoomDeck::class, RoomCard::class], version = 2, exportSchema = false)
@TypeConverters(RoomDateConverter::class)
abstract class KlafRoomDatabase : RoomDatabase() {

    companion object {
        private const val DB_NAME = "klaf_kt.db"
        private var database: KlafRoomDatabase? = null
        private val LOCK = Any()

        fun getInstance(context: Context): KlafRoomDatabase = synchronized(LOCK) {
            database ?: Room.databaseBuilder(context, KlafRoomDatabase::class.java, DB_NAME)
                .fallbackToDestructiveMigration()
                .build()
                .also { database = it }
        }
    }

    abstract fun deckDao(): DeckDao
    abstract fun cardDao(): CardDao
}