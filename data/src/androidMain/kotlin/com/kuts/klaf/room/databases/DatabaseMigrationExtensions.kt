package com.kuts.klaf.room.databases

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.db.SupportSQLiteDatabase

internal fun SupportSQLiteDatabase.hasColumn(
    tableName: String,
    columnName: String,
): Boolean {
    query("PRAGMA table_info($tableName)").use { cursor ->
        val columnNameIndex = cursor.getColumnIndex("name")
        while (cursor.moveToNext()) {
            if (cursor.getString(columnNameIndex) == columnName) {
                return true
            }
        }
    }

    return false
}

internal fun SQLiteConnection.hasColumn(
    tableName: String,
    columnName: String,
): Boolean {
    prepare("PRAGMA table_info($tableName)").use { statement ->
        val columnNameIndex = statement.getColumnNames().indexOf("name")
        if (columnNameIndex == -1) return false

        while (statement.step()) {
            if (statement.getText(columnNameIndex) == columnName) {
                return true
            }
        }
    }

    return false
}

