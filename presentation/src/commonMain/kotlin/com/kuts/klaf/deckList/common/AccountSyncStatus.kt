package com.kuts.klaf.deckList.common

import com.kuts.domain.common.IDataSynchronizationState
import com.kuts.klaf.server.contract.SyncHistoryItem
import kotlinx.coroutines.flow.Flow

private const val SYNC_HISTORY_ID_PREFIX_LENGTH = 8

enum class AccountSyncIndicator { HIDDEN, GREEN, YELLOW, RED, GRAY, SYNCING }

data class AccountSyncDevice(
    val id: String,
    val name: String,
    val platform: String,
    val connected: Boolean,
    val lastConfirmedRevision: Long?,
    val lastSuccessfulSyncAtMillis: Long?,
)

data class AccountSyncStatus(
    val accountEmail: String? = null,
    val indicator: AccountSyncIndicator = AccountSyncIndicator.HIDDEN,
    val confirmedRevision: Long = 0L,
    val serverRevision: Long? = null,
    val pendingOperationCount: Int = 0,
    val hasConflict: Boolean = false,
    val devices: List<AccountSyncDevice> = emptyList(),
)

sealed interface AccountSyncHistoryState {

    data object Loading : AccountSyncHistoryState

    data class Loaded(val entries: List<SyncHistoryItem>) : AccountSyncHistoryState

    data object Failed : AccountSyncHistoryState
}

internal fun SyncHistoryItem.deviceName(devices: List<AccountSyncDevice>): String =
    devices.firstOrNull { it.id == deviceId }?.name ?: deviceId

internal fun SyncHistoryItem.affectedItemsSummary(): String = if (action == "IMPORT_ANDROID_BACKUP") {
    "${affectedSyncIds.size} imported items"
} else {
    affectedSyncIds.joinToString { id ->
        if (id.startsWith("account-interim:")) "interim deck"
        else if (id.length > SYNC_HISTORY_ID_PREFIX_LENGTH) "${id.take(SYNC_HISTORY_ID_PREFIX_LENGTH)}…"
        else id
    }
}

internal fun accountSignOutAllowed(
    accountMode: Boolean,
    synchronizationState: IDataSynchronizationState,
    status: AccountSyncStatus,
): Boolean = !accountMode ||
    (synchronizationState !is IDataSynchronizationState.Synchronizing &&
        status.indicator != AccountSyncIndicator.SYNCING)

interface AccountSyncStatusGateway {

    val status: Flow<AccountSyncStatus>

    suspend fun recentHistory(accountEmail: String): List<SyncHistoryItem>
}
