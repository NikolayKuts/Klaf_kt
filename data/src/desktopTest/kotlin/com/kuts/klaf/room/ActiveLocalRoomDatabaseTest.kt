package com.kuts.klaf.room

import com.kuts.domain.entities.Deck
import com.kuts.domain.entities.Card
import com.kuts.domain.entities.StorageSaveVersion
import com.kuts.domain.entities.VocabularySource
import com.kuts.domain.common.ICoroutineContextProvider
import com.kuts.domain.repositories.IStorageTransactionRepository
import com.kuts.domain.useCases.CreateInterimDeckUseCase
import com.kuts.klaf.room.databases.ActiveLocalRoomDatabase
import com.kuts.klaf.room.databases.ManualSyncInProgressException
import com.kuts.klaf.room.databases.DesktopSelectedAccountStore
import com.kuts.klaf.room.databases.ScopedKlafRoomDatabaseFactory
import com.kuts.klaf.room.databases.SelectedAccountStore
import com.kuts.klaf.room.repositoryImplementations.DeckRepositoryRoom
import com.kuts.klaf.room.repositoryImplementations.CardRepositoryRoom
import com.kuts.klaf.room.repositoryImplementations.GuestAccountDataTransfer
import com.kuts.klaf.room.repositoryImplementations.StorageSaveVersionRepositoryRoom
import com.kuts.klaf.room.repositoryImplementations.StorageTransactionRepositoryRoom
import com.kuts.klaf.room.repositoryImplementations.VocabularySourceRepositoryRoom
import com.kuts.klaf.server.contract.SyncOperation
import com.kuts.klaf.server.contract.accountInterimDeckSyncId
import java.nio.file.Files
import java.nio.file.Path
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import kotlinx.serialization.json.Json

class ActiveLocalRoomDatabaseTest {

    @Test
    fun `guest interim transfers with account identity and keeps its cards`() = withDirectory { directory ->
        val source = openSource(directory)
        val decks = DeckRepositoryRoom(source)
        val cards = CardRepositoryRoom(source)
        try {
            decks.insertDeck(Deck(id = Deck.INTERIM_DECK_ID, name = Deck.INTERIM_DECK_NAME, creationDate = 1L))
            cards.insertCard(Card(Deck.INTERIM_DECK_ID, "native", "guest word", emptyList()))
            val guestSyncId = requireNotNull(decks.getDeckById(Deck.INTERIM_DECK_ID)).syncId

            GuestAccountDataTransfer(source).transfer("alice@example.com")

            source.selectAccount("alice@example.com")
            val accountInterim = requireNotNull(decks.getDeckById(Deck.INTERIM_DECK_ID))
            assertEquals(accountInterimDeckSyncId("alice@example.com"), accountInterim.syncId)
            assertEquals(Deck.INTERIM_DECK_ID, cards.fetchAllCards().single().deckId)
            val pending = source.current().pendingSyncOperationDao().allPending()
            val deckOperation = Json.decodeFromString<SyncOperation>(pending.first().operationJson)
            assertEquals(accountInterim.syncId, (deckOperation as SyncOperation.AddDeck).deck.syncId)
            assertEquals(2, pending.size)
            assertFalse(guestSyncId == accountInterim.syncId)
        } finally {
            source.close()
        }
    }

    @Test
    fun `guest transfer preserves reviewed deck and card and queues one initial upload`() = withDirectory { directory ->
        val source = openSource(directory)
        val decks = DeckRepositoryRoom(source)
        val cards = CardRepositoryRoom(source)
        try {
            val deckId = decks.insertDeck(Deck(
                name = "reviewed guest",
                creationDate = 1L,
                reviewPassDates = listOf(100L),
                scheduledReviewDates = listOf(200L),
                scheduledDateInterval = 300L,
                reviewCount = 1,
                cardQuantity = 1,
            ))
            cards.insertCard(Card(deckId, "native", "foreign", emptyList()))
            val guestDeck = requireNotNull(decks.getDeckById(deckId))
            val guestCard = cards.fetchAllCards().single()

            GuestAccountDataTransfer(source).transfer("alice@example.com")
            assertEquals(emptyList(), decks.fetchAllDecks())
            assertEquals(emptyList(), cards.fetchAllCards())

            source.selectAccount("alice@example.com")
            val accountDeck = decks.fetchAllDecks().single()
            val accountCard = cards.fetchAllCards().single()
            assertEquals(guestDeck, accountDeck)
            assertEquals(guestCard, accountCard)
            val pending = source.current().pendingSyncOperationDao().pendingForAccount("alice@example.com")
            assertEquals(2, pending.size)
            assertEquals(listOf(0L, 0L), pending.map { it.baseRevision })

            source.selectAccount(null)
            GuestAccountDataTransfer(source).transfer("alice@example.com")
            source.selectAccount("alice@example.com")
            assertEquals(1, decks.fetchAllDecks().size)
            assertEquals(1, cards.fetchAllCards().size)
            assertEquals(2, source.current().pendingSyncOperationDao().pendingForAccount("alice@example.com").size)
        } finally {
            source.close()
        }
    }

