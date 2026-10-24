package com.kuts.klaf.room

import com.kuts.domain.entities.Card
import com.kuts.domain.entities.Deck
import com.kuts.domain.entities.IgnoredVocabularyWord
import com.kuts.domain.entities.VocabularySource
import com.kuts.domain.entities.VocabularySourceItem
import com.kuts.domain.entities.VocabularySourceItemCategory
import com.kuts.domain.entities.VocabularySourceItemStatus
import com.kuts.domain.repositories.IStorageTransactionRepository
import com.kuts.klaf.room.databases.ActiveLocalRoomDatabase
import com.kuts.klaf.room.databases.DesktopSelectedAccountStore
import com.kuts.klaf.room.databases.ScopedKlafRoomDatabaseFactory
import com.kuts.klaf.room.maintenance.TemporaryCleanupReceiptStore
import com.kuts.klaf.room.maintenance.TemporaryCleanupResult
import com.kuts.klaf.room.maintenance.TemporaryVocabularyCleanup
import com.kuts.klaf.room.repositoryImplementations.CardRepositoryRoom
import com.kuts.klaf.room.repositoryImplementations.DeckRepositoryRoom
import com.kuts.klaf.room.repositoryImplementations.IgnoredVocabularyWordRepositoryRoom
import com.kuts.klaf.room.repositoryImplementations.RoomSyncOutbox
import com.kuts.klaf.room.repositoryImplementations.StorageTransactionRepositoryRoom
import com.kuts.klaf.room.repositoryImplementations.VocabularySourceRepositoryRoom
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

private const val TEMPORARY_CLEANUP_ACCOUNT = "cleanup@example.test"

private class CleanupTestReceipts : TemporaryCleanupReceiptStore {
    val values = mutableMapOf<String, String>()
    var failWrites = false
    override fun read(scope: String): String? = values[scope]
    override fun write(scope: String, receipt: String) {
        check(!failWrites) { "Injected receipt failure" }
        values[scope] = receipt
    }
}

private class CleanupTestFixture {
    val root = Files.createTempDirectory("klaf-temporary-cleanup-").toRealPath()
    var source = open()
    val receipts = CleanupTestReceipts()
    val sources get() = VocabularySourceRepositoryRoom(source)
    val words get() = IgnoredVocabularyWordRepositoryRoom(source)
    val cards get() = CardRepositoryRoom(source)
    val decks get() = DeckRepositoryRoom(source)

    private fun open() = ActiveLocalRoomDatabase(
        ScopedKlafRoomDatabaseFactory(root.toFile()), DesktopSelectedAccountStore(root.toFile()),
    )

    suspend fun seed(title: String = "Preserve source"): Int {
        val id = sources.saveSource(VocabularySource(title, description = "Keep description",
            url = "https://example.test/source", rawText = "Keep raw text", cleanText = "Keep transcript",
            createdAt = 1, updatedAt = 2))
        sources.saveItems(listOf(item(id)))
        words.saveWords(listOf(IgnoredVocabularyWord("en", "skip", "meaning", 1)))
        return id
    }

    fun item(sourceId: Int) = VocabularySourceItem(sourceId, foreignWord = "cat", nativeWord = "кот",
        category = VocabularySourceItemCategory.NEW, createdAt = 1, updatedAt = 2)

    suspend fun selectAccount() {
        source.selectAccount(TEMPORARY_CLEANUP_ACCOUNT)
        RoomSyncOutbox(source).applyAccepted(TEMPORARY_CLEANUP_ACCOUNT, emptyList(), 7) {}
    }

    fun reopen() {
        source.close()
        source = open()
    }

    fun close() {
        source.close()
        check(root.parent == java.nio.file.Path.of(System.getProperty("java.io.tmpdir")).toRealPath())
        check(root.fileName.toString().startsWith("klaf-temporary-cleanup-"))
        root.toFile().deleteRecursively()
    }
}

private fun withCleanupFixture(block: suspend CleanupTestFixture.() -> Unit) = runBlocking {
    val fixture = CleanupTestFixture()
    try {
        fixture.block()
    } finally {
        fixture.close()
    }
}

class TemporaryVocabularyCleanupTest {
    @Test
    fun cleanupPreservesSourceCardsDecksAndGuestDataAndQueuesSyncDeletions() = withCleanupFixture {
        val guestId = seed("Guest must remain")
        selectAccount()
        val id = seed()
        val deckId = decks.insertDeck(Deck("Keep deck", creationDate = 1))
        cards.insertCard(Card(deckId, "собака", "dog", emptyList()))
        val sourceBefore = sources.fetchSourceById(id)
        val cardsBefore = cards.fetchAllCards()
        val decksBefore = decks.fetchAllDecks()
        val backups = mutableListOf<String>()

        val result = assertIs<TemporaryCleanupResult.Completed>(TemporaryVocabularyCleanup(source, receipts).run { backups += it })

        assertEquals(1, result.receipt.removedItems)
        assertEquals(1, result.receipt.removedIgnoredWords)
        assertTrue(result.receipt.syncRequired)
        assertTrue(sources.fetchItemsBySourceId(id).isEmpty())
        assertTrue(words.fetchWords().isEmpty())
        assertEquals(sourceBefore, sources.fetchSourceById(id))
        assertEquals(cardsBefore, cards.fetchAllCards())
        assertEquals(decksBefore, decks.fetchAllDecks())
        assertTrue(backups.single().contains("Keep transcript"))
        assertTrue(backups.single().contains("skip"))
        val pending = source.current().pendingSyncOperationDao().allPending()
        assertTrue(pending.any { it.operationJson.contains("REMOVE_IGNORED_VOCABULARY_WORD") })
        assertTrue(pending.any { it.operationJson.contains("UPSERT_VOCABULARY_SOURCE") })
        source.selectAccount(null)
        assertEquals("Guest must remain", sources.fetchSourceById(guestId)?.title)
        assertEquals(1, sources.fetchItemsBySourceId(guestId).size)
        assertEquals(1, words.fetchWords().size)
    }

