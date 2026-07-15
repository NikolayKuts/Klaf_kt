package com.kuts.klaf.room.databases

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.kuts.klaf.room.entities.RoomCard.Companion.CARD_TABLE_NAME

object Migrations {

    private const val MNEMONIC_JSON_COLUMN = "mnemonicJson"

    val from6To7 = object : Migration(6, 7) {

        override fun migrate(connection: SQLiteConnection) {
            if (!connection.hasColumn(tableName = CARD_TABLE_NAME, columnName = MNEMONIC_JSON_COLUMN)) {
                connection.execSQL(
                    "ALTER TABLE $CARD_TABLE_NAME " +
                        "ADD COLUMN $MNEMONIC_JSON_COLUMN TEXT NOT NULL DEFAULT ''"
                )
            }
        }
    }
}
