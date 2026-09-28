package com.kuts.klaf.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kuts.klaf.room.entities.RoomSyncConflictSnapshot
import kotlinx.coroutines.flow.Flow

@Dao
interface ISyncConflictSnapshotDao {

    @Query("SELECT * FROM sync_conflict_snapshot WHERE id = 1")
    suspend fun current(): RoomSyncConflictSnapshot?

    @Query("SELECT * FROM sync_conflict_snapshot WHERE id = 1")
    fun observeCurrent(): Flow<RoomSyncConflictSnapshot?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(snapshot: RoomSyncConflictSnapshot)

    @Query("DELETE FROM sync_conflict_snapshot WHERE id = 1")
    suspend fun clear()
}
