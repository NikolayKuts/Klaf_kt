package com.kuts.klaf.room

import com.kuts.domain.entities.Card
import com.kuts.domain.entities.Deck
import com.kuts.domain.common.ICoroutineContextProvider
import com.kuts.domain.useCases.AddNewCardIntoDeckUseCase
import com.kuts.domain.useCases.TransferCardsToDeckUseCase
import com.kuts.klaf.room.databases.ActiveLocalRoomDatabase
import com.kuts.klaf.room.databases.DesktopSelectedAccountStore
import com.kuts.klaf.room.databases.ScopedKlafRoomDatabaseFactory
import com.kuts.klaf.room.repositoryImplementations.CardRepositoryRoom
import com.kuts.klaf.room.repositoryImplementations.DeckRepositoryRoom
import com.kuts.klaf.room.repositoryImplementations.RoomSyncOutbox
import com.kuts.klaf.room.repositoryImplementations.StorageTransactionRepositoryRoom
import com.kuts.klaf.room.repositoryImplementations.StorageSaveVersionRepositoryRoom
import com.kuts.klaf.server.contract.SyncOperation
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertFailsWith
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.EmptyCoroutineContext

private const val ACCOUNT = "alice@example.test"

private object OutboxTestContext : ICoroutineContextProvider {
    override val io = EmptyCoroutineContext
}

class AccountScopedTransactionOutboxTest {

    @Test
    fun `rejected creation in reviewed deck leaves real Room data version and outbox intact`() = withSource { source ->
        source.selectAccount(ACCOUNT)
        val decks = DeckRepositoryRoom(source)
        val cards = CardRepositoryRoom(source)
        val versions = StorageSaveVersionRepositoryRoom(source)
        val deckId = decks.insertDeck(Deck("Reviewed", 1L, reviewCount = 1))
        val beforeDeck = decks.getDeckById(deckId)
        val beforeVersion = versions.fetchVersion()

        assertFailsWith<IllegalStateException> {
            AddNewCardIntoDeckUseCase(
                decks, cards, versions, StorageTransactionRepositoryRoom(source), OutboxTestContext,
            )(Card(deckId, "native", "foreign", emptyList()))
        }

        assertEquals(beforeDeck, decks.getDeckById(deckId))
        assertEquals(emptyList(), cards.fetchCardsByDeckId(deckId))
        assertEquals(beforeVersion, versions.fetchVersion())
        assertEquals(emptyList(), RoomSyncOutbox(source).pendingForAccount(ACCOUNT))
    }

    @Test
    fun `stale move into reviewed deck leaves real Room data version and outbox intact`() = withSource { source ->
        source.selectAccount(ACCOUNT)
        val decks = DeckRepositoryRoom(source)
        val cards = CardRepositoryRoom(source)
        val versions = StorageSaveVersionRepositoryRoom(source)
        val sourceId = decks.insertDeck(Deck("Source", 1L, cardQuantity = 1))
        val targetId = decks.insertDeck(Deck("Target", 2L))
        val staleTarget = requireNotNull(decks.getDeckById(targetId))
        decks.insertDeck(staleTarget.copy(reviewCount = 1))
        cards.insertCard(Card(sourceId, "native", "foreign", emptyList()))
        val beforeSource = requireNotNull(decks.getDeckById(sourceId))
        val beforeTarget = decks.getDeckById(targetId)
        val beforeCards = cards.fetchCardsByDeckId(sourceId)
        val beforeVersion = versions.fetchVersion()

        assertFailsWith<IllegalStateException> {
            TransferCardsToDeckUseCase(
                cards, decks, versions, StorageTransactionRepositoryRoom(source), OutboxTestContext,
            )(beforeSource, staleTarget, *beforeCards.toTypedArray())
        }

        assertEquals(beforeSource, decks.getDeckById(sourceId))
        assertEquals(beforeTarget, decks.getDeckById(targetId))
        assertEquals(beforeCards, cards.fetchCardsByDeckId(sourceId))
        assertEquals(emptyList(), cards.fetchCardsByDeckId(targetId))
        assertEquals(beforeVersion, versions.fetchVersion())
        assertEquals(emptyList(), RoomSyncOutbox(source).pendingForAccount(ACCOUNT))
    }

