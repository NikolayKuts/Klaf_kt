package com.kuts.klaf.room

import com.kuts.klaf.room.databases.ActiveLocalRoomDatabase
import com.kuts.klaf.room.databases.DesktopSelectedAccountStore
import com.kuts.klaf.room.databases.ScopedKlafRoomDatabaseFactory
import com.kuts.klaf.room.entities.RoomCard
import com.kuts.klaf.room.entities.RoomDeck
import com.kuts.klaf.room.repositoryImplementations.RoomSyncDeltaApplier
import com.kuts.klaf.room.repositoryImplementations.RoomSyncOutbox
import com.kuts.klaf.server.contract.SyncCard
import com.kuts.klaf.server.contract.SyncConflict
import com.kuts.klaf.server.contract.SyncDeck
import com.kuts.klaf.server.contract.SyncDelta
import com.kuts.klaf.server.contract.SyncOperation
import com.kuts.klaf.server.contract.SyncResponse
import com.kuts.klaf.server.contract.accountInterimDeckSyncId
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlinx.coroutines.runBlocking

class RoomSyncDeltaApplierTest {

    @Test
    fun `downloaded account interim keeps reserved local navigation ID`() = withDatabase { source ->
        val interimId = accountInterimDeckSyncId("account-a")
        RoomSyncDeltaApplier(source, RoomSyncOutbox(source)).applyConflictFree("account-a", response(
            revision = 1L,
            decks = listOf(syncDeck("interim deck", syncId = interimId)),
        ))

        assertEquals(-1, source.current().deckDao().getDeckBySyncId(interimId)?.id)
        assertEquals(1, source.current().deckDao().getAllDecks().size)
    }

    @Test
    fun `remote update keeps local deck and card IDs while acknowledging own change`() = withDatabase { source ->
        val database = source.current()
        val outbox = RoomSyncOutbox(source)
        val applier = RoomSyncDeltaApplier(source, outbox)
        val deckId = database.deckDao().insertNewDeck(deck("old")).toInt()
        val cardId = database.cardDao().insertNewCard(card(deckId)).toInt()
        outbox.recordChange("account-a", 0L, SyncOperation.EditDeck("edit-a", "deck-a", "new")) {
            database.deckDao().insertDeck(deck("new").copy(id = deckId))
        }

        applier.applyConflictFree("account-a", response(
            revision = 1L,
            accepted = setOf("edit-a"),
            decks = listOf(syncDeck("new", cardQuantity = 1, revision = 1L)),
            cards = listOf(syncCard("new word", revision = 1L)),
        ))

        assertEquals(deckId, database.deckDao().getDeckBySyncId("deck-a")?.id)
        assertEquals(cardId, database.cardDao().getCardBySyncId("card-a")?.id)
        assertEquals("new word", database.cardDao().getCardBySyncId("card-a")?.foreignWord)
        assertEquals(1L, database.deckDao().getDeckBySyncId("deck-a")?.lastChangedServerRevision)
        assertEquals(1L, outbox.confirmedRevision("account-a"))
        assertEquals(emptyList(), outbox.pendingForAccount("account-a"))
    }

    @Test
    fun `remote deck deletion cascades cards and advances checkpoint`() = withDatabase { source ->
        val database = source.current()
        val deckId = database.deckDao().insertNewDeck(deck("old").copy(cardQuantity = 1)).toInt()
        database.cardDao().insertNewCard(card(deckId))

        RoomSyncDeltaApplier(source, RoomSyncOutbox(source)).applyConflictFree("account-a", response(
            revision = 1L,
            deletedDecks = listOf("deck-a"),
            deletedCards = listOf("card-a"),
        ))

        assertEquals(emptyList(), database.deckDao().getAllDecks())
        assertEquals(emptyList(), database.cardDao().getAllCards())
        assertEquals(1L, RoomSyncOutbox(source).confirmedRevision("account-a"))
    }

