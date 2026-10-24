package com.kuts.klaf.room

import com.kuts.domain.common.ICoroutineContextProvider
import com.kuts.domain.entities.IgnoredVocabularyWord
import com.kuts.domain.entities.VocabularySource
import com.kuts.domain.entities.VocabularySourceItem
import com.kuts.domain.entities.VocabularySourceItemCategory
import com.kuts.domain.entities.VocabularySourceItemStatus
import com.kuts.domain.repositories.IIgnoredVocabularyWordRepository
import com.kuts.domain.useCases.CreateVocabularySourceUseCase
import com.kuts.domain.useCases.RemoveVocabularySourceUseCase
import com.kuts.domain.useCases.ReplaceVocabularySourceDraftItemsUseCase
import com.kuts.domain.useCases.SaveVocabularySourceChangesUseCase
import com.kuts.domain.useCases.ResetVocabularySourceAnalysisUseCase
import com.kuts.domain.useCases.AddVocabularySourceItemsToDeckUseCase
import com.kuts.domain.entities.Deck
import com.kuts.domain.vocabularySource.reviewKey
import com.kuts.klaf.room.repositoryImplementations.CardRepositoryRoom
import com.kuts.klaf.room.repositoryImplementations.DeckRepositoryRoom
import com.kuts.klaf.networking.klafServer.SyncEventChannelState
import com.kuts.klaf.networking.klafServer.SyncEventFeedStatus
import com.kuts.klaf.room.databases.ActiveLocalRoomDatabase
import com.kuts.klaf.room.databases.DesktopSelectedAccountStore
import com.kuts.klaf.room.databases.ManualSyncInProgressException
import com.kuts.klaf.room.databases.ScopedKlafRoomDatabaseFactory
import com.kuts.klaf.room.repositoryImplementations.GuestAccountDataTransfer
import com.kuts.klaf.room.repositoryImplementations.IgnoredVocabularyWordRepositoryRoom
import com.kuts.klaf.room.repositoryImplementations.RoomSyncOutbox
import com.kuts.klaf.room.repositoryImplementations.RoomSyncDeltaApplier
import com.kuts.klaf.server.contract.SyncBootstrapResponse
import com.kuts.klaf.room.repositoryImplementations.RoomSyncStatusObserver
import com.kuts.klaf.room.repositoryImplementations.StorageSaveVersionRepositoryRoom
import com.kuts.klaf.room.repositoryImplementations.StorageTransactionRepositoryRoom
import com.kuts.klaf.room.repositoryImplementations.SyncIndicatorState
import com.kuts.klaf.room.repositoryImplementations.VocabularySourceRepositoryRoom
import java.nio.file.Files
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

private const val SOURCE_SYNC_ACCOUNT = "alice@example.test"
private const val SOURCE_SYNC_OTHER_ACCOUNT = "bob@example.test"

private class VocabularySyncFixture {

    val root = Files.createTempDirectory("klaf-source-sync-outbox-").toRealPath()
    var databaseSource = open()
    val sources get() = VocabularySourceRepositoryRoom(databaseSource)
    val ignored get() = IgnoredVocabularyWordRepositoryRoom(databaseSource)
    val version get() = StorageSaveVersionRepositoryRoom(databaseSource)
    val transactions get() = StorageTransactionRepositoryRoom(databaseSource)
    val context = object : ICoroutineContextProvider { override val io = EmptyCoroutineContext }

    private fun open() = ActiveLocalRoomDatabase(
        ScopedKlafRoomDatabaseFactory(root.toFile()), DesktopSelectedAccountStore(root.toFile()),
    )

    suspend fun selectConfirmedAccount() {
        databaseSource.selectAccount(SOURCE_SYNC_ACCOUNT)
        RoomSyncOutbox(databaseSource).applyAccepted(SOURCE_SYNC_ACCOUNT, emptyList(), 7L) {}
    }

    suspend fun create(title: String = "Saved transcript"): Int = CreateVocabularySourceUseCase(
        sources, version, transactions, context,
    )(VocabularySource(title, rawText = "original transcript", cleanText = "clean transcript",
        createdAt = 1L, updatedAt = 2L))

    suspend fun acknowledgeLocalOperations() {
        val pending = databaseSource.current().pendingSyncOperationDao().allPending()
        RoomSyncOutbox(databaseSource).applyAccepted(
            SOURCE_SYNC_ACCOUNT, pending.map { it.operationId }, 7L,
        ) {}
    }

