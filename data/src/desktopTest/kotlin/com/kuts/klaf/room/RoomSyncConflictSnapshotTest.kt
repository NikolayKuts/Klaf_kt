package com.kuts.klaf.room

import com.kuts.klaf.room.databases.ActiveLocalRoomDatabase
import com.kuts.klaf.room.databases.DesktopSelectedAccountStore
import com.kuts.klaf.room.databases.ScopedKlafRoomDatabaseFactory
import com.kuts.klaf.room.repositoryImplementations.RoomSyncConflictStore
import com.kuts.klaf.room.repositoryImplementations.RoomSyncDeltaApplier
import com.kuts.klaf.room.repositoryImplementations.RoomSyncOutbox
import com.kuts.klaf.server.contract.SyncConflict
import com.kuts.klaf.server.contract.SyncBootstrapResponse
import com.kuts.klaf.server.contract.SyncCard
import com.kuts.klaf.server.contract.SyncDeck
import com.kuts.klaf.server.contract.SyncDelta
import com.kuts.klaf.server.contract.SyncOperation
import com.kuts.klaf.server.contract.SyncResponse
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlinx.coroutines.runBlocking

private const val FIRST_ACCOUNT = "alice@example.test"
private const val SECOND_ACCOUNT = "bob@example.test"

class RoomSyncConflictSnapshotTest {

    @Test
    fun `conflict display names come from the selected account Room file`() = withDirectory { path ->
        val source = openSource(path)
        try {
            source.selectAccount(FIRST_ACCOUNT)
            RoomSyncDeltaApplier(source, RoomSyncOutbox(source)).applyInitialSnapshot(
                FIRST_ACCOUNT,
                SyncBootstrapResponse(
                    revision = 0L,
                    decks = listOf(SyncDeck("deck-a", "Words", 1L, cardQuantity = 1)),
                    cards = listOf(SyncCard("card-a", "deck-a", "native", "foreign")),
                ),
            )
            val store = RoomSyncConflictStore(source)
            val (deckNames, cardNames) = store.displayNames(FIRST_ACCOUNT)
            assertEquals("Words", deckNames["deck-a"])
            assertEquals("foreign", cardNames["card-a"])

            source.selectAccount(SECOND_ACCOUNT)
            val (otherDeckNames, otherCardNames) = store.displayNames(SECOND_ACCOUNT)
            assertEquals(null, otherDeckNames["deck-a"])
            assertEquals(null, otherCardNames["card-a"])
            assertFails { store.displayNames(FIRST_ACCOUNT) }

            source.selectAccount(FIRST_ACCOUNT)
            assertEquals("Words", store.displayNames(FIRST_ACCOUNT).first["deck-a"])
        } finally {
            source.close()
        }
    }

    @Test
    fun `conflict and pending operation survive restart but stay isolated from other accounts`() = withDirectory { path ->
        val source = openSource(path)
        val response = conflictResponse()
        try {
            source.selectAccount(FIRST_ACCOUNT)
            val outbox = RoomSyncOutbox(source)
            outbox.recordChange(FIRST_ACCOUNT, 0L, conflictOperation()) {}
            RoomSyncConflictStore(source).record(FIRST_ACCOUNT, setOf("edit-a"), response)
            assertEquals(0L, outbox.confirmedRevision(FIRST_ACCOUNT))
            source.selectAccount(SECOND_ACCOUNT)
            assertEquals(null, RoomSyncConflictStore(source).current(SECOND_ACCOUNT))
        } finally {
            source.close()
        }

        val reopened = openSource(path)
        try {
            reopened.selectAccount(FIRST_ACCOUNT)
            assertEquals(response, RoomSyncConflictStore(reopened).current(FIRST_ACCOUNT))
            assertEquals(listOf("edit-a"), RoomSyncOutbox(reopened).pendingForAccount(FIRST_ACCOUNT)
                .map { it.operation.operationId })
            assertEquals(0L, RoomSyncOutbox(reopened).confirmedRevision(FIRST_ACCOUNT))
        } finally {
            reopened.close()
        }
    }

