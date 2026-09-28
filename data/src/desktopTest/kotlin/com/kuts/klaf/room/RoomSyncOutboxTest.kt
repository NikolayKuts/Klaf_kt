package com.kuts.klaf.room

import com.kuts.klaf.room.databases.ActiveLocalRoomDatabase
import com.kuts.klaf.room.databases.DesktopSelectedAccountStore
import com.kuts.klaf.room.databases.ScopedKlafRoomDatabaseFactory
import com.kuts.klaf.room.entities.RoomSyncConflictSnapshot
import com.kuts.klaf.room.repositoryImplementations.RoomSyncOutbox
import com.kuts.klaf.room.repositoryImplementations.UnresolvedSyncConflictException
import com.kuts.klaf.server.contract.SyncOperation
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlinx.coroutines.runBlocking

class RoomSyncOutboxTest {

    @Test
    fun `local change and immutable operation survive database restart`() = withDatabase { directory, source ->
        val database = source.current()
        val outbox = RoomSyncOutbox(source)
        outbox.applyAccepted("account-a", emptyList(), newRevision = 4L) {}
        outbox.recordChange("account-a", 4L, SyncOperation.EditDeck("operation-a", "deck-a", "renamed")) {
            database.deckDao().insertDeck(deck("renamed", "deck-a"))
        }
        source.close()

        val reopened = openSource(directory)
        try {
            val pending = RoomSyncOutbox(reopened).pendingForAccount("account-a")
            assertEquals(1, reopened.current().deckDao().getAllDecks().size)
            assertEquals(1, pending.size)
            assertEquals(4L, pending.single().baseRevision)
            assertEquals(SyncOperation.EditDeck("operation-a", "deck-a", "renamed"), pending.single().operation)
        } finally {
            reopened.close()
        }
    }

    @Test
    fun `failed local change or duplicate operation rolls back both writes`() = withDatabase { _, source ->
        val database = source.current()
        val outbox = RoomSyncOutbox(source)
        assertFails {
            outbox.recordChange("account-a", 0L, SyncOperation.DeleteDeck("operation-a", "deck-a")) {
                database.deckDao().insertDeck(deck("temporary", "deck-a"))
                error("local write failed")
            }
        }
        assertEquals(emptyList(), database.deckDao().getAllDecks())
        assertEquals(emptyList(), outbox.pendingForAccount("account-a"))

        outbox.recordChange("account-a", 0L, SyncOperation.DeleteDeck("operation-a", "deck-a")) {
            database.deckDao().insertDeck(deck("first", "deck-a"))
        }
        assertFails {
            outbox.recordChange("account-a", 0L, SyncOperation.DeleteDeck("operation-a", "deck-b")) {
                database.deckDao().insertDeck(deck("second", "deck-b"))
            }
        }
        assertEquals(listOf("first"), database.deckDao().getAllDecks().map { it.name })
        assertEquals(listOf("operation-a"), outbox.pendingForAccount("account-a").map { it.operation.operationId })
    }

    @Test
    fun `pending operations remain ordered and isolated by account with individual revisions`() = withDatabase { _, source ->
        val outbox = RoomSyncOutbox(source)
        outbox.applyAccepted("account-a", emptyList(), newRevision = 3L) {}
        outbox.recordChange("account-a", 3L, SyncOperation.DeleteDeck("first", "deck-a")) {}
        source.selectAccount("account-b")
        outbox.applyAccepted("account-b", emptyList(), newRevision = 8L) {}
        outbox.recordChange("account-b", 8L, SyncOperation.DeleteDeck("other", "deck-b")) {}
        source.selectAccount("account-a")
        outbox.applyAccepted("account-a", emptyList(), newRevision = 5L) {}
        outbox.recordChange("account-a", 5L, SyncOperation.DeleteDeck("second", "deck-c")) {}

        assertEquals(listOf("first", "second"), outbox.pendingForAccount("account-a").map { it.operation.operationId })
        assertEquals(listOf(3L, 5L), outbox.pendingForAccount("account-a").map { it.baseRevision })
        outbox.applyAccepted("account-a", listOf("first", "other"), newRevision = 6L) {}
        assertEquals(listOf("second"), outbox.pendingForAccount("account-a").map { it.operation.operationId })
        source.selectAccount("account-b")
        assertEquals(listOf("other"), outbox.pendingForAccount("account-b").map { it.operation.operationId })
    }

    @Test
    fun `failed server delta keeps acknowledgements pending and rolls back local rows`() = withDatabase { _, source ->
        val database = source.current()
        val outbox = RoomSyncOutbox(source)
        outbox.applyAccepted("account-a", emptyList(), newRevision = 2L) {}
        outbox.recordChange("account-a", 2L, SyncOperation.DeleteDeck("operation-a", "deck-a")) {}

        assertFails {
            outbox.applyAccepted("account-a", listOf("operation-a"), newRevision = 3L) {
                database.deckDao().insertDeck(deck("remote", "deck-remote"))
                error("delta failed")
            }
        }

        assertEquals(emptyList(), database.deckDao().getAllDecks())
        assertEquals(listOf("operation-a"), outbox.pendingForAccount("account-a").map { it.operation.operationId })
        assertEquals(2L, outbox.confirmedRevision("account-a"))
    }