    @Test
    fun `new parent is inserted before card and an existing card keeps its ID when moved`() = withDatabase { source ->
        val database = source.current()
        val oldDeckId = database.deckDao().insertNewDeck(deck("old").copy(cardQuantity = 1)).toInt()
        val cardId = database.cardDao().insertNewCard(card(oldDeckId)).toInt()

        RoomSyncDeltaApplier(source, RoomSyncOutbox(source)).applyConflictFree("account-a", response(
            revision = 1L,
            decks = listOf(
                syncDeck("old", cardQuantity = 0),
                syncDeck("new", syncId = "new-deck", cardQuantity = 1),
            ),
            cards = listOf(syncCard("moved", deckSyncId = "new-deck")),
        ))

        val newDeckId = database.deckDao().getDeckBySyncId("new-deck")?.id
        assertEquals(cardId, database.cardDao().getCardBySyncId("card-a")?.id)
        assertEquals(newDeckId, database.cardDao().getCardBySyncId("card-a")?.deckId)
        assertEquals(0, database.cardDao().getCardQuantityInDeck(oldDeckId))
    }

    @Test
    fun `move out and delete source in one delta preserves card ID and removes only remaining cards`() = withDatabase { source ->
        val database = source.current()
        val oldDeckId = database.deckDao().insertNewDeck(deck("source").copy(cardQuantity = 2)).toInt()
        val moved = card(oldDeckId).copy(id = 30)
        database.cardDao().insertNewCard(moved)
        database.cardDao().insertNewCard(card(oldDeckId).copy(syncId = "removed-card"))

        RoomSyncDeltaApplier(source, RoomSyncOutbox(source)).applyConflictFree("account-a", response(
            revision = 1L,
            decks = listOf(syncDeck("destination", syncId = "new-deck", cardQuantity = 1)),
            cards = listOf(syncCard("moved", deckSyncId = "new-deck")),
            deletedDecks = listOf("deck-a"),
            deletedCards = listOf("removed-card"),
        ))

        val destination = requireNotNull(database.deckDao().getDeckBySyncId("new-deck"))
        val received = requireNotNull(database.cardDao().getCardBySyncId("card-a"))
        assertEquals(moved.id, received.id)
        assertEquals(destination.id, received.deckId)
        assertEquals(listOf(received), database.cardDao().getAllCards())
        assertEquals(listOf(destination), database.deckDao().getAllDecks())
        assertEquals(1L, RoomSyncOutbox(source).confirmedRevision("account-a"))
    }

    @Test
    fun `card pointing at deleted parent is rejected without deleting local data or advancing checkpoint`() = withDatabase { source ->
        val database = source.current()
        val oldDeckId = database.deckDao().insertNewDeck(deck("source").copy(cardQuantity = 1)).toInt()
        val cardId = database.cardDao().insertNewCard(card(oldDeckId)).toInt()
        val beforeDecks = database.deckDao().getAllDecks()
        val beforeCards = database.cardDao().getAllCards()
        assertFails {
            RoomSyncDeltaApplier(source, RoomSyncOutbox(source)).applyConflictFree("account-a", response(
                revision = 1L,
                cards = listOf(syncCard("invalid")),
                deletedDecks = listOf("deck-a"),
            ))
        }
        assertEquals(beforeDecks, database.deckDao().getAllDecks())
        assertEquals(beforeCards, database.cardDao().getAllCards())
        assertEquals(cardId, database.cardDao().getCardBySyncId("card-a")?.id)
        assertEquals(0L, RoomSyncOutbox(source).confirmedRevision("account-a"))
    }

    @Test
    fun `incorrect server card count rolls back every local row and checkpoint`() = withDatabase { source ->
        val database = source.current()
        assertFails {
            RoomSyncDeltaApplier(source, RoomSyncOutbox(source)).applyConflictFree("account-a", response(
                revision = 1L,
                decks = listOf(syncDeck("new", syncId = "new-deck", cardQuantity = 2)),
                cards = listOf(syncCard("only one", deckSyncId = "new-deck")),
            ))
        }
        assertEquals(emptyList(), database.deckDao().getAllDecks())
        assertEquals(emptyList(), database.cardDao().getAllCards())
        assertEquals(0L, RoomSyncOutbox(source).confirmedRevision("account-a"))
    }

