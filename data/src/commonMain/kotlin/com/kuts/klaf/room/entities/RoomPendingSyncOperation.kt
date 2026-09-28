package com.kuts.klaf.room.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pending_sync_operations",
    indices = [Index("accountId"), Index(value = ["operationId"], unique = true)],
)
data class RoomPendingSyncOperation(
    val accountId: String,
    val operationId: String,
    val baseRevision: Long,
    val operationJson: String,
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
)
