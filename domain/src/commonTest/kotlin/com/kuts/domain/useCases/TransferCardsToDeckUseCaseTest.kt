package com.kuts.domain.useCases

import com.kuts.domain.common.ICoroutineContextProvider
import com.kuts.domain.entities.Card
import com.kuts.domain.entities.Deck
import com.kuts.domain.entities.StorageSaveVersion
import com.kuts.domain.entities.WordMeaningInsights
import com.kuts.domain.repositories.ICardRepository
import com.kuts.domain.repositories.IDeckRepository
import com.kuts.domain.repositories.IStorageSaveVersionRepository
import com.kuts.domain.repositories.IStorageTransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TransferCardsToDeckUseCaseTest {

    @Test
    fun `transferring multiple cards to non empty deck updates ownership quantities and pass durations`() =
        runTest {
            val sourceDeck = deck(
                id = SOURCE_DECK_ID,
                cardQuantity = 4,
                lastReviewPassDuration = 400L,
                lastFirstReviewDuration = 110L,
                lastSecondReviewDuration = 90L,
                scheduledDateInterval = 1_000L,
                isLastPassSucceeded = false,
            )
            val targetDeck = deck(
                id = TARGET_DECK_ID,
                cardQuantity = 2,
                lastReviewPassDuration = 100L,
                lastFirstReviewDuration = 70L,
                lastSecondReviewDuration = 30L,
                scheduledDateInterval = 2_000L,
                isLastPassSucceeded = true,
            )
            val sourceCards = (1..4).map { card(id = it, deckId = SOURCE_DECK_ID) }
            val targetCards = (10..11).map { card(id = it, deckId = TARGET_DECK_ID) }
            val env = environment(
                sourceDeck = sourceDeck,
                targetDeck = targetDeck,
                sourceCards = sourceCards,
                targetCards = targetCards,
            )

            env.useCase(io = UnconfinedTestDispatcher(testScheduler)).invoke(
                sourceDeck = sourceDeck,
                targetDeck = targetDeck,
                cardsToMove = arrayOf(sourceCards[0], sourceCards[1]),
            )

            val updatedSourceDeck = env.deckRepository.getDeckById(SOURCE_DECK_ID)
            val updatedTargetDeck = env.deckRepository.getDeckById(TARGET_DECK_ID)
            assertNotNull(updatedSourceDeck)
            assertNotNull(updatedTargetDeck)
            assertEquals(expected = 2, actual = updatedSourceDeck.cardQuantity)
            assertEquals(expected = 4, actual = updatedTargetDeck.cardQuantity)
            assertEquals(expected = 200L, actual = updatedSourceDeck.lastReviewPassDuration)
            assertEquals(expected = 200L, actual = updatedTargetDeck.lastReviewPassDuration)
            assertDeckMetadataUnchangedExceptQuantityAndPassDuration(
                expected = sourceDeck,
                actual = updatedSourceDeck,
            )
            assertDeckMetadataUnchangedExceptQuantityAndPassDuration(
                expected = targetDeck,
                actual = updatedTargetDeck,
            )
            assertMovedCards(sourceCards = sourceCards.take(2), env = env)
            assertEquals(
                expected = listOf(sourceCards[2], sourceCards[3]),
                actual = env.cardRepository.fetchCardsByDeckId(deckId = SOURCE_DECK_ID),
            )
            assertEquals(expected = 1, actual = env.saveVersionRepository.increaseCount)
            assertEquals(expected = 1, actual = env.transactionRepository.transactionCount)
        }

    @Test
    fun `transferring one card recalculates durations by per card averages`() = runTest {
        val sourceDeck = deck(
            id = SOURCE_DECK_ID,
            cardQuantity = 3,
            lastReviewPassDuration = 90L,
            lastFirstReviewDuration = 40L,
            lastSecondReviewDuration = 50L,
            scheduledDateInterval = 1_500L,
            isLastPassSucceeded = true,
        )
        val targetDeck = deck(
            id = TARGET_DECK_ID,
            cardQuantity = 3,
            lastReviewPassDuration = 300L,
            lastFirstReviewDuration = 130L,
            lastSecondReviewDuration = 170L,
            scheduledDateInterval = 2_500L,
            isLastPassSucceeded = false,
        )
        val sourceCards = (1..3).map { card(id = it, deckId = SOURCE_DECK_ID) }
        val targetCards = (10..12).map { card(id = it, deckId = TARGET_DECK_ID) }
        val env = environment(
            sourceDeck = sourceDeck,
            targetDeck = targetDeck,
            sourceCards = sourceCards,
            targetCards = targetCards,
        )

        env.useCase(io = UnconfinedTestDispatcher(testScheduler)).invoke(
            sourceDeck = sourceDeck,
            targetDeck = targetDeck,
            cardsToMove = arrayOf(sourceCards.first()),
        )

        val updatedSourceDeck = env.deckRepository.getDeckById(SOURCE_DECK_ID)
        val updatedTargetDeck = env.deckRepository.getDeckById(TARGET_DECK_ID)
        assertNotNull(updatedSourceDeck)
        assertNotNull(updatedTargetDeck)
        assertEquals(expected = 2, actual = updatedSourceDeck.cardQuantity)
        assertEquals(expected = 4, actual = updatedTargetDeck.cardQuantity)
        assertEquals(expected = 60L, actual = updatedSourceDeck.lastReviewPassDuration)
        assertEquals(expected = 400L, actual = updatedTargetDeck.lastReviewPassDuration)
        assertDeckMetadataUnchangedExceptQuantityAndPassDuration(
            expected = sourceDeck,
            actual = updatedSourceDeck,
        )
        assertDeckMetadataUnchangedExceptQuantityAndPassDuration(
            expected = targetDeck,
            actual = updatedTargetDeck,
        )
        assertMovedCards(sourceCards = listOf(sourceCards.first()), env = env)
    }

    @Test
    fun `transferring cards to empty target uses source average duration for target pass duration`() =
        runTest {
            val sourceDeck = deck(
                id = SOURCE_DECK_ID,
                cardQuantity = 4,
                lastReviewPassDuration = 400L,
                lastFirstReviewDuration = 150L,
                lastSecondReviewDuration = 250L,
                scheduledDateInterval = 1_700L,
                isLastPassSucceeded = true,
            )
            val targetDeck = deck(
                id = TARGET_DECK_ID,
                cardQuantity = 0,
                lastReviewPassDuration = 700L,
                lastFirstReviewDuration = 300L,
                lastSecondReviewDuration = 400L,
                scheduledDateInterval = 2_700L,
                isLastPassSucceeded = false,
            )
            val sourceCards = (1..4).map { card(id = it, deckId = SOURCE_DECK_ID) }
            val env = environment(
                sourceDeck = sourceDeck,
                targetDeck = targetDeck,
                sourceCards = sourceCards,
                targetCards = emptyList(),
            )

            env.useCase(io = UnconfinedTestDispatcher(testScheduler)).invoke(
                sourceDeck = sourceDeck,
                targetDeck = targetDeck,
                cardsToMove = arrayOf(sourceCards[0], sourceCards[1]),
            )

            val updatedSourceDeck = env.deckRepository.getDeckById(SOURCE_DECK_ID)
            val updatedTargetDeck = env.deckRepository.getDeckById(TARGET_DECK_ID)
            assertNotNull(updatedSourceDeck)
            assertNotNull(updatedTargetDeck)
            assertEquals(expected = 2, actual = updatedSourceDeck.cardQuantity)
            assertEquals(expected = 2, actual = updatedTargetDeck.cardQuantity)
            assertEquals(expected = 200L, actual = updatedSourceDeck.lastReviewPassDuration)
            assertEquals(expected = 200L, actual = updatedTargetDeck.lastReviewPassDuration)
            assertDeckMetadataUnchangedExceptQuantityAndPassDuration(
                expected = sourceDeck,
                actual = updatedSourceDeck,
            )
            assertDeckMetadataUnchangedExceptQuantityAndPassDuration(
                expected = targetDeck,
                actual = updatedTargetDeck,
            )
            assertMovedCards(sourceCards = sourceCards.take(2), env = env)
        }

    @Test
    fun `transferring all source cards sets source quantity and pass duration to zero`() = runTest {
        val sourceDeck = deck(
            id = SOURCE_DECK_ID,
            cardQuantity = 2,
            lastReviewPassDuration = 100L,
            lastFirstReviewDuration = 45L,
            lastSecondReviewDuration = 55L,
            scheduledDateInterval = 3_000L,
            isLastPassSucceeded = false,
        )
        val targetDeck = deck(
            id = TARGET_DECK_ID,
            cardQuantity = 1,
            lastReviewPassDuration = 30L,
            lastFirstReviewDuration = 10L,
            lastSecondReviewDuration = 20L,
            scheduledDateInterval = 4_000L,
            isLastPassSucceeded = true,
        )
        val sourceCards = (1..2).map { card(id = it, deckId = SOURCE_DECK_ID) }
        val targetCards = listOf(card(id = 10, deckId = TARGET_DECK_ID))
        val env = environment(
            sourceDeck = sourceDeck,
            targetDeck = targetDeck,
            sourceCards = sourceCards,
            targetCards = targetCards,
        )

        env.useCase(io = UnconfinedTestDispatcher(testScheduler)).invoke(
            sourceDeck = sourceDeck,
            targetDeck = targetDeck,
            cardsToMove = sourceCards.toTypedArray(),
        )

        val updatedSourceDeck = env.deckRepository.getDeckById(SOURCE_DECK_ID)
        val updatedTargetDeck = env.deckRepository.getDeckById(TARGET_DECK_ID)
        assertNotNull(updatedSourceDeck)
        assertNotNull(updatedTargetDeck)
        assertEquals(expected = 0, actual = updatedSourceDeck.cardQuantity)
        assertEquals(expected = 3, actual = updatedTargetDeck.cardQuantity)
        assertEquals(expected = 0L, actual = updatedSourceDeck.lastReviewPassDuration)
        assertEquals(expected = 90L, actual = updatedTargetDeck.lastReviewPassDuration)
        assertDeckMetadataUnchangedExceptQuantityAndPassDuration(
            expected = sourceDeck,
            actual = updatedSourceDeck,
        )
        assertDeckMetadataUnchangedExceptQuantityAndPassDuration(
            expected = targetDeck,
            actual = updatedTargetDeck,
        )
        assertEquals(
            expected = emptyList(),
            actual = env.cardRepository.fetchCardsByDeckId(deckId = SOURCE_DECK_ID),
        )
        assertMovedCards(sourceCards = sourceCards, env = env)
    }

    private fun environment(
        sourceDeck: Deck,
        targetDeck: Deck,
        sourceCards: List<Card>,
        targetCards: List<Card>,
    ): TestEnvironment {
        return TestEnvironment(
            cardRepository = InMemoryCardRepository(cards = sourceCards + targetCards),
            deckRepository = InMemoryDeckRepository(decks = listOf(sourceDeck, targetDeck)),
            saveVersionRepository = FakeStorageSaveVersionRepository(),
            transactionRepository = FakeStorageTransactionRepository(),
        )
    }

    private fun TestEnvironment.useCase(io: CoroutineContext): TransferCardsToDeckUseCase {
        return TransferCardsToDeckUseCase(
            cardRepository = cardRepository,
            deckRepository = deckRepository,
            localStorageSaveVersionRepository = saveVersionRepository,
            localStorageTransactionRepository = transactionRepository,
            coroutineContextProvider = TestCoroutineContextProvider(io = io),
        )
    }

    private suspend fun assertMovedCards(
        sourceCards: List<Card>,
        env: TestEnvironment,
    ) {
        val targetCards = env.cardRepository.fetchCardsByDeckId(deckId = TARGET_DECK_ID)

        sourceCards.forEach { sourceCard ->
            val movedCard = targetCards.firstOrNull { it.foreignWord == sourceCard.foreignWord }
            assertNotNull(movedCard)
            assertTrue(actual = movedCard.id != sourceCard.id)
            assertEquals(expected = TARGET_DECK_ID, actual = movedCard.deckId)
            assertEquals(expected = sourceCard.nativeWord, actual = movedCard.nativeWord)
            assertEquals(expected = sourceCard.ipa, actual = movedCard.ipa)
            assertEquals(
                expected = sourceCard.wordMeaningInsights,
                actual = movedCard.wordMeaningInsights,
            )
        }
    }

    private fun assertDeckMetadataUnchangedExceptQuantityAndPassDuration(
        expected: Deck,
        actual: Deck,
    ) {
        assertEquals(expected = expected.name, actual = actual.name)
        assertEquals(expected = expected.creationDate, actual = actual.creationDate)
        assertEquals(expected = expected.reviewPassDates, actual = actual.reviewPassDates)
        assertEquals(expected = expected.scheduledReviewDates, actual = actual.scheduledReviewDates)
        assertEquals(expected = expected.scheduledDateInterval, actual = actual.scheduledDateInterval)
        assertEquals(expected = expected.reviewCount, actual = actual.reviewCount)
        assertEquals(expected = expected.lastFirstReviewDuration, actual = actual.lastFirstReviewDuration)
        assertEquals(expected = expected.lastSecondReviewDuration, actual = actual.lastSecondReviewDuration)
        assertEquals(expected = expected.isLastPassSucceeded, actual = actual.isLastPassSucceeded)
        assertEquals(expected = expected.id, actual = actual.id)
    }

    private fun deck(
        id: Int,
        cardQuantity: Int,
        lastReviewPassDuration: Long,
        lastFirstReviewDuration: Long,
        lastSecondReviewDuration: Long,
        scheduledDateInterval: Long,
        isLastPassSucceeded: Boolean,
    ): Deck {
        return Deck(
            name = "deck-$id",
            creationDate = id * 1_000L,
            reviewPassDates = listOf(id * 10L, id * 20L),
            scheduledReviewDates = listOf(id * 30L),
            scheduledDateInterval = scheduledDateInterval,
            reviewCount = id + 6,
            cardQuantity = cardQuantity,
            lastFirstReviewDuration = lastFirstReviewDuration,
            lastSecondReviewDuration = lastSecondReviewDuration,
            lastReviewPassDuration = lastReviewPassDuration,
            isLastPassSucceeded = isLastPassSucceeded,
            id = id,
        )
    }

    private fun card(id: Int, deckId: Int): Card {
        return Card(
            id = id,
            deckId = deckId,
            nativeWord = "native-$id",
            foreignWord = "foreign-$id",
            ipa = emptyList(),
            wordMeaningInsights = WordMeaningInsights(
                word = "foreign-$id",
                language = "en",
            ),
        )
    }

    private data class TestEnvironment(
        val cardRepository: InMemoryCardRepository,
        val deckRepository: InMemoryDeckRepository,
        val saveVersionRepository: FakeStorageSaveVersionRepository,
        val transactionRepository: FakeStorageTransactionRepository,
    )

    private class TestCoroutineContextProvider(
        override val io: CoroutineContext,
    ) : ICoroutineContextProvider

    private class InMemoryCardRepository(cards: List<Card>) : ICardRepository {

        private val cardsById = cards.associateBy { it.id }.toMutableMap()
        private var nextId = (cardsById.keys.maxOrNull() ?: 0) + 1

        override suspend fun fetchCardQuantityByDeckId(deckId: Int): Int {
            return fetchCardsByDeckId(deckId = deckId).size
        }

        override suspend fun fetchAllCards(): List<Card> {
            return cardsById.values.toList()
        }

        override suspend fun insertCard(card: Card) {
            val id = if (card.id == 0 || cardsById.containsKey(card.id)) nextId++ else card.id
            cardsById[id] = card.copy(id = id)
        }

        override suspend fun insertCardAtPath(card: Card, rootEmailPath: String) {
            throw UnsupportedOperationException()
        }

        override fun fetchObservableCardById(cardId: Int): Flow<Card?> {
            return flowOf(cardsById[cardId])
        }

        override fun fetchObservableCardsByDeckId(deckId: Int): Flow<List<Card>> {
            return flowOf(cardsById.values.filter { it.deckId == deckId })
        }

        override suspend fun fetchCardsByDeckId(deckId: Int): List<Card> {
            return cardsById.values.filter { it.deckId == deckId }.sortedBy { it.id }
        }

        override suspend fun deleteCard(cardId: Int) {
            cardsById.remove(cardId)
        }

        override suspend fun removeCardsOfDeck(deckId: Int) {
            cardsById.entries.removeAll { (_, card) -> card.deckId == deckId }
        }

        override suspend fun checkIfCardExists(foreignWord: String): List<Deck> {
            throw UnsupportedOperationException()
        }
    }

    private class InMemoryDeckRepository(decks: List<Deck>) : IDeckRepository {

        private val decksById = decks.associateBy { it.id }.toMutableMap()

        override fun fetchDeckSource(): Flow<List<Deck>> {
            return flowOf(decksById.values.toList())
        }

        override suspend fun fetchAllDecks(): List<Deck> {
            return decksById.values.toList()
        }

        override fun fetchObservableDeckById(deckId: Int): Flow<Deck?> {
            return flowOf(decksById[deckId])
        }

        override suspend fun insertDeck(deck: Deck) {
            decksById[deck.id] = deck
        }

        override suspend fun insertDeckAtPath(deck: Deck, rootEmailPath: String) {
            throw UnsupportedOperationException()
        }

        override suspend fun removeDeck(deckId: Int) {
            decksById.remove(deckId)
        }

        override suspend fun getDeckById(deckId: Int): Deck? {
            return decksById[deckId]
        }

        override suspend fun getCardQuantityInDeck(deckId: Int): Int {
            return decksById[deckId]?.cardQuantity ?: 0
        }
    }

    private class FakeStorageSaveVersionRepository : IStorageSaveVersionRepository {

        var increaseCount = 0
            private set

        override suspend fun fetchVersion(): StorageSaveVersion? {
            return StorageSaveVersion(version = increaseCount.toLong())
        }

        override suspend fun insertVersion(version: StorageSaveVersion) = Unit

        override suspend fun insertVersionAtPath(
            version: StorageSaveVersion,
            rootEmailPath: String,
        ) = Unit

        override suspend fun increaseVersion() {
            increaseCount++
        }
    }

    private class FakeStorageTransactionRepository : IStorageTransactionRepository {

        var transactionCount = 0
            private set

        override suspend fun <R> performWithTransaction(block: suspend () -> R) {
            transactionCount++
            block()
        }
    }

    private companion object {
        const val SOURCE_DECK_ID = 1
        const val TARGET_DECK_ID = 2
    }
}
