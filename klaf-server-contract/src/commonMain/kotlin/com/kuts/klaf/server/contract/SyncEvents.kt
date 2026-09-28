package com.kuts.klaf.server.contract

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SyncEventDevice(
    val id: String,
    val name: String,
    val platform: String,
    val connected: Boolean,
    val lastConfirmedRevision: Long? = null,
    val lastSuccessfulSyncAtMillis: Long? = null,
)

@Serializable
sealed interface SyncEventMessage {

    @Serializable
    @SerialName("STATE")
    data class State(
        val revision: Long,
        val devices: List<SyncEventDevice>,
    ) : SyncEventMessage

    @Serializable
    @SerialName("REVISION_CHANGED")
    data class RevisionChanged(val revision: Long) : SyncEventMessage
}
