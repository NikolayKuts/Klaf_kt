package com.kuts.klaf.room

import com.kuts.klaf.room.databases.ActiveLocalRoomDatabase
import com.kuts.klaf.room.databases.DesktopSelectedAccountStore
import com.kuts.klaf.room.databases.ScopedKlafRoomDatabaseFactory
import com.kuts.klaf.room.repositoryImplementations.ManualRoomSyncCoordinator
import com.kuts.klaf.room.repositoryImplementations.ManualSyncAttemptState
import com.kuts.klaf.room.repositoryImplementations.ManualSyncResult
import com.kuts.klaf.room.databases.ManualSyncInProgressException
import com.kuts.klaf.room.repositoryImplementations.DeckRepositoryRoom
import com.kuts.klaf.room.repositoryImplementations.RoomSyncDeltaApplier
import com.kuts.klaf.room.repositoryImplementations.RoomSyncOutbox
import com.kuts.klaf.room.repositoryImplementations.StorageTransactionRepositoryRoom
import com.kuts.klaf.room.entities.RoomDeck
import com.kuts.domain.entities.Deck
import com.kuts.klaf.server.contract.SyncDelta
import com.kuts.klaf.server.contract.SyncBootstrapResponse
import com.kuts.klaf.server.contract.SyncCard
import com.kuts.klaf.server.contract.SyncDeck
import com.kuts.klaf.server.contract.SyncOperation
import com.kuts.klaf.server.contract.accountInterimDeckSyncId
import com.kuts.klaf.server.contract.SyncResponse
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

private const val ATTEMPT_ACCOUNT = "alice@example.test"

class ManualRoomSyncAttemptStateTest {

    @Test
    fun `image preparation runs before publishing metadata and failed uploads retain pending changes`() = withSource { source ->
        val outbox = RoomSyncOutbox(source)
        outbox.recordChange(ATTEMPT_ACCOUNT, 0L, SyncOperation.AddDeck("image-batch", SyncDeck("image-deck", "Image deck", 1L))) {
            source.current().deckDao().insertNewDeck(RoomDeck("Image deck", 1L, emptyList(), emptyList(), 0, 0, 0, 0, 0, 0, true,
                syncId = "image-deck"))
        }
        val events = mutableListOf<String>()
        var fail = true
        val coordinator = ManualRoomSyncCoordinator(
            source, outbox, RoomSyncDeltaApplier(source, outbox), "device-a",
            sendRequest = { request ->
                events += "metadata"
                SyncResponse(0L, request.operations.map { it.operationId }.toSet(), delta = SyncDelta(0L, 0L,
                    emptyList(), emptyList(), emptyList(), emptyList(), emptyList()))
            },
            confirmAppliedRevision = { _, _, _ -> },
            prepareImages = {
                events += "image"
                if (fail) error("upload interrupted")
            },
        )
        assertFailsWith<IllegalStateException> { coordinator.synchronize() }
        assertEquals(listOf("image"), events)
        assertEquals("image-batch", outbox.pendingForAccount(ATTEMPT_ACCOUNT).single().operation.operationId)
        fail = false
        coordinator.synchronize()
        assertEquals(listOf("image", "image", "metadata"), events)
        assertEquals(0, outbox.pendingForAccount(ATTEMPT_ACCOUNT).size)
    }

    @Test
    fun `first sync with local edits does not silently skip imported server baseline`() = withSource { source ->
        val outbox = RoomSyncOutbox(source)
        outbox.recordChange(
            ATTEMPT_ACCOUNT,
            0L,
            SyncOperation.AddDeck("local-add", SyncDeck("local-deck", "Local", 1L)),
        ) {
            source.current().deckDao().insertNewDeck(RoomDeck(
                name = "Local",
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
                syncId = "local-deck",
            ))
        }
        var sentChanges = false
        val coordinator = ManualRoomSyncCoordinator(
            databaseSource = source,
            outbox = outbox,
            applier = RoomSyncDeltaApplier(source, outbox),
            deviceId = "device-a",
            sendRequest = { sentChanges = true; error("Must not upload before downloading the baseline") },
            confirmAppliedRevision = { _, _, _ -> },
            fetchBootstrap = { _, _ -> SyncBootstrapResponse(
                0L, listOf(SyncDeck("imported", "Imported", 2L)), emptyList(),
            ) },
        )

        val failure = assertFailsWith<IllegalStateException> { coordinator.synchronize() }
        assertEquals(false, sentChanges)
        assertEquals(true, failure.message.orEmpty().contains("initial server snapshot"))
        assertEquals(listOf("Local"), source.current().deckDao().getAllDecks().map(RoomDeck::name))
        assertEquals(listOf("local-add"), outbox.pendingForAccount(ATTEMPT_ACCOUNT).map { it.operation.operationId })
        assertEquals(null, source.current().syncCheckpointDao().current())
    }