    @Test
    fun relaunchDoesNotDeleteNewAnalysisOrNewIgnoredRules() = withCleanupFixture {
        val id = seed()
        TemporaryVocabularyCleanup(source, receipts).run {}
        sources.saveItems(listOf(item(id)))
        words.saveWords(listOf(IgnoredVocabularyWord("en", "new", "new meaning", 3)))
        reopen()

        val result = assertIs<TemporaryCleanupResult.Completed>(TemporaryVocabularyCleanup(source, receipts).run {
            error("Must not back up or clean a second time")
        })

        assertTrue(result.alreadyCompleted)
        assertEquals(1, sources.fetchItemsBySourceId(id).size)
        assertEquals("new", words.fetchWords().single().foreignWord)
    }

    @Test
    fun ambiguousSourceSelectionDoesNotDeleteAnything() = withCleanupFixture {
        val first = seed("First")
        val second = seed("Second")
        val before = sources.fetchSources()

        val result = assertIs<TemporaryCleanupResult.Skipped>(TemporaryVocabularyCleanup(source, receipts).run {
            error("Must not start cleanup when source is ambiguous")
        })

        assertEquals(2, result.sourceCount)
        assertEquals(before, sources.fetchSources())
        assertEquals(1, sources.fetchItemsBySourceId(first).size)
        assertEquals(1, sources.fetchItemsBySourceId(second).size)
        assertEquals(1, words.fetchWords().size)
        assertTrue(receipts.values.isEmpty())
    }

    @Test
    fun transactionFailureRollsBackDataOutboxAndCompletion() = withCleanupFixture {
        selectAccount()
        val id = seed()
        val actual = StorageTransactionRepositoryRoom(source)
        val failing = object : IStorageTransactionRepository {
            override suspend fun <R> performWithTransaction(block: suspend () -> R): R = actual.performWithTransaction {
                block()
                error("Injected failure before commit")
            }
        }

        assertFailsWith<IllegalStateException> { TemporaryVocabularyCleanup(source, receipts, failing).run {} }

        assertEquals(1, sources.fetchItemsBySourceId(id).size)
        assertEquals(1, words.fetchWords().size)
        assertTrue(source.current().pendingSyncOperationDao().allPending().isEmpty())
        assertTrue(receipts.values.isEmpty())
    }

    @Test
    fun failedBackupPreventsDeletion() = withCleanupFixture {
        val id = seed()

        assertFailsWith<IllegalStateException> { TemporaryVocabularyCleanup(source, receipts).run { error("Backup failed") } }

        assertEquals(1, sources.fetchItemsBySourceId(id).size)
        assertEquals(1, words.fetchWords().size)
        assertTrue(receipts.values.isEmpty())
    }

    @Test
    fun failedReceiptCanBeRetriedWithoutLosingQueuedDeletions() = withCleanupFixture {
        selectAccount()
        val id = seed()
        receipts.failWrites = true

        assertFailsWith<IllegalStateException> { TemporaryVocabularyCleanup(source, receipts).run {} }

        assertTrue(sources.fetchItemsBySourceId(id).isEmpty())
        assertTrue(words.fetchWords().isEmpty())
        val pending = source.current().pendingSyncOperationDao().allPending()
        assertTrue(pending.isNotEmpty())
        receipts.failWrites = false
        assertIs<TemporaryCleanupResult.Completed>(TemporaryVocabularyCleanup(source, receipts).run {})
        assertEquals(pending, source.current().pendingSyncOperationDao().allPending())
    }

    @Test
    fun waitingStartupRechecksCompletionBeforeTouchingNewData() = withCleanupFixture {
        val id = seed()
        val first = TemporaryVocabularyCleanup(source, receipts)
        val actual = StorageTransactionRepositoryRoom(source)
        val delayed = object : IStorageTransactionRepository {
            override suspend fun <R> performWithTransaction(block: suspend () -> R): R {
                first.run {}
                sources.saveItems(listOf(item(id)))
                words.saveWords(listOf(IgnoredVocabularyWord("en", "new", "new meaning", 3)))
                return actual.performWithTransaction(block)
            }
        }

        val result = assertIs<TemporaryCleanupResult.Completed>(TemporaryVocabularyCleanup(source, receipts, delayed).run {
            error("Waiting startup must not clean after another startup completed")
        })

        assertTrue(result.alreadyCompleted)
        assertEquals(1, sources.fetchItemsBySourceId(id).size)
        assertEquals("new", words.fetchWords().single().foreignWord)
    }

    @Test
    fun skippedAccountCleanupDoesNotNormalizeUnrelatedItemsOrQueueWrites() = withCleanupFixture {
        selectAccount()
        val first = seed("First")
        seed("Second")
        val item = sources.fetchItemsBySourceId(first).single()
        sources.saveItems(listOf(item.copy(status = VocabularySourceItemStatus.ADDED, createdCardId = 999)))
        val before = source.current().vocabularySourceItemDao().getItems()

        val result = assertIs<TemporaryCleanupResult.Skipped>(TemporaryVocabularyCleanup(source, receipts).run {
            error("Ambiguous account must not be backed up or changed")
        })

        assertEquals(2, result.sourceCount)
        assertEquals(before, source.current().vocabularySourceItemDao().getItems())
        assertTrue(source.current().pendingSyncOperationDao().allPending().isEmpty())
        assertTrue(receipts.values.isEmpty())
    }
}