    @Test
    fun `guest transfer rejects a nonempty account without deleting guest cards`() = withDirectory { directory ->
        val source = openSource(directory)
        val decks = DeckRepositoryRoom(source)
        val cards = CardRepositoryRoom(source)
        try {
            val guestDeckId = decks.insertDeck(Deck(name = "guest", creationDate = 1L))
            cards.insertCard(Card(guestDeckId, "native", "guest card", emptyList()))
            source.selectAccount("alice@example.com")
            decks.insertDeck(Deck(name = "existing account", creationDate = 2L))
            source.selectAccount(null)

            assertFails { GuestAccountDataTransfer(source).transfer("alice@example.com") }
            assertEquals(listOf("guest"), decks.fetchAllDecks().map(Deck::name))
            assertEquals(listOf("guest card"), cards.fetchAllCards().map(Card::foreignWord))
            source.selectAccount("alice@example.com")
            assertEquals(listOf("existing account"), decks.fetchAllDecks().map(Deck::name))
            assertEquals(emptyList(), cards.fetchAllCards())
        } finally {
            source.close()
        }
    }

    @Test
    fun `empty guest cannot adopt unrelated preexisting account data`() = withDirectory { directory ->
        val source = openSource(directory)
        val decks = DeckRepositoryRoom(source)
        try {
            source.selectAccount("alice@example.com")
            decks.insertDeck(Deck(name = "old local account", creationDate = 1L))
            source.selectAccount(null)

            assertFails { GuestAccountDataTransfer(source).transfer("alice@example.com") }
            assertEquals(emptyList(), decks.fetchAllDecks())
            source.selectAccount("alice@example.com")
            assertEquals(listOf("old local account"), decks.fetchAllDecks().map(Deck::name))
        } finally {
            source.close()
        }
    }

    @Test
    fun `interrupted transfer retries an exact account copy before deleting guest rows`() = withDirectory { directory ->
        val source = openSource(directory)
        val decks = DeckRepositoryRoom(source)
        val cards = CardRepositoryRoom(source)
        try {
            val deckId = decks.insertDeck(Deck(name = "guest", creationDate = 1L))
            cards.insertCard(Card(deckId, "native", "foreign", emptyList()))
            assertFails {
                GuestAccountDataTransfer(source, afterAccountCopy = { error("interrupted") })
                    .transfer("alice@example.com")
            }
            assertEquals(1, decks.fetchAllDecks().size)
            assertEquals(1, cards.fetchAllCards().size)

            GuestAccountDataTransfer(source).transfer("alice@example.com")
            assertEquals(emptyList(), decks.fetchAllDecks())
            source.selectAccount("alice@example.com")
            assertEquals(1, decks.fetchAllDecks().size)
            assertEquals(1, cards.fetchAllCards().size)
            assertEquals(2, source.current().pendingSyncOperationDao().allPending().size)
        } finally {
            source.close()
        }
    }

    @Test
    fun `interim deck check and creation use the account selected at transaction start`() = withDirectory { directory ->
        val source = openSource(directory)
        val decks = DeckRepositoryRoom(databaseSource = source)
        val transactions = StorageTransactionRepositoryRoom(databaseSource = source)
        val switchingTransactions = object : IStorageTransactionRepository {
            override suspend fun <R> performWithTransaction(block: suspend () -> R): R {
                source.selectAccount("alice@example.com")
                return transactions.performWithTransaction(block)
            }
        }
        try {
            decks.insertDeck(Deck(id = Deck.INTERIM_DECK_ID, name = "guest interim", creationDate = 1L))
            val versions = StorageSaveVersionRepositoryRoom(source)
            val createInterimDeck = CreateInterimDeckUseCase(
                deckRepository = decks,
                localStorageSaveVersionRepository = versions,
                localStorageTransactionRepository = switchingTransactions,
                coroutineContextProvider = object : ICoroutineContextProvider {
                    override val io = EmptyCoroutineContext
                },
            )
            createInterimDeck()

            assertEquals("alice@example.com", source.selection.value.accountEmail)
            assertEquals(Deck.INTERIM_DECK_NAME, decks.getDeckById(Deck.INTERIM_DECK_ID)?.name)
            assertEquals(accountInterimDeckSyncId("alice@example.com"),
                decks.getDeckById(Deck.INTERIM_DECK_ID)?.syncId)
            assertEquals(1, source.current().pendingSyncOperationDao().allPending().size)
            versions.insertVersion(StorageSaveVersion(42L))
            createInterimDeck()
            assertEquals(42L, versions.fetchVersion()?.version)
            assertEquals(1, decks.fetchAllDecks().count { it.id == Deck.INTERIM_DECK_ID })
            source.selectAccount(null)
            assertEquals("guest interim", decks.getDeckById(Deck.INTERIM_DECK_ID)?.name)
        } finally {
            source.close()
        }
    }

