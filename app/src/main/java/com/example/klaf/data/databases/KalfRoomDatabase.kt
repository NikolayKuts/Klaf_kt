package com.example.klaf.data.dao

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.klaf.domain.pojo.Card
import com.example.klaf.domain.pojo.Deck

@Database(entities = [Deck::class, Card::class], version = 2, exportSchema = false)
abstract class KlafRoomDatabase : RoomDatabase() {

    companion object {
        private const val DB_NAME = "klaf.db"
        private var database: KlafRoomDatabase? = null
        private val LOCK = Any()

        fun getInstance(context: Context): KlafRoomDatabase {
            return database ?: synchronized(LOCK) {
                val instance = Room.databaseBuilder(context, KlafRoomDatabase::class.java, DB_NAME)
                    .fallbackToDestructiveMigration()
                    .build()
                database = instance
                instance
            }
        }
    }

    abstract fun deckDao(): DeckDao
    abstract fun cardDao(): CardDao
}