    suspend fun pending() = databaseSource.current().pendingSyncOperationDao().allPending()

    fun item(sourceId: Int, word: String = "newword") = VocabularySourceItem(
        sourceId = sourceId, language = "en", foreignWord = word, nativeWord = "translation",
        sourceExample = "sentence with $word", category = VocabularySourceItemCategory.NEW,
        createdAt = 1L, updatedAt = 2L,
    )

    fun reopen() {
        databaseSource.close()
        databaseSource = open()
    }

    fun close() {
        databaseSource.close()
        check(root.parent == java.nio.file.Path.of(System.getProperty("java.io.tmpdir")).toRealPath())
        check(root.fileName.toString().startsWith("klaf-source-sync-outbox-"))
        root.toFile().deleteRecursively()
    }
}

private fun withVocabularySyncFixture(block: suspend VocabularySyncFixture.() -> Unit) = runBlocking {
    val fixture = VocabularySyncFixture()
    try {
        fixture.block()
    } finally {
        fixture.close()
    }
}

class VocabularySourceSyncOutboxTest {

    @Test
    fun `saved ignore survives reopen and restore updates other sources and queues removal`() = withVocabularySyncFixture {
        selectConfirmedAccount()
        val first = create("First")
        val second = create("Second")
        sources.saveItems(listOf(item(second)))
        val save = SaveVocabularySourceChangesUseCase(sources, ignored, version, transactions, context)
        save(requireNotNull(sources.fetchSourceById(first)), listOf(item(first).copy(status = VocabularySourceItemStatus.IGNORED)),
            true, mapOf(item(first).reviewKey() to true))
        acknowledgeLocalOperations()
        reopen()
        val saved = sources.fetchItemsBySourceId(first).single()
        assertEquals(VocabularySourceItemStatus.IGNORED, saved.status)
        assertTrue(sources.observeAllItems().first().all { it.status == VocabularySourceItemStatus.IGNORED })

        SaveVocabularySourceChangesUseCase(sources, ignored, version, transactions, context)(
            requireNotNull(sources.fetchSourceById(first)),
            listOf(saved.copy(status = VocabularySourceItemStatus.PENDING)),
            true,
            mapOf(saved.reviewKey() to false),
        )
        assertTrue(ignored.fetchWords().isEmpty())
        assertTrue(sources.observeAllItems().first().all { it.status == VocabularySourceItemStatus.PENDING })
        assertTrue(pending().any { it.operationJson.contains("REMOVE_IGNORED_VOCABULARY_WORD") })
    }

    @Test
    fun `reset clears only current analysis and preserves global ignored rules sources and cards`() = withVocabularySyncFixture {
        selectConfirmedAccount()
        val first = create("First")
        val second = create("Second")
        sources.saveItems(listOf(item(first), item(second, "other")))
        val cards = CardRepositoryRoom(databaseSource)
        val decks = DeckRepositoryRoom(databaseSource)
        AddVocabularySourceItemsToDeckUseCase(cards, decks, sources, version, transactions, context, ignored)(
            "Keep this deck", sources.fetchItemsBySourceId(first),
        )
        ignored.saveWords(listOf(IgnoredVocabularyWord("en", "skip", "meaning", 1)))
        val sourcesBefore = sources.fetchSources()
        val cardsBefore = cards.fetchAllCards()
        val decksBefore = decks.fetchAllDecks()
        val ignoredBefore = ignored.fetchWords()
        acknowledgeLocalOperations()

        ResetVocabularySourceAnalysisUseCase(sources, version, transactions, context)(first)

        assertEquals(sourcesBefore, sources.fetchSources())
        assertEquals(cardsBefore, cards.fetchAllCards())
        assertEquals(decksBefore, decks.fetchAllDecks())
        assertTrue(sources.fetchItemsBySourceId(first).isEmpty())
        assertEquals(1, sources.fetchItemsBySourceId(second).size)
        assertEquals(ignoredBefore, ignored.fetchWords())
        assertTrue(pending().none { it.operationJson.contains("REMOVE_IGNORED_VOCABULARY_WORD") })
        assertTrue(pending().any { it.operationJson.contains("UPSERT_VOCABULARY_SOURCE") })
    }