    @Test
    fun `first download replaces queued empty interim deck without duplicating it`() = withSource { source ->
        val outbox = RoomSyncOutbox(source)
        val interimId = accountInterimDeckSyncId(ATTEMPT_ACCOUNT)
        outbox.recordChange(
            ATTEMPT_ACCOUNT,
            0L,
            SyncOperation.AddDeck("local-interim", SyncDeck(interimId, "interim deck", 1L)),
        ) {
            source.current().deckDao().insertNewDeck(RoomDeck(
                name = "interim deck",
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
                id = Deck.INTERIM_DECK_ID,
                syncId = interimId,
            ))
        }
        var deltaRequests = 0
        val coordinator = ManualRoomSyncCoordinator(
            databaseSource = source,
            outbox = outbox,
            applier = RoomSyncDeltaApplier(source, outbox),
            deviceId = "device-a",
            sendRequest = { deltaRequests++; error("Revision-zero baseline has no delta") },
            confirmAppliedRevision = { _, _, _ -> },
            fetchBootstrap = { _, _ -> SyncBootstrapResponse(
                0L,
                listOf(SyncDeck(interimId, "interim deck", 1L), SyncDeck("imported", "Imported", 2L)),
                emptyList(),
            ) },
        )

        assertEquals(ManualSyncResult.Applied(0L), coordinator.synchronize())
        assertEquals(0, deltaRequests)
        assertEquals(emptyList(), outbox.pendingForAccount(ATTEMPT_ACCOUNT))
        assertEquals(setOf(interimId, "imported"),
            source.current().deckDao().getAllDecks().map(RoomDeck::syncId).toSet())
        assertEquals(Deck.INTERIM_DECK_ID, source.current().deckDao().getDeckBySyncId(interimId)?.id)
    }

    @Test
    fun `first manual sync downloads imported revision zero snapshot`() = withSource { source ->
        val outbox = RoomSyncOutbox(source)
        var snapshotReads = 0
        var deltaRequests = 0
        val coordinator = ManualRoomSyncCoordinator(
            databaseSource = source,
            outbox = outbox,
            applier = RoomSyncDeltaApplier(source, outbox),
            deviceId = "device-a",
            sendRequest = { request ->
                deltaRequests++
                SyncResponse(0L, delta = SyncDelta(request.baseRevision, 0L,
                    emptyList(), emptyList(), emptyList(), emptyList(), emptyList()))
            },
            confirmAppliedRevision = { _, _, revision -> assertEquals(0L, revision) },
            fetchBootstrap = { account, device ->
                assertEquals(ATTEMPT_ACCOUNT, account)
                assertEquals("device-a", device)
                snapshotReads++
                SyncBootstrapResponse(
                    revision = 0L,
                    decks = listOf(SyncDeck("deck-imported", "Imported", 1L, cardQuantity = 1)),
                    cards = listOf(SyncCard("card-imported", "deck-imported", "слово", "word")),
                )
            },
        )

        assertEquals(ManualSyncResult.Applied(0L), coordinator.synchronize())
        assertEquals("Imported", source.current().deckDao().getDeckBySyncId("deck-imported")?.name)
        assertEquals("word", source.current().cardDao().getCardBySyncId("card-imported")?.foreignWord)
        assertEquals(0L, outbox.confirmedRevision(ATTEMPT_ACCOUNT))
        assertEquals(1, snapshotReads)
        assertEquals(0, deltaRequests)

        assertEquals(ManualSyncResult.Applied(0L), coordinator.synchronize())
        assertEquals(1, snapshotReads)
        assertEquals(1, deltaRequests)
    }

