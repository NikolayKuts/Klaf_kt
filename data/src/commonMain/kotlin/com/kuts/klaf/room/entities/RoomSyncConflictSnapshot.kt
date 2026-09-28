package com.kuts.klaf.room.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_conflict_snapshot")
data class RoomSyncConflictSnapshot(
    val accountId: String,
    val baseRevision: Long,
    val responseJson: String,
    @PrimaryKey val id: Int = SNAPSHOT_ID,
) {

    companion object {
        const val SNAPSHOT_ID = 1
    }
}
