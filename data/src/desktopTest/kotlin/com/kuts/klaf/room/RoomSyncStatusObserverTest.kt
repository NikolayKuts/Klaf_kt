package com.kuts.klaf.room

import com.kuts.klaf.networking.klafServer.SyncEventChannelState
import com.kuts.klaf.networking.klafServer.SyncEventFeedStatus
import com.kuts.klaf.room.databases.ActiveLocalRoomDatabase
import com.kuts.klaf.room.databases.DesktopSelectedAccountStore
import com.kuts.klaf.room.databases.ScopedKlafRoomDatabaseFactory
import com.kuts.klaf.room.entities.RoomSyncConflictSnapshot
import com.kuts.klaf.room.repositoryImplementations.ManualSyncAttemptState
import com.kuts.klaf.room.repositoryImplementations.RoomSyncOutbox
import com.kuts.klaf.room.repositoryImplementations.RoomSyncStatusObserver
import com.kuts.klaf.room.repositoryImplementations.SyncIndicatorState
import com.kuts.klaf.server.contract.SyncEventDevice
import com.kuts.klaf.server.contract.SyncOperation
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

private const val STATUS_ALICE = "alice@example.test"
private const val STATUS_BOB = "bob@example.test"

class RoomSyncStatusObserverTest {

    @Test
    fun `matching revisions are not green while saved images are missing after restart`() = withSource { source ->
        source.selectAccount(STATUS_ALICE)
        RoomSyncOutbox(source).applyAccepted(STATUS_ALICE, emptyList(), 1L) {}
        val events = MutableStateFlow(connected(STATUS_ALICE, 1L))
        var missing = true
        val observer = RoomSyncStatusObserver(
            databaseSource = source,
            events = events,
            hasMissingImages = { account ->
                assertEquals(STATUS_ALICE, account)
                missing
            },
        )

        assertEquals(SyncIndicatorState.YELLOW, observer.status.first().indicator)
        missing = false
        events.value = connected(STATUS_ALICE, 2L)
        events.value = connected(STATUS_ALICE, 1L)
        assertEquals(SyncIndicatorState.GREEN, observer.await(SyncIndicatorState.GREEN).indicator)
    }

    @Test
    fun `Room and events update indicator without mixing guest or account state`() = withSource { source ->
        val events = MutableStateFlow(SyncEventFeedStatus())
        val attempts = MutableStateFlow<ManualSyncAttemptState>(ManualSyncAttemptState.Idle)
        val observer = RoomSyncStatusObserver(source, events, attempts)
        assertEquals(SyncIndicatorState.HIDDEN, observer.status.first().indicator)

        source.selectAccount(STATUS_ALICE)
        events.value = connected(STATUS_ALICE, 0L)
        assertEquals(SyncIndicatorState.YELLOW, observer.await(SyncIndicatorState.YELLOW).indicator)
        RoomSyncOutbox(source).applyAccepted(STATUS_ALICE, emptyList(), 0L) {}
        assertEquals(SyncIndicatorState.GREEN, observer.await(SyncIndicatorState.GREEN).indicator)

        val outbox = RoomSyncOutbox(source)
        outbox.recordChange(STATUS_ALICE, 0L, SyncOperation.DeleteDeck("pending-a", "deck-a")) {}
        val pending = observer.await(SyncIndicatorState.YELLOW)
        assertEquals(1, pending.pendingOperationCount)
        assertEquals(0L, pending.confirmedRevision)
        events.value = connected(STATUS_ALICE, 1L)
        assertEquals(1L, observer.await(SyncIndicatorState.YELLOW).serverRevision)

        outbox.applyAccepted(STATUS_ALICE, listOf("pending-a"), 1L) {}
        assertEquals(0, observer.await(SyncIndicatorState.GREEN).pendingOperationCount)

        source.current().syncConflictSnapshotDao().save(RoomSyncConflictSnapshot(
            accountId = STATUS_ALICE, baseRevision = 1L, responseJson = "{}",
        ))
        assertEquals(true, observer.await(SyncIndicatorState.RED).hasConflict)
        events.value = events.value.copy(channel = SyncEventChannelState.DISCONNECTED)
        assertEquals(SyncIndicatorState.RED, observer.await(SyncIndicatorState.RED).indicator)
        source.current().syncConflictSnapshotDao().clear()
        val unavailable = observer.await(SyncIndicatorState.GRAY)
        assertEquals(false, unavailable.devices.single().connected)
        assertEquals(1L, unavailable.serverRevision)

        source.selectAccount(STATUS_BOB)
        val bob = observer.await(SyncIndicatorState.GRAY)
        assertEquals(STATUS_BOB, bob.accountEmail)
        assertEquals(0L, bob.confirmedRevision)
        assertEquals(null, bob.serverRevision)
        assertEquals(emptyList(), bob.devices)
        events.value = connected(STATUS_BOB, 0L)
        assertEquals(SyncIndicatorState.YELLOW, observer.await(SyncIndicatorState.YELLOW).indicator)
        RoomSyncOutbox(source).applyAccepted(STATUS_BOB, emptyList(), 0L) {}
        assertEquals(SyncIndicatorState.GREEN, observer.await(SyncIndicatorState.GREEN).indicator)

        source.selectAccount(null)
        val guest = observer.await(SyncIndicatorState.HIDDEN)
        assertEquals(null, guest.accountEmail)
        assertEquals(emptyList(), guest.devices)
    }

