package com.kuts.klaf.room

import com.kuts.domain.entities.WordMeaningInsights
import com.kuts.domain.common.ConflictResolutionAction
import com.kuts.domain.common.ConflictResolutionDecision
import com.kuts.klaf.room.databases.ActiveLocalRoomDatabase
import com.kuts.klaf.room.databases.DesktopSelectedAccountStore
import com.kuts.klaf.room.databases.ScopedKlafRoomDatabaseFactory
import com.kuts.klaf.room.entities.RoomCard
import com.kuts.klaf.room.entities.RoomDeck
import com.kuts.klaf.room.repositoryImplementations.RoomSyncConflictResolver
import com.kuts.klaf.room.repositoryImplementations.RoomSyncConflictStore
import com.kuts.klaf.room.repositoryImplementations.RoomSyncDeltaApplier
import com.kuts.klaf.room.repositoryImplementations.RoomSyncOutbox
import com.kuts.klaf.server.contract.SyncCard
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
import kotlinx.coroutines.runBlocking

private const val CONFLICT_ACCOUNT = "alice@example.test"

class RoomSyncConflictResolverTest {

    @Test
    fun `server choice applies remote edit and keeps independently accepted local deck`() = withSource { source ->
        val database = source.current()
        val outbox = RoomSyncOutbox(source)
        val oldDeckId = database.deckDao().insertNewDeck(deck("Base", "deck-a")).toInt()
        outbox.recordChange(CONFLICT_ACCOUNT, 0L, SyncOperation.EditDeck("edit-a", "deck-a", "Local")) {
            database.deckDao().insertDeck(deck("Local", "deck-a").copy(id = oldDeckId))
        }
        val secondDeckId = outbox.recordChange(CONFLICT_ACCOUNT, 0L, SyncOperation.AddDeck(
            "add-b", syncDeck("Independent", "deck-b"),
        )) {
            database.deckDao().insertNewDeck(deck("Independent", "deck-b"))
        }.toInt()
        val response = response(
            accepted = setOf("add-b"),
            conflicts = listOf(conflict("edit-a", SyncOperation.EditDeck("edit-a", "deck-a", "Local"))),
            decks = listOf(syncDeck("Remote", "deck-a"), syncDeck("Independent", "deck-b")),
        )
        RoomSyncConflictStore(source).record(CONFLICT_ACCOUNT, setOf("edit-a", "add-b"), response)

        val revision = resolver(source, outbox).acceptServerForAll(CONFLICT_ACCOUNT)

        assertEquals(2L, revision)
        assertEquals("Remote", database.deckDao().getDeckBySyncId("deck-a")?.name)
        assertEquals(oldDeckId, database.deckDao().getDeckBySyncId("deck-a")?.id)
        assertEquals(secondDeckId, database.deckDao().getDeckBySyncId("deck-b")?.id)
        assertEquals(emptyList(), outbox.pendingForAccount(CONFLICT_ACCOUNT))
        assertEquals(2L, outbox.confirmedRevision(CONFLICT_ACCOUNT))
        assertEquals(null, RoomSyncConflictStore(source).current(CONFLICT_ACCOUNT))
    }

    @Test
    fun `server deck deletion removes child card and its conflicting local edit`() = withSource { source ->
        val database = source.current()
        val deckId = database.deckDao().insertNewDeck(deck("Base", "deck-a").copy(cardQuantity = 1)).toInt()
        database.cardDao().insertNewCard(card(deckId))
        val outbox = RoomSyncOutbox(source)
        val edit = SyncOperation.EditCard("edit-card", "card-a", SyncCard("card-a", "deck-a", "native", "Local"))
        outbox.recordChange(CONFLICT_ACCOUNT, 0L, edit) {
            val card = requireNotNull(database.cardDao().getCardBySyncId("card-a"))
            database.cardDao().insetCard(card.copy(foreignWord = "Local"))
        }
        RoomSyncConflictStore(source).record(CONFLICT_ACCOUNT, setOf("edit-card"), response(
            conflicts = listOf(conflict("edit-card", edit)),
            deletedDecks = listOf("deck-a"),
            deletedCards = listOf("card-a"),
        ))

        resolver(source, outbox).acceptServerForAll(CONFLICT_ACCOUNT)

        assertEquals(emptyList(), database.deckDao().getAllDecks())
        assertEquals(emptyList(), database.cardDao().getAllCards())
        assertEquals(emptyList(), outbox.pendingForAccount(CONFLICT_ACCOUNT))
    }

