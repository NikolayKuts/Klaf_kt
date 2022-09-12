package com.example.klaf.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.klaf.data.room.entities.StorageSaveVersion

@Dao
interface StorageSaveVersionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertStorageSaveVersion(saveVersion: StorageSaveVersion)

    @Query("SELECT * FROM ${StorageSaveVersion.TABLE_NAME}")
    fun getStorageSaveVersion(): StorageSaveVersion?
}