    @Test
    fun `adding stale or repeated selections does not create duplicate cards or empty decks`() = withVocabularySyncFixture {
        val id = create()
        sources.saveItems(listOf(item(id)))
        val staleItem = sources.fetchItemsBySourceId(id).single()
        val cards = CardRepositoryRoom(databaseSource)
        val decks = DeckRepositoryRoom(databaseSource)
        val add = AddVocabularySourceItemsToDeckUseCase(cards, decks, sources, version, transactions, context, ignored)
        assertEquals(1, add("Words", listOf(staleItem, staleItem)))
        val decksBefore = decks.fetchAllDecks()
        assertEquals(0, add("Unneeded", listOf(staleItem)))
        assertEquals(decksBefore, decks.fetchAllDecks())
        assertEquals(1, cards.fetchAllCards().size)
    }

    @Test
    fun `empty reanalysis removes an orphan added item after its card was deleted in guest mode`() = withVocabularySyncFixture {
        val id = create()
        sources.saveItems(listOf(item(id)))
        val cards = CardRepositoryRoom(databaseSource)
        AddVocabularySourceItemsToDeckUseCase(cards, DeckRepositoryRoom(databaseSource), sources,
            version, transactions, context, ignored)("Words", sources.fetchItemsBySourceId(id))
        cards.deleteCard(cards.fetchAllCards().single().id)
        assertEquals(VocabularySourceItemStatus.PENDING, sources.observeItemsBySourceId(id).first().single().status)

        SaveVocabularySourceChangesUseCase(sources, ignored, version, transactions, context)(
            requireNotNull(sources.fetchSourceById(id)), emptyList(), true,
        )
        assertTrue(sources.fetchItemsBySourceId(id).isEmpty())
    }

    @Test
    fun `saving an inherited ignored snapshot cannot resurrect a globally restored pair`() = withVocabularySyncFixture {
        val id = create()
        val source = requireNotNull(sources.fetchSourceById(id))
        val ignoredItem = item(id).copy(status = VocabularySourceItemStatus.IGNORED)
        val save = SaveVocabularySourceChangesUseCase(sources, ignored, version, transactions, context)
        save(source, listOf(ignoredItem), true, mapOf(ignoredItem.reviewKey() to true))
        val capturedItems = sources.fetchItemsBySourceId(id)
        ignored.removeWords(setOf(ignoredItem.reviewKey()))

        save(source.copy(description = "Unrelated edit"), capturedItems, true)

        assertTrue(ignored.fetchWords().isEmpty())
        assertEquals(VocabularySourceItemStatus.PENDING, sources.observeItemsBySourceId(id).first().single().status)
    }

    @Test
    fun `initial snapshot cannot overwrite a source-only account`() = withVocabularySyncFixture {
        selectConfirmedAccount()
        val id = create()
        acknowledgeLocalOperations()
        val before = sources.fetchSourceById(id)
        val outbox = RoomSyncOutbox(databaseSource)

        assertFailsWith<IllegalStateException> {
            RoomSyncDeltaApplier(databaseSource, outbox).applyInitialSnapshot(
                SOURCE_SYNC_ACCOUNT, SyncBootstrapResponse(7, emptyList(), emptyList()),
            )
        }
        assertEquals(before, sources.fetchSourceById(id))
    }

    @Test
    fun `creating a source queues upload and turns a confirmed green account yellow`() = withVocabularySyncFixture {
        selectConfirmedAccount()
        val observer = RoomSyncStatusObserver(databaseSource, MutableStateFlow(SyncEventFeedStatus(
            accountEmail = SOURCE_SYNC_ACCOUNT, channel = SyncEventChannelState.CONNECTED, serverRevision = 7L,
        )))
        assertEquals(SyncIndicatorState.GREEN, observer.status.first().indicator)
        val id = create()

        assertNotNull(sources.fetchSourceById(id))
        val status = observer.status.first()
        assertEquals(SyncIndicatorState.YELLOW, status.indicator)
        assertEquals(1, status.pendingOperationCount)
        assertEquals(7L, status.confirmedRevision)
        val operation = pending().single()
        assertEquals(SOURCE_SYNC_ACCOUNT, operation.accountId)
        assertEquals(7L, operation.baseRevision)
        assertTrue(operation.operationId.isNotBlank())
        assertTrue(operation.operationJson.contains("Saved transcript"))
        assertTrue(operation.operationJson.contains("original transcript"))
    }