    @Test
    fun `card flow stops observing the previous account`() = withDirectory { directory ->
        val source = openSource(directory)
        val decks = DeckRepositoryRoom(databaseSource = source)
        val cards = CardRepositoryRoom(databaseSource = source)
        val words = Channel<List<String>>(Channel.UNLIMITED)
        val collector = launch {
            cards.fetchObservableCardsByDeckId(1).map { rows -> rows.map(Card::foreignWord) }
                .distinctUntilChanged()
                .collect { words.send(it) }
        }
        try {
            assertEquals(emptyList(), withTimeout(5_000) { words.receive() })
            val guestDeck = decks.insertDeck(Deck(name = "guest", creationDate = 1L))
            cards.insertCard(Card(guestDeck, "native", "guest card", emptyList()))
            assertEquals(listOf("guest card"), withTimeout(5_000) { words.receive() })

            source.selectAccount("alice@example.com")
            assertEquals(emptyList(), withTimeout(5_000) { words.receive() })
            val accountDeck = decks.insertDeck(Deck(name = "account", creationDate = 2L))
            cards.insertCard(Card(accountDeck, "native", "account card", emptyList()))
            assertEquals(listOf("account card"), withTimeout(5_000) { words.receive() })
        } finally {
            collector.cancelAndJoin()
            words.close()
            source.close()
        }
    }

    @Test
    fun `failed persistence leaves the previous database selected`() = withDirectory { directory ->
        val source = ActiveLocalRoomDatabase(
            factory = ScopedKlafRoomDatabaseFactory(directory.toFile()),
            accountStore = object : SelectedAccountStore {
                override fun read(): String? = null
                override fun write(email: String?) = error("write failed")
            },
        )
        try {
            val guestDatabase = source.current()
            assertFails { source.selectAccount("alice@example.com") }
            assertEquals(null, source.selection.value.accountEmail)
            assertEquals(guestDatabase, source.current())
        } finally {
            source.close()
        }
    }

    @Test
    fun `corrupt account database is rejected before selection is persisted`() = withDirectory { directory ->
        val factory = ScopedKlafRoomDatabaseFactory(directory.toFile())
        val accountDatabase = factory.openAccount("alice@example.com")
        try {
            accountDatabase.deckDao().getAllDecks()
        } finally {
            accountDatabase.close()
        }
        val accountFile = Files.list(directory).use { files ->
            files.filter { it.fileName.toString().startsWith("klaf_account_") && it.fileName.toString().endsWith(".db") }
                .findFirst().orElseThrow()
        }
        Files.writeString(accountFile, "not a sqlite database")

        val source = openSource(directory)
        try {
            assertFails { source.selectAccount("alice@example.com") }
            assertEquals(null, source.selection.value.accountEmail)
            assertEquals(null, DesktopSelectedAccountStore(directory.toFile()).read())
        } finally {
            source.close()
        }
    }

    @Test
    fun `cards vocabulary and local metadata follow the selected database`() = withDirectory { directory ->
        val source = openSource(directory)
        val decks = DeckRepositoryRoom(databaseSource = source)
        val cards = CardRepositoryRoom(databaseSource = source)
        val vocabulary = VocabularySourceRepositoryRoom(databaseSource = source)
        val versions = StorageSaveVersionRepositoryRoom(databaseSource = source)
        try {
            val guestDeckId = decks.insertDeck(Deck(name = "guest", creationDate = 1L))
            cards.insertCard(Card(guestDeckId, "native", "guest card", emptyList()))
            vocabulary.saveSource(VocabularySource(title = "guest source", createdAt = 1L, updatedAt = 1L))
            versions.insertVersion(StorageSaveVersion(7L))

            source.selectAccount("alice@example.com")
            assertEquals(emptyList(), cards.fetchAllCards())
            assertEquals(emptyList(), vocabulary.fetchSources())
            assertEquals(null, versions.fetchVersion())

            val accountDeckId = decks.insertDeck(Deck(name = "account", creationDate = 2L))
            cards.insertCard(Card(accountDeckId, "native", "account card", emptyList()))
            vocabulary.saveSource(VocabularySource(title = "account source", createdAt = 2L, updatedAt = 2L))
            versions.insertVersion(StorageSaveVersion(3L))

            source.selectAccount(null)
            assertEquals(listOf("guest card"), cards.fetchAllCards().map(Card::foreignWord))
            assertEquals(listOf("guest source"), vocabulary.fetchSources().map(VocabularySource::title))
            assertEquals(7L, versions.fetchVersion()?.version)
            source.selectAccount("alice@example.com")
            assertEquals(listOf("account card"), cards.fetchAllCards().map(Card::foreignWord))
            assertEquals(listOf("account source"), vocabulary.fetchSources().map(VocabularySource::title))
            assertEquals(3L, versions.fetchVersion()?.version)
        } finally {
            source.close()
        }
    }

