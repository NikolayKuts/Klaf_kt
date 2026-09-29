package com.kuts.klaf.room.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "sync_checkpoint")
data class RoomSyncCheckpoint(
    val confirmedRevision: Long,
    @PrimaryKey val id: Int = CHECKPOINT_ID,
    @ColumnInfo(defaultValue = "0")
    val vocabularySyncInitialized: Boolean = false,
) {

    companion object {
        const val CHECKPOINT_ID = 1
    }
}
