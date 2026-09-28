package com.kuts.klaf.room

import com.kuts.domain.entities.Card
import com.kuts.domain.entities.Deck
import com.kuts.domain.entities.IgnoredVocabularyWord
import com.kuts.domain.entities.VocabularySource
import com.kuts.domain.entities.VocabularySourceItem
import com.kuts.domain.entities.VocabularySourceItemCategory
import com.kuts.domain.entities.VocabularySourceItemStatus
import com.kuts.klaf.room.databases.ActiveLocalRoomDatabase
import com.kuts.klaf.room.databases.DesktopSelectedAccountStore
import com.kuts.klaf.room.databases.ScopedKlafRoomDatabaseFactory
import com.kuts.klaf.room.repositoryImplementations.CardRepositoryRoom
import com.kuts.klaf.room.repositoryImplementations.DeckRepositoryRoom
import com.kuts.klaf.room.repositoryImplementations.GuestAccountDataTransfer
import com.kuts.klaf.room.repositoryImplementations.IgnoredVocabularyWordRepositoryRoom
import com.kuts.klaf.room.repositoryImplementations.VocabularySourceRepositoryRoom
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

private fun withIntegrationAccountSource(block: suspend (ActiveLocalRoomDatabase) -> Unit) = runBlocking {
    val root = Files.createTempDirectory("klaf-integration-ownership-").toFile()
    val source = ActiveLocalRoomDatabase(ScopedKlafRoomDatabaseFactory(root), DesktopSelectedAccountStore(root))
    try {
        block(source)
    } finally {
        source.close()
        check(root.name.startsWith("klaf-integration-ownership-"))
        root.walkBottomUp().forEach { it.delete() }
    }
}

class BranchIntegrationOwnershipTest {

    @Test
    fun sourceToCardUsesAccountOutboxAndRespectsReviewAndSyncRestrictions() = withIntegrationAccountSource { source ->
        val decks = DeckRepositoryRoom(source)
        val cards = CardRepositoryRoom(source)
        val sources = VocabularySourceRepositoryRoom(source)
        val deckId = decks.insertDeck(Deck("Guest", creationDate = 1))
        val sourceId = sources.saveSource(VocabularySource("Words", createdAt = 1, updatedAt = 2))
        val draftItem = VocabularySourceItem(sourceId, foreignWord = "word", nativeWord = "meaning",
            category = VocabularySourceItemCategory.NEW, createdAt = 1, updatedAt = 2)
        sources.saveItems(listOf(draftItem))
        val item = sources.fetchItemsBySourceId(sourceId).single()
        GuestAccountDataTransfer(source).transfer("alice@example.com")
        source.selectAccount("alice@example.com")
        val addItems = com.kuts.domain.useCases.AddVocabularySourceItemsToDeckUseCase(cards, decks, sources,
            com.kuts.klaf.room.repositoryImplementations.StorageSaveVersionRepositoryRoom(source),
            com.kuts.klaf.room.repositoryImplementations.StorageTransactionRepositoryRoom(source),
            object : com.kuts.domain.common.ICoroutineContextProvider {
                override val io = kotlin.coroutines.EmptyCoroutineContext
            })
        val deck = requireNotNull(decks.getDeckById(deckId))
        source.beginManualSyncAttempt("alice@example.com")
        try {
            assertFailsWith<com.kuts.klaf.room.databases.ManualSyncInProgressException> { addItems(deck, listOf(item)) }
        } finally {
            source.endManualSyncAttempt("alice@example.com")
        }
        assertTrue(cards.fetchAllCards().isEmpty())
        assertEquals(1, addItems(deck, listOf(item)))
        assertEquals(2, source.current().pendingSyncOperationDao().allPending().size)
        val added = sources.fetchItemsBySourceId(sourceId).single()
        assertEquals(VocabularySourceItemStatus.ADDED, added.status)
        assertEquals(cards.fetchAllCards().single().id, added.createdCardId)
        decks.insertDeck(requireNotNull(decks.getDeckById(deckId)).copy(reviewCount = 1))
        assertFailsWith<com.kuts.domain.common.ReviewedDeckCardAdditionException> {
            addItems(deck, listOf(item.copy(foreignWord = "second")))
        }
        assertEquals(1, cards.fetchAllCards().size)
        assertEquals(2, source.current().pendingSyncOperationDao().allPending().size)
    }

    @Test
    fun signupTransfersSourceCardLinksAndIgnoredRulesAndClearsGuestCopy() = withIntegrationAccountSource { source ->
        val sources = VocabularySourceRepositoryRoom(source)
        val ignored = IgnoredVocabularyWordRepositoryRoom(source)
        val deckId = DeckRepositoryRoom(source).insertDeck(Deck("Guest", creationDate = 1))
        val cardId = CardRepositoryRoom(source).insertCard(Card(deckId, "meaning", "word", emptyList()))
        val sourceId = sources.saveSource(VocabularySource("Transcript", rawText = "word", createdAt = 1, updatedAt = 2))
        sources.saveItems(listOf(VocabularySourceItem(sourceId, foreignWord = "word", nativeWord = "meaning",
            category = VocabularySourceItemCategory.NEW, status = VocabularySourceItemStatus.ADDED,
            createdCardId = cardId, targetDeckId = deckId, createdAt = 1, updatedAt = 2)))
        ignored.saveWords(listOf(IgnoredVocabularyWord("en", "other", "meaning", 1)))
        val expectedSources = sources.fetchSources()
        val expectedItems = sources.fetchItemsBySourceId(sourceId)
        val expectedIgnored = ignored.fetchWords()

        GuestAccountDataTransfer(source).transfer("alice@example.com")
        assertTrue(sources.fetchSources().isEmpty())
        assertTrue(sources.fetchItemsBySourceId(sourceId).isEmpty())
        assertTrue(ignored.fetchWords().isEmpty())
        source.selectAccount("alice@example.com")
        assertEquals(expectedSources, sources.fetchSources())
        assertEquals(expectedItems, sources.fetchItemsBySourceId(sourceId))
        assertEquals(expectedIgnored, ignored.fetchWords())
        assertEquals(cardId, CardRepositoryRoom(source).fetchAllCards().single().id)
        source.selectAccount("bob@example.com")
        assertTrue(sources.fetchSources().isEmpty())
        assertTrue(ignored.fetchWords().isEmpty())
    }

    @Test
    fun sourceOnlyTransferSurvivesInterruptionAndRetriesWithoutDuplicates() = withIntegrationAccountSource { source ->
        val sources = VocabularySourceRepositoryRoom(source)
        val sourceId = sources.saveSource(VocabularySource("No deck yet", createdAt = 1, updatedAt = 2))
        sources.saveItems(listOf(VocabularySourceItem(sourceId, foreignWord = "word", nativeWord = "meaning",
            category = VocabularySourceItemCategory.NEW, createdAt = 1, updatedAt = 2)))
        val expected = sources.fetchSources()
        assertFailsWith<IllegalStateException> {
            GuestAccountDataTransfer(source, afterAccountCopy = { error("interrupted") }).transfer("alice@example.com")
        }
        assertEquals(expected, sources.fetchSources())
        GuestAccountDataTransfer(source).transfer("alice@example.com")
        GuestAccountDataTransfer(source).transfer("alice@example.com")
        assertTrue(sources.fetchSources().isEmpty())
        source.selectAccount("alice@example.com")
        assertEquals(expected, sources.fetchSources())
        assertEquals(1, sources.fetchItemsBySourceId(sourceId).size)
        assertTrue(source.current().pendingSyncOperationDao().allPending().isEmpty())
    }
}
