package com.example.klaf.data.room.databases

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.klaf.data.room.entities.RoomCard.Companion.CARD_TABLE_NAME

object Migrations {

    val from3To4 = object : Migration(3, 4) {

        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("UPDATE $CARD_TABLE_NAME SET ipa = '[]'")
        }
    }
}