    @Test
    fun `new local operation after conflict prevents stale server choice`() = withSource { source ->
        val database = source.current()
        val outbox = RoomSyncOutbox(source)
        val edit = SyncOperation.EditDeck("edit-a", "deck-a", "Local")
        outbox.recordChange(CONFLICT_ACCOUNT, 0L, edit) {
            database.deckDao().insertNewDeck(deck("Local", "deck-a"))
        }
        val response = response(
            conflicts = listOf(conflict("edit-a", edit)),
            decks = listOf(syncDeck("Remote", "deck-a")),
        )
        RoomSyncConflictStore(source).record(CONFLICT_ACCOUNT, setOf("edit-a"), response)
        outbox.recordChange(CONFLICT_ACCOUNT, 0L, SyncOperation.DeleteDeck("late", "deck-b")) {}

        assertFails { resolver(source, outbox).acceptServerForAll(CONFLICT_ACCOUNT) }

        assertEquals("Local", database.deckDao().getDeckBySyncId("deck-a")?.name)
        assertEquals(0L, outbox.confirmedRevision(CONFLICT_ACCOUNT))
        assertEquals(listOf("edit-a", "late"), outbox.pendingForAccount(CONFLICT_ACCOUNT)
            .map { it.operation.operationId })
        assertEquals(response, RoomSyncConflictStore(source).current(CONFLICT_ACCOUNT))
    }

    @Test
    fun `invalid server delta rolls back resolution and preserves conflict`() = withSource { source ->
        val outbox = RoomSyncOutbox(source)
        val edit = SyncOperation.EditDeck("edit-a", "deck-a", "Local")
        outbox.recordChange(CONFLICT_ACCOUNT, 0L, edit) {}
        val response = response(
            conflicts = listOf(conflict("edit-a", edit)),
            cards = listOf(SyncCard("orphan", "missing-deck", "native", "word")),
        )
        RoomSyncConflictStore(source).record(CONFLICT_ACCOUNT, setOf("edit-a"), response)

        assertFails { resolver(source, outbox).acceptServerForAll(CONFLICT_ACCOUNT) }

        assertEquals(response, RoomSyncConflictStore(source).current(CONFLICT_ACCOUNT))
        assertEquals(0L, outbox.confirmedRevision(CONFLICT_ACCOUNT))
        assertEquals(listOf("edit-a"), outbox.pendingForAccount(CONFLICT_ACCOUNT)
            .map { it.operation.operationId })
    }

    @Test
    fun `keeping local deck name rebases it as a new operation after full server apply`() = withSource { source ->
        val database = source.current()
        val outbox = RoomSyncOutbox(source)
        val deckId = database.deckDao().insertNewDeck(deck("Base", "deck-a")).toInt()
        val edit = SyncOperation.EditDeck("edit-a", "deck-a", "Local")
        outbox.recordChange(CONFLICT_ACCOUNT, 0L, edit) {
            database.deckDao().insertDeck(deck("Local", "deck-a").copy(id = deckId))
        }
        val response = response(
            conflicts = listOf(conflict("edit-a", edit)),
            decks = listOf(syncDeck("Remote", "deck-a")),
        )
        RoomSyncConflictStore(source).record(CONFLICT_ACCOUNT, setOf("edit-a"), response)

        val revision = resolver(source, outbox).keepLocalDeckEdits(CONFLICT_ACCOUNT)

        assertEquals(2L, revision)
        assertEquals("Local", database.deckDao().getDeckBySyncId("deck-a")?.name)
        assertEquals(deckId, database.deckDao().getDeckBySyncId("deck-a")?.id)
        assertEquals(2L, outbox.confirmedRevision(CONFLICT_ACCOUNT))
        val rebased = outbox.pendingForAccount(CONFLICT_ACCOUNT).single()
        assertEquals(2L, rebased.baseRevision)
        assertEquals(true, rebased.operation.operationId != "edit-a")
        assertEquals("Local", (rebased.operation as SyncOperation.EditDeck).name)
        assertEquals(null, RoomSyncConflictStore(source).current(CONFLICT_ACCOUNT))
    }