    @Test
    fun `device identity is resolved when manual synchronization starts`() = withSource { source ->
        val outbox = RoomSyncOutbox(source)
        var deviceReads = 0
        val coordinator = ManualRoomSyncCoordinator(
            databaseSource = source,
            outbox = outbox,
            applier = RoomSyncDeltaApplier(source, outbox),
            deviceIdProvider = { deviceReads++; "device-a" },
            sendRequest = { request ->
                assertEquals("device-a", request.deviceId)
                SyncResponse(0L, delta = SyncDelta(0L, 0L,
                    emptyList(), emptyList(), emptyList(), emptyList(), emptyList()))
            },
            confirmAppliedRevision = { _, _, _ -> },
        )
        assertEquals(0, deviceReads)

        assertIs<ManualSyncResult.Applied>(coordinator.synchronize())
        assertEquals(1, deviceReads)
    }

    @Test
    fun `account edits are paused during network sync and resume afterward`() = withSource { source ->
        val requestStarted = CompletableDeferred<Unit>()
        val reply = CompletableDeferred<SyncResponse>()
        val coordinator = coordinator(source) {
            requestStarted.complete(Unit)
            reply.await()
        }
        val transactions = StorageTransactionRepositoryRoom(source)
        val decks = DeckRepositoryRoom(source)

        val attempt = async { coordinator.synchronize() }
        requestStarted.await()
        assertFailsWith<ManualSyncInProgressException> {
            transactions.performWithTransaction { decks.insertDeck(Deck("Blocked", 1L)) }
        }
        assertEquals(emptyList(), decks.fetchAllDecks())
        reply.complete(SyncResponse(0L, delta = SyncDelta(0L, 0L,
            emptyList(), emptyList(), emptyList(), emptyList(), emptyList())))
        assertIs<ManualSyncResult.Applied>(attempt.await())

        transactions.performWithTransaction { decks.insertDeck(Deck("Allowed", 2L)) }
        assertEquals(listOf("Allowed"), decks.fetchAllDecks().map(Deck::name))
    }

    @Test
    fun `cancelling a manual request always releases the account edit pause`() = withSource { source ->
        val requestStarted = CompletableDeferred<Unit>()
        val coordinator = coordinator(source) {
            requestStarted.complete(Unit)
            awaitCancellation()
        }
        val attempt = async { coordinator.synchronize() }
        requestStarted.await()
        attempt.cancelAndJoin()

        StorageTransactionRepositoryRoom(source).performWithTransaction {
            DeckRepositoryRoom(source).insertDeck(Deck("After cancellation", 1L))
        }
        assertEquals(listOf("After cancellation"), DeckRepositoryRoom(source).fetchAllDecks().map(Deck::name))
    }

    @Test
    fun `timed out manual request releases account edit pause and signout state`() = withSource { source ->
        val requestStarted = CompletableDeferred<Unit>()
        val coordinator = coordinator(source) {
            requestStarted.complete(Unit)
            awaitCancellation()
        }

        assertFailsWith<TimeoutCancellationException> {
            withTimeout(1_000L) {
                coordinator.synchronize()
            }
        }
        assertEquals(true, requestStarted.isCompleted)
        assertEquals(ManualSyncAttemptState.Idle, coordinator.attemptState.value)

        StorageTransactionRepositoryRoom(source).performWithTransaction {
            DeckRepositoryRoom(source).insertDeck(Deck("After timeout", 1L))
        }
        assertEquals(listOf("After timeout"), DeckRepositoryRoom(source).fetchAllDecks().map(Deck::name))
    }