    @Test
    fun `account transaction queues deck and card creation using final identities`() = withSource { source ->
        source.selectAccount(ACCOUNT)
        val decks = DeckRepositoryRoom(source)
        val cards = CardRepositoryRoom(source)
        StorageTransactionRepositoryRoom(source).performWithTransaction {
            val deckId = decks.insertDeck(Deck("Words", 1L))
            cards.insertCard(Card(deckId, "native", "foreign", emptyList()))
            decks.insertDeck(requireNotNull(decks.getDeckById(deckId)).copy(cardQuantity = 1))
        }

        val pending = RoomSyncOutbox(source).pendingForAccount(ACCOUNT)
        val addDeck = assertIs<SyncOperation.AddDeck>(pending[0].operation)
        val addCard = assertIs<SyncOperation.AddCard>(pending[1].operation)
        assertEquals(addDeck.deck.syncId, addCard.card.deckSyncId)
        assertEquals("foreign", addCard.card.foreignWord)
        assertEquals(listOf(0L, 0L), pending.map { it.baseRevision })
    }

    @Test
    fun `account transaction groups deck deletion with its child cards`() = withSource { source ->
        source.selectAccount(ACCOUNT)
        val decks = DeckRepositoryRoom(source)
        val cards = CardRepositoryRoom(source)
        val deckId = decks.insertDeck(Deck("Words", 1L))
        cards.insertCard(Card(deckId, "native", "foreign", emptyList()))
        StorageTransactionRepositoryRoom(source).performWithTransaction {
            decks.removeDeck(deckId)
            cards.removeCardsOfDeck(deckId)
        }

        val pending = RoomSyncOutbox(source).pendingForAccount(ACCOUNT)
        assertEquals(1, pending.size)
        assertIs<SyncOperation.DeleteDeck>(pending.single().operation)
    }

    @Test
    fun `guest transaction never creates an account outbox operation`() = withSource { source ->
        StorageTransactionRepositoryRoom(source).performWithTransaction {
            DeckRepositoryRoom(source).insertDeck(Deck("Guest", 1L))
        }

        assertEquals(emptyList(), source.current().pendingSyncOperationDao().allPending())
    }

    @Test
    fun `account transaction queues rename card edit move and deletion`() = withSource { source ->
        source.selectAccount(ACCOUNT)
        val decks = DeckRepositoryRoom(source)
        val cards = CardRepositoryRoom(source)
        val firstId = decks.insertDeck(Deck("First", 1L))
        val secondId = decks.insertDeck(Deck("Second", 2L))
        val editedId = cards.insertCard(Card(firstId, "one", "uno", emptyList()))
        val movedId = cards.insertCard(Card(firstId, "two", "dos", emptyList()))
        val deletedId = cards.insertCard(Card(firstId, "three", "tres", emptyList()))
        StorageTransactionRepositoryRoom(source).performWithTransaction {
            decks.insertDeck(requireNotNull(decks.getDeckById(firstId)).copy(name = "Renamed", cardQuantity = 1))
            decks.insertDeck(requireNotNull(decks.getDeckById(secondId)).copy(cardQuantity = 1))
            cards.insertCard(requireNotNull(source.current().cardDao().getCardById(editedId)).toDomainEntity()
                .copy(foreignWord = "ONE"))
            cards.insertCard(requireNotNull(source.current().cardDao().getCardById(movedId)).toDomainEntity()
                .copy(deckId = secondId))
            cards.deleteCard(deletedId)
        }

        val operations = RoomSyncOutbox(source).pendingForAccount(ACCOUNT).map { it.operation }
        assertEquals(4, operations.size)
        assertEquals(1, operations.filterIsInstance<SyncOperation.EditDeck>().size)
        assertEquals(1, operations.filterIsInstance<SyncOperation.EditCard>().size)
        assertEquals(1, operations.filterIsInstance<SyncOperation.MoveCard>().size)
        assertEquals(1, operations.filterIsInstance<SyncOperation.DeleteCard>().size)
    }

