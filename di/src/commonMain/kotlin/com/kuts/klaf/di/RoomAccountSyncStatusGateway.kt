package com.kuts.klaf.di

import com.kuts.klaf.deckList.common.AccountSyncDevice
import com.kuts.klaf.deckList.common.AccountSyncIndicator
import com.kuts.klaf.deckList.common.AccountSyncStatus
import com.kuts.klaf.deckList.common.AccountSyncStatusGateway
import com.kuts.klaf.networking.klafServer.AccountDeviceIdentity
import com.kuts.klaf.networking.klafServer.SyncEventConnector
import com.kuts.klaf.networking.klafServer.SyncEventFeed
import com.kuts.klaf.room.databases.ActiveLocalRoomDatabase
import com.kuts.klaf.room.repositoryImplementations.ManualRoomSyncCoordinator
import com.kuts.klaf.room.repositoryImplementations.RoomSyncStatus
import com.kuts.klaf.room.repositoryImplementations.RoomSyncStatusObserver
import com.kuts.klaf.room.repositoryImplementations.SyncIndicatorState
import com.kuts.klaf.server.contract.SyncHistoryItem
import com.kuts.klaf.server.contract.SyncHistoryResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

internal fun RoomSyncStatus.toAccountSyncStatus(): AccountSyncStatus = AccountSyncStatus(
    accountEmail = accountEmail,
    indicator = when (indicator) {
        SyncIndicatorState.HIDDEN -> AccountSyncIndicator.HIDDEN
        SyncIndicatorState.GREEN -> AccountSyncIndicator.GREEN
        SyncIndicatorState.YELLOW -> AccountSyncIndicator.YELLOW
        SyncIndicatorState.RED -> AccountSyncIndicator.RED
        SyncIndicatorState.GRAY -> AccountSyncIndicator.GRAY
        SyncIndicatorState.SYNCING -> AccountSyncIndicator.SYNCING
    },
    confirmedRevision = confirmedRevision,
    serverRevision = serverRevision,
    pendingOperationCount = pendingOperationCount,
    hasConflict = hasConflict,
    devices = devices.map { device ->
        AccountSyncDevice(
            id = device.id,
            name = device.name,
            platform = device.platform,
            connected = device.connected,
            lastConfirmedRevision = device.lastConfirmedRevision,
            lastSuccessfulSyncAtMillis = device.lastSuccessfulSyncAtMillis,
        )
    },
)

internal class AccountSyncHistoryReader(
    private val selectedEmail: () -> String?,
    private val deviceId: suspend () -> String,
    private val fetch: suspend (String, String) -> SyncHistoryResponse,
) {

    suspend fun recentHistory(accountEmail: String): List<SyncHistoryItem> {
        check(selectedEmail() == accountEmail) { "Selected account changed before history read" }
        val response = fetch(accountEmail, deviceId())
        check(selectedEmail() == accountEmail) { "Selected account changed during history read" }
        return response.entries
    }
}

internal class RoomAccountSyncStatusGateway(
    private val databaseSource: ActiveLocalRoomDatabase,
    private val identity: AccountDeviceIdentity,
    private val connector: SyncEventConnector,
    private val coordinator: ManualRoomSyncCoordinator,
    private val historyReader: AccountSyncHistoryReader,
    private val hasMissingImages: suspend (String) -> Boolean,
) : AccountSyncStatusGateway {

    override suspend fun recentHistory(accountEmail: String): List<SyncHistoryItem> =
        historyReader.recentHistory(accountEmail)

    override val status: Flow<AccountSyncStatus> = flow {
        val feed = SyncEventFeed(
            selectedAccountEmail = databaseSource.selection.map { it.accountEmail },
            deviceIdProvider = { identity.current().id },
            connector = connector,
            scope = CoroutineScope(currentCoroutineContext()),
        )
        val observer = RoomSyncStatusObserver(
            databaseSource,
            feed.state,
            coordinator.attemptState,
            hasMissingImages,
        )
        feed.start()
        try {
            emitAll(observer.status.map(RoomSyncStatus::toAccountSyncStatus))
        } finally {
            feed.stop()
        }
    }
}