    @Test
    fun `manual attempt and revision mismatch have explicit precedence`() = withSource { source ->
        source.selectAccount(STATUS_ALICE)
        val events = MutableStateFlow(SyncEventFeedStatus(STATUS_ALICE, SyncEventChannelState.DISCONNECTED))
        val attempts = MutableStateFlow<ManualSyncAttemptState>(ManualSyncAttemptState.Idle)
        val observer = RoomSyncStatusObserver(source, events, attempts)
        assertEquals(SyncIndicatorState.GRAY, observer.await(SyncIndicatorState.GRAY).indicator)

        attempts.value = ManualSyncAttemptState.Running(STATUS_ALICE)
        assertEquals(SyncIndicatorState.SYNCING, observer.await(SyncIndicatorState.SYNCING).indicator)
        attempts.value = ManualSyncAttemptState.Failed(STATUS_ALICE)
        assertEquals(SyncIndicatorState.RED, observer.await(SyncIndicatorState.RED).indicator)
        attempts.value = ManualSyncAttemptState.Failed(STATUS_BOB)
        assertEquals(SyncIndicatorState.GRAY, observer.await(SyncIndicatorState.GRAY).indicator)

        RoomSyncOutbox(source).applyAccepted(STATUS_ALICE, emptyList(), 2L) {}
        events.value = connected(STATUS_ALICE, 1L)
        val mismatch = observer.await(SyncIndicatorState.RED)
        assertEquals(2L, mismatch.confirmedRevision)
        assertEquals(1L, mismatch.serverRevision)
    }

    @Test
    fun `active Room observer emits after local outbox and conflict writes`() = withSource { source ->
        source.selectAccount(STATUS_ALICE)
        val events = MutableStateFlow(connected(STATUS_ALICE, 0L))
        val observer = RoomSyncStatusObserver(source, events)
        RoomSyncOutbox(source).applyAccepted(STATUS_ALICE, emptyList(), 0L) {}
        coroutineScope {
            val updates = Channel<SyncIndicatorState>(Channel.UNLIMITED)
            val collector = launch { observer.status.collect { updates.send(it.indicator) } }
            try {
                updates.await(SyncIndicatorState.GREEN)
                RoomSyncOutbox(source).recordChange(STATUS_ALICE, 0L,
                    SyncOperation.DeleteDeck("pending-live", "deck-a")) {}
                updates.await(SyncIndicatorState.YELLOW)
                source.current().syncConflictSnapshotDao().save(RoomSyncConflictSnapshot(
                    accountId = STATUS_ALICE, baseRevision = 0L, responseJson = "{}",
                ))
                updates.await(SyncIndicatorState.RED)
            } finally {
                collector.cancelAndJoin()
            }
        }
    }

    private suspend fun Channel<SyncIndicatorState>.await(expected: SyncIndicatorState) = withTimeout(2_000L) {
        while (receive() != expected) Unit
    }

    private suspend fun RoomSyncStatusObserver.await(indicator: SyncIndicatorState) = withTimeout(2_000L) {
        status.first { it.indicator == indicator }
    }

    private fun connected(email: String, revision: Long) = SyncEventFeedStatus(
        accountEmail = email,
        channel = SyncEventChannelState.CONNECTED,
        serverRevision = revision,
        devices = listOf(SyncEventDevice("device-a", "Desktop", "DESKTOP", true)),
    )

    private fun withSource(block: suspend (ActiveLocalRoomDatabase) -> Unit) = runBlocking {
        val tempRoot = Path.of(System.getProperty("java.io.tmpdir")).toRealPath()
        val directory = Files.createTempDirectory(tempRoot, "klaf-status-").toRealPath()
        require(directory.parent == tempRoot && directory.fileName.toString().startsWith("klaf-status-"))
        val source = ActiveLocalRoomDatabase(
            ScopedKlafRoomDatabaseFactory(directory.toFile()),
            DesktopSelectedAccountStore(directory.toFile()),
        )
        try {
            block(source)
        } finally {
            source.close()
            directory.toFile().deleteRecursively()
        }
    }
}
