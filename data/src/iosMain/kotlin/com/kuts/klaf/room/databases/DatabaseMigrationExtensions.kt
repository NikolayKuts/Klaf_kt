package com.kuts.klaf.room.databases

import androidx.sqlite.SQLiteConnection

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
