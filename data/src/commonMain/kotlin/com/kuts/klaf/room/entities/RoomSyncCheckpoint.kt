package com.kuts.klaf.room.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_checkpoint")
data class RoomSyncCheckpoint(
    val confirmedRevision: Long,
    @PrimaryKey val id: Int = CHECKPOINT_ID,
) {

    companion object {
        const val CHECKPOINT_ID = 1
    }
}