    @Test
    fun `source metadata and analyzed words produce one complete aggregate change`() = withVocabularySyncFixture {
        selectConfirmedAccount()
        val id = create()
        acknowledgeLocalOperations()
        val existing = requireNotNull(sources.fetchSourceById(id))
        SaveVocabularySourceChangesUseCase(sources, ignored, version, transactions, context)(
            existing.copy(title = "Reanalyzed transcript", cleanText = "new clean text", updatedAt = 3L),
            listOf(item(id, "firstword"), item(id, "secondword")), true,
        )

        assertEquals(2, sources.fetchItemsBySourceId(id).size)
        val queued = pending().single()
        listOf("Reanalyzed transcript", "new clean text", "firstword", "secondword").forEach {
            assertTrue(queued.operationJson.contains(it), "Whole-source payload must include $it")
        }
        assertEquals(7L, queued.baseRevision)
        assertEquals(2L, version.fetchVersion()?.version)
    }

    @Test
    fun `word-only reanalysis queues the source even when its text is unchanged`() = withVocabularySyncFixture {
        selectConfirmedAccount()
        val id = create()
        acknowledgeLocalOperations()
        ReplaceVocabularySourceDraftItemsUseCase(sources, version, transactions, context)(id, listOf(item(id)))

        assertEquals(1, sources.fetchItemsBySourceId(id).size)
        val queued = pending().single()
        assertTrue(queued.operationJson.contains("Saved transcript"))
        assertTrue(queued.operationJson.contains("newword"))
    }

    @Test
    fun `reanalysis replaces pending words but retains added word status`() = withVocabularySyncFixture {
        selectConfirmedAccount()
        val id = create()
        sources.saveItems(listOf(item(id, "addedword"), item(id, "obsoleteword")))
        AddVocabularySourceItemsToDeckUseCase(
            CardRepositoryRoom(databaseSource), DeckRepositoryRoom(databaseSource), sources,
            version, transactions, context, ignored,
        )("Added words", listOf(sources.fetchItemsBySourceId(id).single { it.foreignWord == "addedword" }))
        acknowledgeLocalOperations()
        ReplaceVocabularySourceDraftItemsUseCase(sources, version, transactions, context)(id, listOf(item(id)))

        val words = sources.fetchItemsBySourceId(id)
        assertEquals(setOf("addedword", "newword"), words.map { it.foreignWord }.toSet())
        assertEquals(VocabularySourceItemStatus.ADDED, words.single { it.foreignWord == "addedword" }.status)
        val json = pending().single().operationJson
        assertTrue(json.contains("addedword"))
        assertTrue(json.contains("newword"))
        assertTrue(!json.contains("obsoleteword"))
    }

    @Test
    fun `deleting a synchronized source removes its words and queues its deletion`() = withVocabularySyncFixture {
        selectConfirmedAccount()
        val id = create()
        sources.saveItems(listOf(item(id)))
        acknowledgeLocalOperations()
        RemoveVocabularySourceUseCase(sources, version, transactions, context)(id)

        assertTrue(sources.fetchSources().isEmpty())
        assertTrue(sources.fetchItemsBySourceId(id).isEmpty())
        assertEquals(1, pending().size)
        assertEquals(7L, pending().single().baseRevision)
    }

    @Test
    fun `ignored word rules become pending sync data even without a source edit`() = withVocabularySyncFixture {
        selectConfirmedAccount()
        transactions.performWithTransaction {
            ignored.saveWords(listOf(IgnoredVocabularyWord("en", "ignoredword", "meaning", 1L)))
            version.increaseVersion()
        }
        assertEquals(1, ignored.fetchWords().size)
        assertEquals(1, pending().size)
        assertTrue(pending().single().operationJson.contains("ignoredword"))
    }

    @Test
    fun `injected failure rolls back source words ignored rules version and outbox together`() = withVocabularySyncFixture {
        selectConfirmedAccount()
        val id = create()
        acknowledgeLocalOperations()
        val before = sources.fetchSourceById(id)
        val beforeVersion = version.fetchVersion()
        val failingIgnored = object : IIgnoredVocabularyWordRepository {
            override fun observeWords() = kotlinx.coroutines.flow.flow { emit(fetchWords()) }
            override suspend fun removeWords(keys: Set<com.kuts.domain.vocabularySource.VocabularySourceItemKey>) = Unit
            override suspend fun fetchWords() = ignored.fetchWords()
            override suspend fun saveWords(words: List<IgnoredVocabularyWord>) {
                ignored.saveWords(words)
                error("Injected failure after ignored rule write")
            }
        }
        assertFailsWith<IllegalStateException> {
            SaveVocabularySourceChangesUseCase(sources, failingIgnored, version, transactions, context)(
                requireNotNull(before).copy(title = "Must roll back"),
                listOf(item(id).copy(status = VocabularySourceItemStatus.IGNORED)), true,
                mapOf(item(id).reviewKey() to true),
            )
        }
        assertEquals(before, sources.fetchSourceById(id))
        assertTrue(sources.fetchItemsBySourceId(id).isEmpty())
        assertTrue(ignored.fetchWords().isEmpty())
        assertEquals(beforeVersion, version.fetchVersion())
        assertTrue(pending().isEmpty())
    }

