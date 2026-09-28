package com.kuts.domain.useCases

import com.kuts.domain.entities.Card
import com.kuts.domain.common.ReviewedDeckCardAdditionException
import com.kuts.domain.entities.Deck
import com.kuts.domain.entities.WordMeaningInsights
import com.kuts.domain.repositories.IStorageTransactionRepository
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertIs

private const val SOURCE_DECK_ID = 101
private const val TARGET_DECK_ID = 202

private class CountingTransaction : IStorageTransactionRepository {
    var transactionCount = 0
        private set

    override suspend fun <R> performWithTransaction(block: suspend () -> R): R {
        transactionCount++
        return block()
    }
}

class ReviewedDeckCardMutationContractTest {

    @Test
    fun `new card is accepted before the first review`() = runTest {
        val deck = deck(reviewCount = 0)
        val cards = TestCardRepository()
        val decks = TestDeckRepository(listOf(deck))
        val version = TestStorageSaveVersionRepository(initialVersion = 0)
        val transaction = CountingTransaction()

        addCard(cards, decks, version, transaction).invoke(testCard(id = 0, deckId = deck.id))

        assertEquals(1, cards.fetchCardQuantityByDeckId(deck.id))
        assertEquals(1, decks.getDeckById(deck.id)?.cardQuantity)
        assertEquals(1L, version.currentVersion?.version)
        assertEquals(1, transaction.transactionCount)
    }

    @Test
    fun `new card is rejected immediately after the first review`() = runTest {
        val deck = deck(reviewCount = 1)
        val cards = TestCardRepository()
        val decks = TestDeckRepository(listOf(deck))
        val version = TestStorageSaveVersionRepository(initialVersion = 0)
        val transaction = CountingTransaction()

        val result = runCatching {
            addCard(cards, decks, version, transaction).invoke(testCard(id = 0, deckId = deck.id))
        }

        assertTrue(result.isFailure)
        assertIs<ReviewedDeckCardAdditionException>(result.exceptionOrNull())
        assertTrue(cards.fetchAllCards().isEmpty())
        assertEquals(deck, decks.getDeckById(deck.id))
        assertEquals(0L, version.currentVersion?.version)
    }

    @Test
    fun `new card is rejected after many reviews too`() = runTest {
        val deck = deck(reviewCount = 6)
        val cards = TestCardRepository()
        val decks = TestDeckRepository(listOf(deck))
        val version = TestStorageSaveVersionRepository(initialVersion = 0)
        val transaction = CountingTransaction()

        val result = runCatching {
            addCard(cards, decks, version, transaction).invoke(testCard(id = 0, deckId = deck.id))
        }

        assertTrue(result.isFailure)
        assertTrue(cards.fetchAllCards().isEmpty())
        assertEquals(0L, version.currentVersion?.version)
    }

    @Test
    fun `existing card in a reviewed deck can still be edited`() = runTest {
        val deck = deck(reviewCount = 6, cardQuantity = 1)
        val original = testCard(id = 17, deckId = deck.id)
        val edited = original.copy(nativeWord = "edited native word")
        val cards = TestCardRepository(listOf(original))
        val version = TestStorageSaveVersionRepository(initialVersion = 0)
        val transaction = CountingTransaction()

        UpdateCardUseCase(
            cardRepository = cards,
            localStorageSaveVersionRepository = version,
            localStorageTransactionRepository = transaction,
            coroutineContextProvider = TestCoroutineContextProvider(EmptyCoroutineContext),
        ).invoke(edited)

        assertEquals(listOf(edited), cards.fetchCardsByDeckId(deck.id))
        assertEquals(1L, version.currentVersion?.version)
        assertEquals(1, transaction.transactionCount)
    }

    @Test
    fun `existing card in a reviewed deck can still be deleted`() = runTest {
        val deck = deck(reviewCount = 1, cardQuantity = 1)
        val original = testCard(id = 17, deckId = deck.id)
        val cards = TestCardRepository(listOf(original))
        val decks = TestDeckRepository(listOf(deck))
        val version = TestStorageSaveVersionRepository(initialVersion = 0)
        val transaction = CountingTransaction()

        DeleteCardsFromDeckUseCase(
            deckRepository = decks,
            cardRepository = cards,
            localStorageSaveVersionRepository = version,
            localStorageTransactionRepository = transaction,
            coroutineContextProvider = TestCoroutineContextProvider(EmptyCoroutineContext),
        ).invoke(deck.id, original.id)

        assertTrue(cards.fetchCardsByDeckId(deck.id).isEmpty())
        assertEquals(0, decks.getDeckById(deck.id)?.cardQuantity)
        assertEquals(deck.reviewCount, decks.getDeckById(deck.id)?.reviewCount)
        assertEquals(1L, version.currentVersion?.version)
    }