    @Test
    fun `changed pending snapshot cannot persist a stale conflict response`() = withDirectory { path ->
        val source = openSource(path)
        try {
            source.selectAccount(FIRST_ACCOUNT)
            val outbox = RoomSyncOutbox(source)
            outbox.recordChange(FIRST_ACCOUNT, 0L, conflictOperation()) {}
            outbox.recordChange(FIRST_ACCOUNT, 0L, SyncOperation.DeleteDeck("late", "deck-b")) {}

            assertFails {
                RoomSyncConflictStore(source).record(FIRST_ACCOUNT, setOf("edit-a"), conflictResponse())
            }
            assertEquals(null, RoomSyncConflictStore(source).current(FIRST_ACCOUNT))
            assertEquals(2, outbox.pendingForAccount(FIRST_ACCOUNT).size)
        } finally {
            source.close()
        }
    }

    @Test
    fun `conflict snapshot clears with a successful delta and acknowledgement`() = withDirectory { path ->
        val source = openSource(path)
        try {
            source.selectAccount(FIRST_ACCOUNT)
            val outbox = RoomSyncOutbox(source)
            outbox.recordChange(FIRST_ACCOUNT, 0L, conflictOperation()) {}
            val store = RoomSyncConflictStore(source)
            store.record(FIRST_ACCOUNT, setOf("edit-a"), conflictResponse())

            RoomSyncDeltaApplier(source, outbox).applyConflictFree(FIRST_ACCOUNT, SyncResponse(
                revision = 1L,
                acceptedOperationIds = setOf("edit-a"),
                delta = emptyDelta(1L),
            ))

            assertEquals(null, store.current(FIRST_ACCOUNT))
            assertEquals(emptyList(), outbox.pendingForAccount(FIRST_ACCOUNT))
            assertEquals(1L, outbox.confirmedRevision(FIRST_ACCOUNT))
        } finally {
            source.close()
        }
    }

    @Test
    fun `failed delta keeps conflict snapshot pending operation and checkpoint together`() = withDirectory { path ->
        val source = openSource(path)
        try {
            source.selectAccount(FIRST_ACCOUNT)
            val outbox = RoomSyncOutbox(source)
            outbox.recordChange(FIRST_ACCOUNT, 0L, conflictOperation()) {}
            val store = RoomSyncConflictStore(source)
            val response = conflictResponse()
            store.record(FIRST_ACCOUNT, setOf("edit-a"), response)

            assertFails {
                RoomSyncDeltaApplier(source, outbox).applyConflictFree(FIRST_ACCOUNT, SyncResponse(
                    revision = 1L,
                    acceptedOperationIds = setOf("edit-a"),
                    delta = emptyDelta(1L).copy(cards = listOf(SyncCard(
                        syncId = "orphan-card",
                        deckSyncId = "missing-deck",
                        nativeWord = "native",
                        foreignWord = "word",
                    ))),
                ))
            }

            assertEquals(response, store.current(FIRST_ACCOUNT))
            assertEquals(listOf("edit-a"), outbox.pendingForAccount(FIRST_ACCOUNT).map { it.operation.operationId })
            assertEquals(0L, outbox.confirmedRevision(FIRST_ACCOUNT))
        } finally {
            source.close()
        }
    }

    private fun withDirectory(block: suspend (Path) -> Unit) = runBlocking {
        val tempRoot = Path.of(System.getProperty("java.io.tmpdir")).toRealPath()
        val directory = Files.createTempDirectory(tempRoot, "klaf-conflict-").toRealPath()
        require(directory.parent == tempRoot && directory.fileName.toString().startsWith("klaf-conflict-"))
        try {
            block(directory)
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    private fun openSource(directory: Path): ActiveLocalRoomDatabase = ActiveLocalRoomDatabase(
        ScopedKlafRoomDatabaseFactory(directory.toFile()),
        DesktopSelectedAccountStore(directory.toFile()),
    )

    private fun conflictOperation() = SyncOperation.EditDeck("edit-a", "deck-a", "Local")

    private fun conflictResponse() = SyncResponse(
        revision = 2L,
        conflicts = listOf(SyncConflict(
            operationId = "edit-a",
            reason = "CONCURRENT_CHANGE",
            localOperation = conflictOperation(),
            serverChanges = emptyList(),
        )),
        delta = emptyDelta(2L),
    )

    private fun emptyDelta(revision: Long) = SyncDelta(
        fromRevision = 0L,
        toRevision = revision,
        decks = emptyList(),
        cards = emptyList(),
        deletedDeckSyncIds = emptyList(),
        deletedCardSyncIds = emptyList(),
        history = emptyList(),
    )
}