    @Test
    fun `guest source creation stays local with no account upload`() = withVocabularySyncFixture {
        val id = create()
        assertNotNull(sources.fetchSourceById(id))
        assertTrue(pending().isEmpty())
    }

    @Test
    fun `sign in leaves guest sources untouched and does not queue them for the account`() = withVocabularySyncFixture {
        val id = create("Guest source")
        databaseSource.selectAccount(SOURCE_SYNC_ACCOUNT)
        assertTrue(sources.fetchSources().isEmpty())
        assertTrue(pending().isEmpty())
        databaseSource.selectAccount(null)
        assertEquals("Guest source", sources.fetchSourceById(id)?.title)
        assertTrue(pending().isEmpty())
    }

    @Test
    fun `signup transfers source-only guest data and queues the complete first upload`() = withVocabularySyncFixture {
        val id = create("Guest source")
        sources.saveItems(listOf(item(id)))
        GuestAccountDataTransfer(databaseSource).transfer(SOURCE_SYNC_ACCOUNT)
        assertTrue(sources.fetchSources().isEmpty())
        databaseSource.selectAccount(SOURCE_SYNC_ACCOUNT)
        assertEquals("Guest source", sources.fetchSourceById(id)?.title)
        assertEquals(1, sources.fetchItemsBySourceId(id).size)
        val queued = pending().single()
        assertEquals(0L, queued.baseRevision)
        assertTrue(queued.operationJson.contains("Guest source"))
        assertTrue(queued.operationJson.contains("newword"))
    }

    @Test
    fun `pending source change survives restart with identical operation id and payload`() = withVocabularySyncFixture {
        selectConfirmedAccount()
        val id = create()
        val before = pending()
        assertEquals(1, before.size)
        reopen()
        assertEquals(SOURCE_SYNC_ACCOUNT, databaseSource.selection.value.accountEmail)
        assertEquals(before, pending())
        assertEquals("Saved transcript", sources.fetchSourceById(id)?.title)
    }

    @Test
    fun `account switch never exposes another accounts source outbox`() = withVocabularySyncFixture {
        selectConfirmedAccount()
        val id = create()
        val before = pending()
        assertEquals(1, before.size)
        databaseSource.selectAccount(SOURCE_SYNC_OTHER_ACCOUNT)
        assertTrue(sources.fetchSources().isEmpty())
        assertTrue(pending().isEmpty())
        databaseSource.selectAccount(SOURCE_SYNC_ACCOUNT)
        assertNotNull(sources.fetchSourceById(id))
        assertEquals(before, pending())
    }

    @Test
    fun `source changes are blocked during manual sync without changing any stored data`() = withVocabularySyncFixture {
        selectConfirmedAccount()
        val id = create()
        acknowledgeLocalOperations()
        val before = sources.fetchSources()
        val beforeVersion = version.fetchVersion()
        databaseSource.beginManualSyncAttempt(SOURCE_SYNC_ACCOUNT)
        try {
            assertFailsWith<ManualSyncInProgressException> { create("Blocked create") }
            assertFailsWith<ManualSyncInProgressException> {
                RemoveVocabularySourceUseCase(sources, version, transactions, context)(id)
            }
            assertFailsWith<ManualSyncInProgressException> {
                ReplaceVocabularySourceDraftItemsUseCase(sources, version, transactions, context)(id, listOf(item(id)))
            }
        } finally {
            databaseSource.endManualSyncAttempt(SOURCE_SYNC_ACCOUNT)
        }
        assertEquals(before, sources.fetchSources())
        assertTrue(sources.fetchItemsBySourceId(id).isEmpty())
        assertEquals(beforeVersion, version.fetchVersion())
        assertTrue(pending().isEmpty())
    }
}
