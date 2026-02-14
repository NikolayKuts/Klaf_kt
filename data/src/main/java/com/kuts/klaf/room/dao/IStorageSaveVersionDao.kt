package com.kuts.klaf.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kuts.klaf.room.entities.RoomStorageSaveVersion

@Dao
interface IStorageSaveVersionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertStorageSaveVersion(saveVersion: RoomStorageSaveVersion)

    @Query("SELECT * FROM ${RoomStorageSaveVersion.TABLE_NAME}")
    fun getStorageSaveVersion(): RoomStorageSaveVersion?
}