    @Test
    fun `moving out of a reviewed deck into an unreviewed deck preserves card identity and data`() = runTest {
        val source = deck(id = SOURCE_DECK_ID, reviewCount = 6, cardQuantity = 1)
        val target = deck(id = TARGET_DECK_ID, reviewCount = 0)
        val original = testCard(id = 17, deckId = source.id, imageAssetId = "image-17")
            .copy(
                wordMeaningInsights = WordMeaningInsights(word = "foreign-17", language = "en"),
                syncId = "card-sync-17",
                lastChangedServerRevision = 12L,
            )
        val cards = TestCardRepository(listOf(original))
        val decks = TestDeckRepository(listOf(source, target))
        val version = TestStorageSaveVersionRepository(initialVersion = 0)
        val transaction = CountingTransaction()

        moveCards(cards, decks, version, transaction).invoke(source, target, original)

        assertTrue(cards.fetchCardsByDeckId(source.id).isEmpty())
        assertEquals(listOf(original.copy(deckId = target.id)), cards.fetchCardsByDeckId(target.id))
        assertEquals(0, decks.getDeckById(source.id)?.cardQuantity)
        assertEquals(1, decks.getDeckById(target.id)?.cardQuantity)
        assertEquals(1L, version.currentVersion?.version)
    }

    @Test
    fun `moving into a reviewed deck is rejected without changing either deck or the card`() = runTest {
        val source = deck(id = SOURCE_DECK_ID, reviewCount = 0, cardQuantity = 1)
        val target = deck(id = TARGET_DECK_ID, reviewCount = 1)
        val original = testCard(id = 17, deckId = source.id, imageAssetId = "image-17")
        val cards = TestCardRepository(listOf(original))
        val decks = TestDeckRepository(listOf(source, target))
        val version = TestStorageSaveVersionRepository(initialVersion = 0)
        val transaction = CountingTransaction()

        val result = runCatching {
            moveCards(cards, decks, version, transaction).invoke(source, target, original)
        }

        assertTrue(result.isFailure)
        assertEquals(listOf(original), cards.fetchCardsByDeckId(source.id))
        assertTrue(cards.fetchCardsByDeckId(target.id).isEmpty())
        assertEquals(source, decks.getDeckById(source.id))
        assertEquals(target, decks.getDeckById(target.id))
        assertEquals(0L, version.currentVersion?.version)
    }

    @Test
    fun `moving checks current destination state rather than stale UI snapshot`() = runTest {
        val source = deck(id = SOURCE_DECK_ID, reviewCount = 0, cardQuantity = 1)
        val staleTarget = deck(id = TARGET_DECK_ID, reviewCount = 0)
        val currentTarget = staleTarget.copy(reviewCount = 1)
        val original = testCard(id = 17, deckId = source.id)
        val cards = TestCardRepository(listOf(original))
        val decks = TestDeckRepository(listOf(source, currentTarget))
        val version = TestStorageSaveVersionRepository(initialVersion = 0)
        val transaction = CountingTransaction()

        val result = runCatching {
            moveCards(cards, decks, version, transaction).invoke(source, staleTarget, original)
        }

        assertTrue(result.isFailure)
        assertEquals(listOf(original), cards.fetchCardsByDeckId(source.id))
        assertTrue(cards.fetchCardsByDeckId(deckId = TARGET_DECK_ID).isEmpty())
        assertEquals(currentTarget, decks.getDeckById(TARGET_DECK_ID))
        assertEquals(0L, version.currentVersion?.version)
    }

    private fun deck(
        id: Int = TARGET_DECK_ID,
        reviewCount: Int,
        cardQuantity: Int = 0,
    ): Deck = Deck(
        id = id,
        name = "deck-$id",
        creationDate = 1_000L,
        reviewCount = reviewCount,
        cardQuantity = cardQuantity,
    )

    private fun addCard(
        cards: TestCardRepository,
        decks: TestDeckRepository,
        version: TestStorageSaveVersionRepository,
        transaction: CountingTransaction,
    ): AddNewCardIntoDeckUseCase = AddNewCardIntoDeckUseCase(
        deckRepository = decks,
        cardRepository = cards,
        localStorageSaveVersionRepository = version,
        localStorageTransactionRepository = transaction,
        coroutineContextProvider = TestCoroutineContextProvider(EmptyCoroutineContext),
    )

    private fun moveCards(
        cards: TestCardRepository,
        decks: TestDeckRepository,
        version: TestStorageSaveVersionRepository,
        transaction: CountingTransaction,
    ): TransferCardsToDeckUseCase = TransferCardsToDeckUseCase(
        cardRepository = cards,
        deckRepository = decks,
        localStorageSaveVersionRepository = version,
        localStorageTransactionRepository = transaction,
        coroutineContextProvider = TestCoroutineContextProvider(EmptyCoroutineContext),
    )
}
