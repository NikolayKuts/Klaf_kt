package com.kuts.klaf.room

import com.kuts.klaf.room.databases.ActiveLocalRoomDatabase
import com.kuts.klaf.room.databases.DesktopSelectedAccountStore
import com.kuts.klaf.room.databases.ScopedKlafRoomDatabaseFactory
import com.kuts.klaf.room.entities.RoomDeck
import com.kuts.klaf.room.repositoryImplementations.RoomSyncConflictStore
import com.kuts.klaf.room.repositoryImplementations.RoomSyncConflictResolver
import com.kuts.klaf.room.repositoryImplementations.RoomSyncDeltaApplier
import com.kuts.klaf.room.repositoryImplementations.RoomSyncOutbox
import com.kuts.klaf.room.repositoryImplementations.RoomSyncPartialApplier
import com.kuts.klaf.room.repositoryImplementations.UnresolvedSyncConflictException
import com.kuts.klaf.server.contract.SyncConflict
import com.kuts.klaf.server.contract.SyncDeck
import com.kuts.klaf.server.contract.SyncDelta
import com.kuts.klaf.server.contract.SyncOperation
import com.kuts.klaf.server.contract.SyncResponse
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlinx.coroutines.runBlocking

private const val PARTIAL_ACCOUNT = "alice@example.test"

class RoomSyncPartialApplierTest {

    @Test
    fun `disjoint deck addition is acknowledged while conflicting deck stays local`() = withSource { source ->
        val outbox = RoomSyncOutbox(source)
        val oldId = outbox.recordChange(PARTIAL_ACCOUNT, 0L, SyncOperation.EditDeck(
            "edit-a", "deck-a", "Local",
        )) {
            source.current().deckDao().insertNewDeck(deck("Local", "deck-a"))
        }.toInt()
        val newId = outbox.recordChange(PARTIAL_ACCOUNT, 0L, SyncOperation.AddDeck(
            "add-b", syncDeck("Independent", "deck-b"),
        )) {
            source.current().deckDao().insertNewDeck(deck("Independent", "deck-b"))
        }.toInt()
        val response = response()

        assertEquals(true, partial(source, outbox).tryApply(
            PARTIAL_ACCOUNT, outbox.pendingForAccount(PARTIAL_ACCOUNT), response,
        ))

        assertEquals(oldId, source.current().deckDao().getDeckBySyncId("deck-a")?.id)
        assertEquals("Local", source.current().deckDao().getDeckBySyncId("deck-a")?.name)
        assertEquals(newId, source.current().deckDao().getDeckBySyncId("deck-b")?.id)
        assertEquals(2L, outbox.confirmedRevision(PARTIAL_ACCOUNT))
        assertEquals(listOf("edit-a"), outbox.pendingForAccount(PARTIAL_ACCOUNT)
            .map { it.operation.operationId })
        assertEquals(response, RoomSyncConflictStore(source).current(PARTIAL_ACCOUNT))

        assertFailsWith<UnresolvedSyncConflictException> {
            outbox.recordChange(PARTIAL_ACCOUNT, 2L, SyncOperation.AddDeck(
                "late-add", syncDeck("Late", "deck-c"),
            )) {
                source.current().deckDao().insertNewDeck(deck("Late", "deck-c"))
            }
        }
        assertEquals(null, source.current().deckDao().getDeckBySyncId("deck-c"))
        assertEquals(listOf("edit-a"), outbox.pendingForAccount(PARTIAL_ACCOUNT)
            .map { it.operation.operationId })

        source.selectAccount("bob@example.test")
        outbox.recordChange("bob@example.test", 0L, SyncOperation.DeleteDeck("bob-edit", "bob-deck")) {}
        source.selectAccount(PARTIAL_ACCOUNT)
        assertFailsWith<UnresolvedSyncConflictException> {
            outbox.recordChange(PARTIAL_ACCOUNT, 2L, SyncOperation.DeleteDeck("late-delete", "deck-b")) {}
        }

        RoomSyncConflictResolver(source, outbox, RoomSyncDeltaApplier(source, outbox))
            .acceptServerForAll(PARTIAL_ACCOUNT)
        outbox.recordChange(PARTIAL_ACCOUNT, 2L, SyncOperation.AddDeck(
            "after-resolution", syncDeck("After", "deck-c"),
        )) {
            source.current().deckDao().insertNewDeck(deck("After", "deck-c"))
        }
        assertEquals("After", source.current().deckDao().getDeckBySyncId("deck-c")?.name)
    }