    @Test
    fun `repository flow switches between guest and accounts and selection survives restart`() = withDirectory { directory ->
        val source = openSource(directory)
        val decks = DeckRepositoryRoom(databaseSource = source)
        val names = Channel<List<String>>(Channel.UNLIMITED)
        val collector = launch {
            decks.fetchDeckSource().map { rows -> rows.map(Deck::name) }
                .distinctUntilChanged()
                .collect { names.send(it) }
        }
        try {
            assertEquals(emptyList(), withTimeout(5_000) { names.receive() })
            decks.insertDeck(Deck(name = "guest", creationDate = 1L))
            assertEquals(listOf("guest"), withTimeout(5_000) { names.receive() })

            source.selectAccount(" Alice@Example.com ")
            assertEquals(emptyList(), withTimeout(5_000) { names.receive() })
            decks.insertDeck(Deck(name = "Alice", creationDate = 2L))
            assertEquals(listOf("Alice"), withTimeout(5_000) { names.receive() })

            source.selectAccount("bob@example.com")
            assertEquals(emptyList(), withTimeout(5_000) { names.receive() })
            decks.insertDeck(Deck(name = "Bob", creationDate = 3L))
            assertEquals(listOf("Bob"), withTimeout(5_000) { names.receive() })

            source.selectAccount(null)
            assertEquals(listOf("guest"), withTimeout(5_000) { names.receive() })
            source.selectAccount("alice@example.COM")
            assertEquals(listOf("Alice"), withTimeout(5_000) { names.receive() })
        } finally {
            collector.cancelAndJoin()
            names.close()
            source.close()
        }

        val reopened = openSource(directory)
        try {
            assertEquals("alice@example.com", reopened.selection.value.accountEmail)
            assertEquals(listOf("Alice"), DeckRepositoryRoom(databaseSource = reopened).fetchAllDecks().map(Deck::name))
        } finally {
            reopened.close()
        }
    }

    @Test
    fun `account switch is rejected while manual synchronization is active`() = withDirectory { directory ->
        val source = openSource(directory)
        try {
            source.selectAccount("alice@example.com")
            source.beginManualSyncAttempt("alice@example.com")
            assertFailsWith<ManualSyncInProgressException> { source.selectAccount(null) }
            assertEquals("alice@example.com", source.selection.value.accountEmail)
            source.endManualSyncAttempt("alice@example.com")
            source.selectAccount(null)
            assertEquals(null, source.selection.value.accountEmail)
        } finally {
            source.close()
        }
    }

    @Test
    fun `account switch waits for a local transaction to finish`() = withDirectory { directory ->
        val source = openSource(directory)
        val decks = DeckRepositoryRoom(databaseSource = source)
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        try {
            val write = async {
                source.transaction {
                    entered.complete(Unit)
                    release.await()
                    decks.insertDeck(Deck(name = "guest write", creationDate = 1L))
                }
            }
            entered.await()
            val switch = async { source.selectAccount("alice@example.com") }
            yield()
            assertFalse(switch.isCompleted)
            release.complete(Unit)
            write.await()
            switch.await()

            assertEquals(emptyList(), decks.fetchAllDecks())
            source.selectAccount(null)
            assertEquals(listOf("guest write"), decks.fetchAllDecks().map(Deck::name))
        } finally {
            source.close()
        }
    }

    private fun openSource(directory: Path): ActiveLocalRoomDatabase = ActiveLocalRoomDatabase(
        factory = ScopedKlafRoomDatabaseFactory(directory.toFile()),
        accountStore = DesktopSelectedAccountStore(directory.toFile()),
    )

    private fun withDirectory(block: suspend kotlinx.coroutines.CoroutineScope.(Path) -> Unit) = runBlocking {
        val tempRoot = Path.of(System.getProperty("java.io.tmpdir")).toRealPath()
        val directory = Files.createTempDirectory(tempRoot, "klaf-active-db-").toRealPath()
        require(directory.parent == tempRoot && directory.fileName.toString().startsWith("klaf-active-db-"))
        try {
            block(directory)
        } finally {
            directory.toFile().deleteRecursively()
        }
    }
}