    @Test
    fun `unsupported structural keep-local choice leaves conflict untouched`() = withSource { source ->
        val outbox = RoomSyncOutbox(source)
        val deletion = SyncOperation.DeleteDeck("delete-a", "deck-a")
        outbox.recordChange(CONFLICT_ACCOUNT, 0L, deletion) {}
        val response = response(conflicts = listOf(conflict("delete-a", deletion)))
        RoomSyncConflictStore(source).record(CONFLICT_ACCOUNT, setOf("delete-a"), response)

        assertFails { resolver(source, outbox).keepLocalDeckEdits(CONFLICT_ACCOUNT) }

        assertEquals(response, RoomSyncConflictStore(source).current(CONFLICT_ACCOUNT))
        assertEquals(0L, outbox.confirmedRevision(CONFLICT_ACCOUNT))
        assertEquals(listOf("delete-a"), outbox.pendingForAccount(CONFLICT_ACCOUNT)
            .map { it.operation.operationId })
    }

    @Test
    fun `keep local card edit cannot silently restore a server deleted deck`() = withSource { source ->
        val database = source.current()
        val deckId = database.deckDao().insertNewDeck(deck("Base", "deck-a").copy(cardQuantity = 1)).toInt()
        database.cardDao().insertNewCard(card(deckId))
        val outbox = RoomSyncOutbox(source)
        val edit = SyncOperation.EditCard(
            "edit-card", "card-a", SyncCard("card-a", "deck-a", "native", "Local"),
        )
        outbox.recordChange(CONFLICT_ACCOUNT, 0L, edit) {
            val card = requireNotNull(database.cardDao().getCardBySyncId("card-a"))
            database.cardDao().insetCard(card.copy(foreignWord = "Local"))
        }
        val response = response(
            conflicts = listOf(conflict("edit-card", edit)),
            deletedDecks = listOf("deck-a"),
            deletedCards = listOf("card-a"),
        )
        RoomSyncConflictStore(source).record(CONFLICT_ACCOUNT, setOf("edit-card"), response)

        assertFails { resolver(source, outbox).keepLocalCardEdits(CONFLICT_ACCOUNT) }

        assertEquals("Local", database.cardDao().getCardBySyncId("card-a")?.foreignWord)
        assertEquals(0L, outbox.confirmedRevision(CONFLICT_ACCOUNT))
        assertEquals(listOf("edit-card"), outbox.pendingForAccount(CONFLICT_ACCOUNT)
            .map { it.operation.operationId })
        assertEquals(response, RoomSyncConflictStore(source).current(CONFLICT_ACCOUNT))
    }

    @Test
    fun `moved card rescue rejects a destination reviewed on the server`() = withSource { source ->
        val database = source.current()
        database.deckDao().insertNewDeck(deck("Source", "deck-a"))
        val targetId = database.deckDao().insertNewDeck(deck("Target", "deck-b").copy(cardQuantity = 1)).toInt()
        database.cardDao().insertNewCard(card(targetId))
        val outbox = RoomSyncOutbox(source)
        val move = SyncOperation.MoveCard("move-card", "card-a", "deck-a", "deck-b")
        outbox.recordChange(CONFLICT_ACCOUNT, 0L, move) {}
        val response = response(
            conflicts = listOf(conflict("move-card", move)),
            decks = listOf(syncDeck("Target", "deck-b").copy(reviewCount = 1)),
            deletedDecks = listOf("deck-a"),
            deletedCards = listOf("card-a"),
        )
        RoomSyncConflictStore(source).record(CONFLICT_ACCOUNT, setOf("move-card"), response)

        assertFails { resolver(source, outbox).rescueMovedCardsAfterSourceDeletion(CONFLICT_ACCOUNT) }

        assertEquals(targetId, database.cardDao().getCardBySyncId("card-a")?.deckId)
        assertEquals(0L, outbox.confirmedRevision(CONFLICT_ACCOUNT))
        assertEquals(listOf("move-card"), outbox.pendingForAccount(CONFLICT_ACCOUNT)
            .map { it.operation.operationId })
        assertEquals(response, RoomSyncConflictStore(source).current(CONFLICT_ACCOUNT))
    }

