package com.kuts.klaf.data.room.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = RoomStorageSaveVersion.TABLE_NAME)
data class RoomStorageSaveVersion(
    @ColumnInfo(name = "save_version")
    val version: Long,
    @PrimaryKey(autoGenerate = false)
    val id: Int = PRIMARY_KEY,
) {

    companion object {

        const val TABLE_NAME = "storage_save_version_table_name"

        private const val PRIMARY_KEY = -1
    }
}