package com.example.klaf.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.klaf.data.room.entities.RoomStorageSaveVersion

@Dao
interface StorageSaveVersionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertStorageSaveVersion(saveVersion: RoomStorageSaveVersion)

    @Query("SELECT * FROM ${RoomStorageSaveVersion.TABLE_NAME}")
    fun getStorageSaveVersion(): RoomStorageSaveVersion?
}