    @Test
    fun `device confirmation follows local checkpoint and can be retried after network failure`() = withSource { source ->
        val outbox = RoomSyncOutbox(source)
        val response = SyncResponse(1L, delta = SyncDelta(0L, 1L,
            emptyList(), emptyList(), emptyList(), emptyList(), emptyList()))
        val confirmationCalls = mutableListOf<Long>()
        val failing = ManualRoomSyncCoordinator(
            source, outbox, RoomSyncDeltaApplier(source, outbox), "device-a",
            sendRequest = { response },
            confirmAppliedRevision = { account, device, revision ->
                assertEquals(ATTEMPT_ACCOUNT, account)
                assertEquals("device-a", device)
                assertEquals(1L, outbox.confirmedRevision(account))
                confirmationCalls += revision
                error("confirmation response lost")
            },
        )

        assertFails { failing.synchronize() }
        assertEquals(listOf(1L), confirmationCalls)
        assertEquals(1L, outbox.confirmedRevision(ATTEMPT_ACCOUNT))
        assertEquals(ManualSyncAttemptState.Failed(ATTEMPT_ACCOUNT), failing.attemptState.value)

        val retry = ManualRoomSyncCoordinator(
            source, outbox, RoomSyncDeltaApplier(source, outbox), "device-a",
            sendRequest = { request ->
                assertEquals(1L, request.baseRevision)
                SyncResponse(1L, delta = SyncDelta(1L, 1L,
                    emptyList(), emptyList(), emptyList(), emptyList(), emptyList()))
            },
            confirmAppliedRevision = { account, device, revision ->
                assertEquals(ATTEMPT_ACCOUNT, account)
                assertEquals("device-a", device)
                assertEquals(1L, outbox.confirmedRevision(account))
                confirmationCalls += revision
            },
        )

        assertEquals(ManualSyncResult.Applied(1L), retry.synchronize())
        assertEquals(listOf(1L, 1L), confirmationCalls)
        assertEquals(ManualSyncAttemptState.Idle, retry.attemptState.value)
    }

    @Test
    fun `manual coordinator reports running success and failed account states`() = withSource { source ->
        val reply = CompletableDeferred<SyncResponse>()
        val coordinator = coordinator(source) { reply.await() }
        assertEquals(ManualSyncAttemptState.Idle, coordinator.attemptState.value)

        val sync = async { coordinator.synchronize() }
        assertEquals(ManualSyncAttemptState.Running(ATTEMPT_ACCOUNT), withTimeout(2_000L) {
            coordinator.attemptState.first { it is ManualSyncAttemptState.Running }
        })
        reply.complete(SyncResponse(0L, delta = SyncDelta(0L, 0L,
            emptyList(), emptyList(), emptyList(), emptyList(), emptyList())))
        assertIs<ManualSyncResult.Applied>(sync.await())
        assertEquals(ManualSyncAttemptState.Idle, coordinator.attemptState.value)

        val failing = coordinator(source) { error("network down") }
        assertFails { failing.synchronize() }
        assertEquals(ManualSyncAttemptState.Failed(ATTEMPT_ACCOUNT), failing.attemptState.value)
    }

    private fun coordinator(
        source: ActiveLocalRoomDatabase,
        send: suspend (com.kuts.klaf.server.contract.SyncRequest) -> SyncResponse,
    ): ManualRoomSyncCoordinator {
        val outbox = RoomSyncOutbox(source)
        return ManualRoomSyncCoordinator(
            source, outbox, RoomSyncDeltaApplier(source, outbox), "device-a", send,
            confirmAppliedRevision = { _, _, _ -> },
        )
    }

    private fun withSource(block: suspend kotlinx.coroutines.CoroutineScope.(ActiveLocalRoomDatabase) -> Unit) = runBlocking {
        val tempRoot = Path.of(System.getProperty("java.io.tmpdir")).toRealPath()
        val directory = Files.createTempDirectory(tempRoot, "klaf-attempt-").toRealPath()
        require(directory.parent == tempRoot && directory.fileName.toString().startsWith("klaf-attempt-"))
        val source = ActiveLocalRoomDatabase(
            ScopedKlafRoomDatabaseFactory(directory.toFile()),
            DesktopSelectedAccountStore(directory.toFile()),
        )
        try {
            source.selectAccount(ATTEMPT_ACCOUNT)
            block(source)
        } finally {
            source.close()
            directory.toFile().deleteRecursively()
        }
    }
}