    @Test
    fun `failed account transaction rolls back data and outbox`() = withSource { source ->
        source.selectAccount(ACCOUNT)
        val decks = DeckRepositoryRoom(source)
        assertFailsWith<IllegalStateException> {
            StorageTransactionRepositoryRoom(source).performWithTransaction {
                decks.insertDeck(Deck("Must roll back", 1L))
                error("abort")
            }
        }

        assertEquals(emptyList(), decks.fetchAllDecks())
        assertEquals(emptyList(), RoomSyncOutbox(source).pendingForAccount(ACCOUNT))
    }

    @Test
    fun `switching accounts keeps outbox isolated`() = withSource { source ->
        source.selectAccount(ACCOUNT)
        StorageTransactionRepositoryRoom(source).performWithTransaction {
            DeckRepositoryRoom(source).insertDeck(Deck("Alice", 1L))
        }
        source.selectAccount("bob@example.test")
        StorageTransactionRepositoryRoom(source).performWithTransaction {
            DeckRepositoryRoom(source).insertDeck(Deck("Bob", 2L))
        }

        assertEquals("Bob", assertIs<SyncOperation.AddDeck>(
            RoomSyncOutbox(source).pendingForAccount("bob@example.test").single().operation,
        ).deck.name)
        source.selectAccount(ACCOUNT)
        assertEquals("Alice", assertIs<SyncOperation.AddDeck>(
            RoomSyncOutbox(source).pendingForAccount(ACCOUNT).single().operation,
        ).deck.name)
    }

    @Test
    fun `card move carries recalculated review durations of both decks`() = withSource { source ->
        source.selectAccount(ACCOUNT)
        val decks = DeckRepositoryRoom(source)
        val cards = CardRepositoryRoom(source)
        val sourceId = decks.insertDeck(Deck("Reviewed", 1L, cardQuantity = 1,
            reviewCount = 1, reviewPassDates = listOf(10L), scheduledReviewDates = listOf(20L),
            lastReviewPassDuration = 100L))
        val targetId = decks.insertDeck(Deck("Target", 2L))
        val cardId = cards.insertCard(Card(sourceId, "one", "uno", emptyList()))

        StorageTransactionRepositoryRoom(source).performWithTransaction {
            cards.insertCard(requireNotNull(source.current().cardDao().getCardById(cardId)).toDomainEntity()
                .copy(deckId = targetId))
            decks.insertDeck(requireNotNull(decks.getDeckById(sourceId)).copy(
                cardQuantity = 0, lastReviewPassDuration = 0L))
            decks.insertDeck(requireNotNull(decks.getDeckById(targetId)).copy(
                cardQuantity = 1, lastReviewPassDuration = 100L))
        }

        val operation = assertIs<SyncOperation.MoveCard>(
            RoomSyncOutbox(source).pendingForAccount(ACCOUNT).single().operation,
        )
        assertEquals(0L, operation.sourceLastReviewPassDuration)
        assertEquals(100L, operation.targetLastReviewPassDuration)
        assertEquals(100L, operation.movedCardReviewDuration)
    }

    private fun withSource(block: suspend (ActiveLocalRoomDatabase) -> Unit) = runBlocking {
        val tempRoot = Path.of(System.getProperty("java.io.tmpdir")).toRealPath()
        val directory = Files.createTempDirectory(tempRoot, "klaf-transaction-outbox-").toRealPath()
        require(directory.parent == tempRoot && directory.fileName.toString().startsWith("klaf-transaction-outbox-"))
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
