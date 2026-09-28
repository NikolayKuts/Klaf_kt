package com.kuts.klaf.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kuts.klaf.room.entities.RoomSyncCheckpoint
import kotlinx.coroutines.flow.Flow

@Dao
interface ISyncCheckpointDao {

    @Query("SELECT * FROM sync_checkpoint WHERE id = 1")
    suspend fun current(): RoomSyncCheckpoint?

    @Query("SELECT * FROM sync_checkpoint WHERE id = 1")
    fun observeCurrent(): Flow<RoomSyncCheckpoint?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(checkpoint: RoomSyncCheckpoint)
}