    @Test
    fun `mixed manual choices keep one local deck edit and accept server card edit`() = withSource { source ->
        val database = source.current()
        val deckAId = database.deckDao().insertNewDeck(deck("Base A", "deck-a")).toInt()
        val deckBId = database.deckDao().insertNewDeck(deck("Base B", "deck-b").copy(cardQuantity = 1)).toInt()
        database.cardDao().insertNewCard(card(deckBId))
        val outbox = RoomSyncOutbox(source)
        val deckEdit = SyncOperation.EditDeck("edit-deck", "deck-a", "Local A")
        val cardEdit = SyncOperation.EditCard(
            "edit-card", "card-a", SyncCard("card-a", "deck-b", "native", "Local card"),
        )
        outbox.recordChange(CONFLICT_ACCOUNT, 0L, deckEdit) {
            database.deckDao().insertDeck(deck("Local A", "deck-a").copy(id = deckAId))
        }
        outbox.recordChange(CONFLICT_ACCOUNT, 0L, cardEdit) {
            database.cardDao().insetCard(requireNotNull(database.cardDao().getCardBySyncId("card-a"))
                .copy(foreignWord = "Local card"))
        }
        val response = response(
            conflicts = listOf(
                conflict("edit-deck", deckEdit).copy(serverDeck = syncDeck("Remote A", "deck-a")),
                conflict("edit-card", cardEdit).copy(
                    serverCard = SyncCard("card-a", "deck-b", "native", "Remote card"),
                ),
            ),
            decks = listOf(syncDeck("Remote A", "deck-a")),
            cards = listOf(SyncCard("card-a", "deck-b", "native", "Remote card")),
        )
        RoomSyncConflictStore(source).record(CONFLICT_ACCOUNT, setOf("edit-deck", "edit-card"), response)

        val revision = resolver(source, outbox).resolveSelected(CONFLICT_ACCOUNT, listOf(
            ConflictResolutionDecision("edit-deck", ConflictResolutionAction.KEEP_LOCAL_DECK),
            ConflictResolutionDecision("edit-card", ConflictResolutionAction.ACCEPT_SERVER),
        ))

        assertEquals(2L, revision)
        assertEquals("Local A", database.deckDao().getDeckBySyncId("deck-a")?.name)
        assertEquals(deckAId, database.deckDao().getDeckBySyncId("deck-a")?.id)
        assertEquals("Remote card", database.cardDao().getCardBySyncId("card-a")?.foreignWord)
        assertEquals(2L, outbox.confirmedRevision(CONFLICT_ACCOUNT))
        val pending = outbox.pendingForAccount(CONFLICT_ACCOUNT).single()
        assertEquals(2L, pending.baseRevision)
        assertEquals("Local A", (pending.operation as SyncOperation.EditDeck).name)
        assertEquals(null, RoomSyncConflictStore(source).current(CONFLICT_ACCOUNT))
    }