    @Test
    fun `local change refuses a base revision older than selected checkpoint`() = withDatabase { _, source ->
        val outbox = RoomSyncOutbox(source)
        outbox.applyAccepted("account-a", emptyList(), newRevision = 2L) {}

        assertFails {
            outbox.recordChange("account-a", 1L, SyncOperation.DeleteDeck("stale", "deck-a")) {
                source.current().deckDao().insertDeck(deck("must not be written", "deck-a"))
            }
        }

        assertEquals(emptyList(), source.current().deckDao().getAllDecks())
        assertEquals(emptyList(), outbox.pendingForAccount("account-a"))
        assertEquals(2L, outbox.confirmedRevision("account-a"))
    }

    @Test
    fun `confirmed revision persists per account and rejects backwards acknowledgement`() = withDatabase { directory, source ->
        val outbox = RoomSyncOutbox(source)
        assertEquals(0L, outbox.confirmedRevision("account-a"))
        outbox.applyAccepted("account-a", emptyList(), newRevision = 5L) {}
        assertEquals(5L, outbox.confirmedRevision("account-a"))
        assertFails {
            outbox.applyAccepted("account-a", emptyList(), newRevision = 4L) {}
        }
        assertEquals(5L, outbox.confirmedRevision("account-a"))

        source.selectAccount("account-b")
        assertEquals(0L, outbox.confirmedRevision("account-b"))
        source.close()

        val reopened = openSource(directory)
        try {
            assertEquals(5L, RoomSyncOutbox(reopened).confirmedRevision("account-a"))
        } finally {
            reopened.close()
        }
    }

    @Test
    fun `partial conflict edit pause survives restart before local write`() = withDatabase { directory, source ->
        val outbox = RoomSyncOutbox(source)
        outbox.applyAccepted("account-a", emptyList(), newRevision = 3L) {}
        source.current().syncConflictSnapshotDao().save(RoomSyncConflictSnapshot(
            accountId = "account-a",
            baseRevision = 1L,
            responseJson = "{}",
        ))
        source.close()

        val reopened = openSource(directory)
        try {
            val reopenedOutbox = RoomSyncOutbox(reopened)
            assertFailsWith<UnresolvedSyncConflictException> {
                reopenedOutbox.recordChange("account-a", 3L, SyncOperation.AddDeck(
                    "late-add", com.kuts.klaf.server.contract.SyncDeck("deck-a", "Late", 1L),
                )) {
                    reopened.current().deckDao().insertNewDeck(deck("Late", "deck-a"))
                }
            }
            assertEquals(emptyList(), reopened.current().deckDao().getAllDecks())
            assertEquals(emptyList(), reopenedOutbox.pendingForAccount("account-a"))
            assertEquals(3L, reopenedOutbox.confirmedRevision("account-a"))
        } finally {
            reopened.close()
        }
    }

    @Test
    fun `unapplied conflict at same checkpoint does not block ordinary edits`() = withDatabase { _, source ->
        source.current().syncConflictSnapshotDao().save(RoomSyncConflictSnapshot(
            accountId = "account-a",
            baseRevision = 0L,
            responseJson = "{}",
        ))
        val outbox = RoomSyncOutbox(source)

        outbox.recordChange("account-a", 0L, SyncOperation.DeleteDeck("edit-a", "deck-a")) {
            source.current().deckDao().insertNewDeck(deck("Allowed", "deck-a"))
        }

        assertEquals("Allowed", source.current().deckDao().getAllDecks().single().name)
        assertEquals(listOf("edit-a"), outbox.pendingForAccount("account-a").map { it.operation.operationId })
    }

    private fun withDatabase(block: suspend (Path, ActiveLocalRoomDatabase) -> Unit) = runBlocking {
        val tempRoot = Path.of(System.getProperty("java.io.tmpdir")).toRealPath()
        val directory = Files.createTempDirectory(tempRoot, "klaf-outbox-").toRealPath()
        require(directory.parent == tempRoot && directory.fileName.toString().startsWith("klaf-outbox-"))
        val source = openSource(directory)
        try {
            block(directory, source)
        } finally {
            source.close()
            directory.toFile().deleteRecursively()
        }
    }

    private suspend fun openSource(directory: Path): ActiveLocalRoomDatabase = ActiveLocalRoomDatabase(
        factory = ScopedKlafRoomDatabaseFactory(directory.toFile()),
        accountStore = DesktopSelectedAccountStore(directory.toFile()),
    ).also { source -> source.selectAccount("account-a") }

    private fun deck(name: String, syncId: String) = com.kuts.klaf.room.entities.RoomDeck(
        name = name,
        creationDate = 1L,
        repetitionIterationDates = emptyList(),
        scheduledIterationDates = emptyList(),
        scheduledDateInterval = 0L,
        repetitionQuantity = 0,
        cardQuantity = 0,
        lastFirstRepetitionDuration = 0L,
        lastSecondRepetitionDuration = 0L,
        lastRepetitionIterationDuration = 0L,
        isLastIterationSucceeded = true,
        syncId = syncId,
    )
}