    @Test
    fun `structural conflict is not split automatically`() = withSource { source ->
        val outbox = RoomSyncOutbox(source)
        val deletion = SyncOperation.DeleteDeck("delete-a", "deck-a")
        outbox.recordChange(PARTIAL_ACCOUNT, 0L, deletion) {}
        outbox.recordChange(PARTIAL_ACCOUNT, 0L, SyncOperation.AddDeck(
            "add-b", syncDeck("Independent", "deck-b"),
        )) {}
        val response = response(conflictOperation = deletion)

        assertEquals(false, partial(source, outbox).tryApply(
            PARTIAL_ACCOUNT, outbox.pendingForAccount(PARTIAL_ACCOUNT), response,
        ))

        assertEquals(0L, outbox.confirmedRevision(PARTIAL_ACCOUNT))
        assertEquals(listOf("delete-a", "add-b"), outbox.pendingForAccount(PARTIAL_ACCOUNT)
            .map { it.operation.operationId })
        assertEquals(null, RoomSyncConflictStore(source).current(PARTIAL_ACCOUNT))
    }

    @Test
    fun `invalid accepted deck count rolls back partial acknowledgement and snapshot`() = withSource { source ->
        val outbox = RoomSyncOutbox(source)
        outbox.recordChange(PARTIAL_ACCOUNT, 0L, SyncOperation.EditDeck("edit-a", "deck-a", "Local")) {}
        outbox.recordChange(PARTIAL_ACCOUNT, 0L, SyncOperation.AddDeck(
            "add-b", syncDeck("Independent", "deck-b"),
        )) {}
        val response = response(acceptedDeck = syncDeck("Independent", "deck-b").copy(cardQuantity = 1))

        assertFails {
            partial(source, outbox).tryApply(PARTIAL_ACCOUNT, outbox.pendingForAccount(PARTIAL_ACCOUNT), response)
        }

        assertEquals(0L, outbox.confirmedRevision(PARTIAL_ACCOUNT))
        assertEquals(listOf("edit-a", "add-b"), outbox.pendingForAccount(PARTIAL_ACCOUNT)
            .map { it.operation.operationId })
        assertEquals(null, RoomSyncConflictStore(source).current(PARTIAL_ACCOUNT))
    }

    private fun withSource(block: suspend (ActiveLocalRoomDatabase) -> Unit) = runBlocking {
        val tempRoot = Path.of(System.getProperty("java.io.tmpdir")).toRealPath()
        val directory = Files.createTempDirectory(tempRoot, "klaf-partial-").toRealPath()
        require(directory.parent == tempRoot && directory.fileName.toString().startsWith("klaf-partial-"))
        val source = ActiveLocalRoomDatabase(
            ScopedKlafRoomDatabaseFactory(directory.toFile()),
            DesktopSelectedAccountStore(directory.toFile()),
        )
        try {
            source.selectAccount(PARTIAL_ACCOUNT)
            block(source)
        } finally {
            source.close()
            directory.toFile().deleteRecursively()
        }
    }

    private fun partial(source: ActiveLocalRoomDatabase, outbox: RoomSyncOutbox) = RoomSyncPartialApplier(
        source, outbox, RoomSyncDeltaApplier(source, outbox),
    )

    private fun response(
        conflictOperation: SyncOperation = SyncOperation.EditDeck("edit-a", "deck-a", "Local"),
        acceptedDeck: SyncDeck = syncDeck("Independent", "deck-b"),
    ) = SyncResponse(
        revision = 2L,
        acceptedOperationIds = setOf("add-b"),
        conflicts = listOf(SyncConflict(
            operationId = conflictOperation.operationId,
            reason = "CONCURRENT_CHANGE",
            localOperation = conflictOperation,
            serverChanges = emptyList(),
        )),
        delta = SyncDelta(0L, 2L,
            listOf(syncDeck("Remote", "deck-a"), acceptedDeck),
            emptyList(), emptyList(), emptyList(), emptyList(),
        ),
    )

    private fun syncDeck(name: String, syncId: String) = SyncDeck(syncId, name, 1L,
        lastChangedServerRevision = 2L)

    private fun deck(name: String, syncId: String) = RoomDeck(
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
