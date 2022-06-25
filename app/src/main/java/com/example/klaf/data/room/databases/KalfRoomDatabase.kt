package com.example.klaf.data.room.databases

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.klaf.data.room.dao.CardDao
import com.example.klaf.data.room.dao.DeckDao
import com.example.klaf.data.room.entities.RoomCard
import com.example.klaf.data.room.entities.RoomDeck

@Database(entities = [RoomDeck::class, RoomCard::class], version = 1, exportSchema = false)
abstract class KlafRoomDatabase : RoomDatabase() {

    companion object {
        private const val DB_NAME = "klaf.db"
        private var database: KlafRoomDatabase? = null
        private val LOCK = Any()

        fun getInstance(context: Context): KlafRoomDatabase {
            return synchronized(LOCK) {
                database ?: Room.databaseBuilder(context, KlafRoomDatabase::class.java, DB_NAME)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { database = it }
            }
        }
    }

    abstract fun deckDao(): DeckDao
    abstract fun cardDao(): CardDao
}