package com.kuts.klaf.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.kuts.klaf.room.entities.RoomPendingSyncOperation
import kotlinx.coroutines.flow.Flow

@Dao
interface IPendingSyncOperationDao {

    @Insert
    suspend fun insert(operation: RoomPendingSyncOperation)

    @Query("SELECT * FROM pending_sync_operations WHERE accountId = :accountId ORDER BY id")
    suspend fun pendingForAccount(accountId: String): List<RoomPendingSyncOperation>

    @Query("SELECT COUNT(*) FROM pending_sync_operations WHERE accountId = :accountId")
    fun observePendingCount(accountId: String): Flow<Int>

    @Query("SELECT * FROM pending_sync_operations ORDER BY id")
    suspend fun allPending(): List<RoomPendingSyncOperation>

    @Query("DELETE FROM pending_sync_operations WHERE accountId = :accountId AND operationId = :operationId")
    suspend fun removeAccepted(accountId: String, operationId: String)
}