    @Test
    fun `missing duplicate unknown or unsupported manual choices leave conflict untouched`() = withSource { source ->
        val database = source.current()
        val deckId = database.deckDao().insertNewDeck(deck("Local", "deck-a")).toInt()
        val outbox = RoomSyncOutbox(source)
        val edit = SyncOperation.EditDeck("edit-a", "deck-a", "Local")
        outbox.recordChange(CONFLICT_ACCOUNT, 0L, edit) {}
        val response = response(
            conflicts = listOf(conflict("edit-a", edit).copy(serverDeck = syncDeck("Remote", "deck-a"))),
            decks = listOf(syncDeck("Remote", "deck-a")),
        )
        RoomSyncConflictStore(source).record(CONFLICT_ACCOUNT, setOf("edit-a"), response)
        val resolver = resolver(source, outbox)

        assertFails { resolver.resolveSelected(CONFLICT_ACCOUNT, emptyList()) }
        assertFails { resolver.resolveSelected(CONFLICT_ACCOUNT, listOf(
            ConflictResolutionDecision("edit-a", ConflictResolutionAction.ACCEPT_SERVER),
            ConflictResolutionDecision("edit-a", ConflictResolutionAction.KEEP_LOCAL_DECK),
        )) }
        assertFails { resolver.resolveSelected(CONFLICT_ACCOUNT, listOf(
            ConflictResolutionDecision("unknown", ConflictResolutionAction.ACCEPT_SERVER),
        )) }
        assertFails { resolver.resolveSelected(CONFLICT_ACCOUNT, listOf(
            ConflictResolutionDecision("edit-a", ConflictResolutionAction.KEEP_LOCAL_CARD),
        )) }
        assertFails { resolver.resolveSelected(CONFLICT_ACCOUNT, listOf(
            ConflictResolutionDecision("edit-a", ConflictResolutionAction.RESCUE_MOVED_CARD),
        )) }

        assertEquals("Local", database.deckDao().getDeckBySyncId("deck-a")?.name)
        assertEquals(deckId, database.deckDao().getDeckBySyncId("deck-a")?.id)
        assertEquals(0L, outbox.confirmedRevision(CONFLICT_ACCOUNT))
        assertEquals(listOf("edit-a"), outbox.pendingForAccount(CONFLICT_ACCOUNT)
            .map { it.operation.operationId })
        assertEquals(response, RoomSyncConflictStore(source).current(CONFLICT_ACCOUNT))
    }

    private fun withSource(block: suspend (ActiveLocalRoomDatabase) -> Unit) = runBlocking {
        val tempRoot = Path.of(System.getProperty("java.io.tmpdir")).toRealPath()
        val directory = Files.createTempDirectory(tempRoot, "klaf-resolve-").toRealPath()
        require(directory.parent == tempRoot && directory.fileName.toString().startsWith("klaf-resolve-"))
        val source = ActiveLocalRoomDatabase(
            ScopedKlafRoomDatabaseFactory(directory.toFile()),
            DesktopSelectedAccountStore(directory.toFile()),
        )
        try {
            source.selectAccount(CONFLICT_ACCOUNT)
            block(source)
        } finally {
            source.close()
            directory.toFile().deleteRecursively()
        }
    }

    private fun resolver(source: ActiveLocalRoomDatabase, outbox: RoomSyncOutbox) = RoomSyncConflictResolver(
        source, outbox, RoomSyncDeltaApplier(source, outbox),
    )

    private fun conflict(id: String, operation: SyncOperation) = SyncConflict(
        operationId = id,
        reason = "CONCURRENT_CHANGE",
        localOperation = operation,
        serverChanges = emptyList(),
    )

    private fun response(
        accepted: Set<String> = emptySet(),
        conflicts: List<SyncConflict>,
        decks: List<SyncDeck> = emptyList(),
        cards: List<SyncCard> = emptyList(),
        deletedDecks: List<String> = emptyList(),
        deletedCards: List<String> = emptyList(),
    ) = SyncResponse(
        revision = 2L,
        acceptedOperationIds = accepted,
        conflicts = conflicts,
        delta = SyncDelta(0L, 2L, decks, cards, deletedDecks, deletedCards, emptyList()),
    )

    private fun syncDeck(name: String, syncId: String) = SyncDeck(syncId, name, 1L, lastChangedServerRevision = 2L)

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

    private fun card(deckId: Int) = RoomCard(
        deckId = deckId,
        nativeWord = "native",
        foreignWord = "Base",
        ipa = "[]",
        wordMeaningInsights = WordMeaningInsights.EMPTY,
        mnemonicJson = "{}",
        syncId = "card-a",
    )
}
