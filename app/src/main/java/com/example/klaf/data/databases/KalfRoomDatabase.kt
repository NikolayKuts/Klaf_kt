package com.example.klaf.data.dao

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.klaf.domain.pojo.Deck

@Database(entities = [Deck::class], version = 1, exportSchema = false)
abstract class KlafRoomDatabase : RoomDatabase() {

    companion object {
        private const val DB_NAME = "klaf.db"
        private var database: KlafRoomDatabase? = null
        private val LOCK = Any()

        fun getInstance(context: Context): KlafRoomDatabase {
            synchronized(LOCK) {
                database?.let { return it }
                val instance = Room.databaseBuilder(context, KlafRoomDatabase::class.java, DB_NAME)
                    .fallbackToDestructiveMigration()
                    .build()
                database = instance
                return instance
            }
        }
    }

    abstract fun deckDao(): DeckDao
}