    @Test
    fun `missing parent rolls back earlier row writes acknowledgement and checkpoint`() = withDatabase { source ->
        val database = source.current()
        val outbox = RoomSyncOutbox(source)
        outbox.recordChange("account-a", 0L, SyncOperation.DeleteDeck("delete-a", "old-deck")) {}

        assertFails {
            RoomSyncDeltaApplier(source, outbox).applyConflictFree("account-a", response(
                revision = 1L,
                accepted = setOf("delete-a"),
                decks = listOf(syncDeck("valid", syncId = "valid-deck")),
                cards = listOf(syncCard("orphan", deckSyncId = "missing-deck")),
            ))
        }

        assertEquals(emptyList(), database.deckDao().getAllDecks())
        assertEquals(listOf("delete-a"), outbox.pendingForAccount("account-a").map { it.operation.operationId })
        assertEquals(0L, outbox.confirmedRevision("account-a"))
    }

    @Test
    fun `conflicts and stale delta are not applied as conflict free`() = withDatabase { source ->
        val outbox = RoomSyncOutbox(source)
        val applier = RoomSyncDeltaApplier(source, outbox)
        val operation = SyncOperation.DeleteDeck("delete-a", "deck-a")
        outbox.recordChange("account-a", 0L, operation) {}
        val conflict = SyncConflict("delete-a", "CONCURRENT_CHANGE", operation, emptyList())

        assertFails {
            applier.applyConflictFree("account-a", response(1L, conflicts = listOf(conflict)))
        }
        assertEquals(0L, outbox.confirmedRevision("account-a"))
        outbox.applyAccepted("account-a", emptyList(), 2L) {}
        assertFails {
            applier.applyConflictFree("account-a", response(3L, accepted = setOf("delete-a")))
        }
        assertEquals(2L, outbox.confirmedRevision("account-a"))
        assertEquals(1, outbox.pendingForAccount("account-a").size)
    }

    private fun withDatabase(block: suspend (ActiveLocalRoomDatabase) -> Unit) = runBlocking {
        val tempRoot = Path.of(System.getProperty("java.io.tmpdir")).toRealPath()
        val directory = Files.createTempDirectory(tempRoot, "klaf-delta-").toRealPath()
        require(directory.parent == tempRoot && directory.fileName.toString().startsWith("klaf-delta-"))
        val source = ActiveLocalRoomDatabase(
            ScopedKlafRoomDatabaseFactory(directory.toFile()),
            DesktopSelectedAccountStore(directory.toFile()),
        )
        try {
            source.selectAccount("account-a")
            block(source)
        } finally {
            source.close()
            directory.toFile().deleteRecursively()
        }
    }

    private fun response(
        revision: Long,
        accepted: Set<String> = emptySet(),
        decks: List<SyncDeck> = emptyList(),
        cards: List<SyncCard> = emptyList(),
        deletedDecks: List<String> = emptyList(),
        deletedCards: List<String> = emptyList(),
        conflicts: List<SyncConflict> = emptyList(),
    ): SyncResponse = SyncResponse(
        revision = revision,
        acceptedOperationIds = accepted,
        conflicts = conflicts,
        delta = SyncDelta(0L, revision, decks, cards, deletedDecks, deletedCards, emptyList()),
    )

    private fun deck(name: String): RoomDeck = RoomDeck(
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
        syncId = "deck-a",
    )

    private fun card(deckId: Int): RoomCard = RoomCard(
        deckId = deckId,
        nativeWord = "native",
        foreignWord = "old word",
        ipa = "[]",
        wordMeaningInsights = com.kuts.domain.entities.WordMeaningInsights.EMPTY,
        mnemonicJson = "{}",
        syncId = "card-a",
    )

    private fun syncDeck(
        name: String,
        syncId: String = "deck-a",
        cardQuantity: Int = 0,
        revision: Long = 1L,
    ): SyncDeck = SyncDeck(syncId, name, 1L, cardQuantity = cardQuantity, lastChangedServerRevision = revision)

    private fun syncCard(
        word: String,
        deckSyncId: String = "deck-a",
        revision: Long = 1L,
    ): SyncCard = SyncCard(
        syncId = "card-a",
        deckSyncId = deckSyncId,
        nativeWord = "native",
        foreignWord = word,
        wordMeaningInsightsJson = "{}",
        lastChangedServerRevision = revision,
    )
}
