package com.kuts.klaf.presentation.cardTransferring

import com.kuts.domain.common.ICoroutineContextProvider
import com.kuts.domain.common.LoadingState
import com.kuts.domain.entities.Card
import com.kuts.domain.entities.Deck
import com.kuts.domain.entities.StorageSaveVersion
import com.kuts.domain.entities.WordMeaningInsights
import com.kuts.domain.managers.IAudioPlayerManager
import com.kuts.domain.repositories.ICardRepository
import com.kuts.domain.repositories.ICrashlyticsRepository
import com.kuts.domain.repositories.IDeckRepository
import com.kuts.domain.repositories.IStorageSaveVersionRepository
import com.kuts.domain.repositories.IStorageTransactionRepository
import com.kuts.domain.useCases.DeleteCardsFromDeckUseCase
import com.kuts.domain.useCases.FetchCardsUseCase
import com.kuts.domain.useCases.FetchDeckByIdUseCase
import com.kuts.domain.useCases.FetchDeckSourceUseCase
import com.kuts.domain.useCases.TransferCardsToDeckUseCase
import com.kuts.klaf.cardTransferring.common.CardTransferringViewModel
import com.kuts.klaf.cardTransferring.common.ICardTransferringAction
import com.kuts.klaf.cardTransferring.common.ICardTransferringNavigationDestination
import com.kuts.klaf.cardTransferring.common.ICardTransferringNavigationEvent
import com.kuts.klaf.common.EventMessage
import com.kuts.klaf.common.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalCoroutinesApi::class)
class CardTransferringViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `moving selected cards updates decks cards and emits success ui events`() =
        runTest(context = mainDispatcherRule.dispatcher) {
            val sourceDeck = deck(
                id = SOURCE_DECK_ID,
                cardQuantity = 4,
                lastReviewPassDuration = 400L,
                lastFirstReviewDuration = 160L,
                lastSecondReviewDuration = 240L,
                scheduledDateInterval = 1_000L,
                isLastPassSucceeded = false,
            )
            val targetDeck = deck(
                id = TARGET_DECK_ID,
                cardQuantity = 2,
                lastReviewPassDuration = 100L,
                lastFirstReviewDuration = 45L,
                lastSecondReviewDuration = 55L,
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
            val viewModel = env.viewModel()
            val navigationEvents = mutableListOf<ICardTransferringNavigationEvent>()
            val eventMessages = mutableListOf<EventMessage>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.navigationEvent.collect(navigationEvents::add)
            }
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.eventMessage.collect(eventMessages::add)
            }

            runCurrent()
            viewModel.sendAction(ICardTransferringAction.ChangeSelectionState(position = 0))
            viewModel.sendAction(ICardTransferringAction.ChangeSelectionState(position = 1))
            runCurrent()
            viewModel.sendAction(ICardTransferringAction.MoveCards(targetDeck = targetDeck))
            runCurrent()

            val updatedSourceDeck = checkNotNull(env.deckRepository.getDeckById(SOURCE_DECK_ID))
            val updatedTargetDeck = checkNotNull(env.deckRepository.getDeckById(TARGET_DECK_ID))
            assertEquals(2, updatedSourceDeck.cardQuantity)
            assertEquals(4, updatedTargetDeck.cardQuantity)
            assertEquals(200L, updatedSourceDeck.lastReviewPassDuration)
            assertEquals(200L, updatedTargetDeck.lastReviewPassDuration)
            assertDeckMetadataUnchangedExceptQuantityAndPassDuration(sourceDeck, updatedSourceDeck)
            assertDeckMetadataUnchangedExceptQuantityAndPassDuration(targetDeck, updatedTargetDeck)
            assertEquals(
                listOf(sourceCards[2], sourceCards[3]),
                env.cardRepository.fetchCardsByDeckId(deckId = SOURCE_DECK_ID),
            )
            assertMovedCards(sourceCards = sourceCards.take(2), env = env)
            assertTrue(navigationEvents.contains(ICardTransferringNavigationEvent.ToPrevious))
            assertTrue(eventMessages.any { it.type == EventMessage.Type.Positive })
            assertEquals(1, env.saveVersionRepository.increaseCount)
            assertEquals(1, env.transactionRepository.transactionCount)
        }

    @Test
    fun `move dialog request without selected cards emits negative message and no navigation`() =
        runTest(context = mainDispatcherRule.dispatcher) {
            val env = environment()
            val viewModel = env.viewModel()
            val navigationEvents = mutableListOf<ICardTransferringNavigationEvent>()
            val eventMessages = mutableListOf<EventMessage>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.navigationEvent.collect(navigationEvents::add)
            }
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.eventMessage.collect(eventMessages::add)
            }

            runCurrent()
            viewModel.sendAction(
                ICardTransferringAction.NavigateTo(
                    destination = ICardTransferringNavigationDestination.CardMovingDialog,
                )
            )
            runCurrent()

            assertTrue(eventMessages.any { it.type == EventMessage.Type.Negative })
            assertFalse(navigationEvents.contains(ICardTransferringNavigationEvent.ToCardMovingDialog))
        }

    @Test
    fun `move dialog request with selected cards emits moving dialog navigation`() =
        runTest(context = mainDispatcherRule.dispatcher) {
            val env = environment()
            val viewModel = env.viewModel()
            val navigationEvents = mutableListOf<ICardTransferringNavigationEvent>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.navigationEvent.collect(navigationEvents::add)
            }

            runCurrent()
            viewModel.sendAction(ICardTransferringAction.ChangeSelectionState(position = 0))
            runCurrent()
            viewModel.sendAction(
                ICardTransferringAction.NavigateTo(
                    destination = ICardTransferringNavigationDestination.CardMovingDialog,
                )
            )
            runCurrent()

            assertTrue(navigationEvents.contains(ICardTransferringNavigationEvent.ToCardMovingDialog))
        }

    @Test
    fun `initial deck list excludes source deck for target choosing`() =
        runTest(context = mainDispatcherRule.dispatcher) {
            val env = environment()
            val viewModel = env.viewModel()

            runCurrent()

            assertEquals(
                listOf(TARGET_DECK_ID),
                viewModel.decks.value.map { it.id },
            )
        }

    private fun environment(
        sourceDeck: Deck = deck(
            id = SOURCE_DECK_ID,
            cardQuantity = 2,
            lastReviewPassDuration = 100L,
            lastFirstReviewDuration = 40L,
            lastSecondReviewDuration = 60L,
            scheduledDateInterval = 1_000L,
            isLastPassSucceeded = true,
        ),
        targetDeck: Deck = deck(
            id = TARGET_DECK_ID,
            cardQuantity = 1,
            lastReviewPassDuration = 30L,
            lastFirstReviewDuration = 10L,
            lastSecondReviewDuration = 20L,
            scheduledDateInterval = 2_000L,
            isLastPassSucceeded = false,
        ),
        sourceCards: List<Card> = (1..2).map { card(id = it, deckId = SOURCE_DECK_ID) },
        targetCards: List<Card> = listOf(card(id = 10, deckId = TARGET_DECK_ID)),
    ): TestEnvironment {
        val cardRepository = InMemoryCardRepository(cards = sourceCards + targetCards)
        val deckRepository = InMemoryDeckRepository(decks = listOf(sourceDeck, targetDeck))

        return TestEnvironment(
            cardRepository = cardRepository,
            deckRepository = deckRepository,
            saveVersionRepository = FakeStorageSaveVersionRepository(),
            transactionRepository = FakeStorageTransactionRepository(),
        )
    }

    private fun TestEnvironment.viewModel(): CardTransferringViewModel {
        val coroutineContextProvider = TestCoroutineContextProvider(io = mainDispatcherRule.dispatcher)

        return CardTransferringViewModel(
            sourceDeckId = SOURCE_DECK_ID,
            fetchDeckById = FetchDeckByIdUseCase(deckRepository = deckRepository),
            fetchCards = FetchCardsUseCase(cardRepository = cardRepository),
            deleteCardsFromDeckUseCase = DeleteCardsFromDeckUseCase(
                deckRepository = deckRepository,
                cardRepository = cardRepository,
                localStorageSaveVersionRepository = saveVersionRepository,
                localStorageTransactionRepository = transactionRepository,
                coroutineContextProvider = coroutineContextProvider,
            ),
            fetchDeckSource = FetchDeckSourceUseCase(deckRepository = deckRepository),
            audioPlayer = FakeAudioPlayerManager(),
            moveCardsToDeck = TransferCardsToDeckUseCase(
                cardRepository = cardRepository,
                deckRepository = deckRepository,
                localStorageSaveVersionRepository = saveVersionRepository,
                localStorageTransactionRepository = transactionRepository,
                coroutineContextProvider = coroutineContextProvider,
            ),
            crashlytics = FakeCrashlyticsRepository(),
            coroutineContextProvider = coroutineContextProvider,
        )
    }

    private suspend fun assertMovedCards(
        sourceCards: List<Card>,
        env: TestEnvironment,
    ) {
        val targetCards = env.cardRepository.fetchCardsByDeckId(deckId = TARGET_DECK_ID)

        sourceCards.forEach { sourceCard ->
            val movedCard = checkNotNull(
                targetCards.firstOrNull { it.foreignWord == sourceCard.foreignWord }
            )
            assertTrue(movedCard.id != sourceCard.id)
            assertEquals(TARGET_DECK_ID, movedCard.deckId)
            assertEquals(sourceCard.nativeWord, movedCard.nativeWord)
            assertEquals(sourceCard.ipa, movedCard.ipa)
            assertEquals(sourceCard.wordMeaningInsights, movedCard.wordMeaningInsights)
        }
    }

    private fun assertDeckMetadataUnchangedExceptQuantityAndPassDuration(
        expected: Deck,
        actual: Deck,
    ) {
        assertEquals(expected.name, actual.name)
        assertEquals(expected.creationDate, actual.creationDate)
        assertEquals(expected.reviewPassDates, actual.reviewPassDates)
        assertEquals(expected.scheduledReviewDates, actual.scheduledReviewDates)
        assertEquals(expected.scheduledDateInterval, actual.scheduledDateInterval)
        assertEquals(expected.reviewCount, actual.reviewCount)
        assertEquals(expected.lastFirstReviewDuration, actual.lastFirstReviewDuration)
        assertEquals(expected.lastSecondReviewDuration, actual.lastSecondReviewDuration)
        assertEquals(expected.isLastPassSucceeded, actual.isLastPassSucceeded)
        assertEquals(expected.id, actual.id)
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

    private class FakeAudioPlayerManager : IAudioPlayerManager {

        override val loadingState: StateFlow<LoadingState<Unit, Unit>> =
            MutableStateFlow(LoadingState.Non)

        override fun onCreate() = Unit

        override fun onResume() = Unit

        override fun onStop() = Unit

        override fun onDestroy() = Unit

        override fun preparePronunciation(word: String) = Unit

        override fun play() = Unit

        override fun preparePronunciationAndPlay(word: String) = Unit
    }

    private class FakeCrashlyticsRepository : ICrashlyticsRepository {

        override fun report(exception: Throwable) = Unit
    }

    private class InMemoryCardRepository(cards: List<Card>) : ICardRepository {

        private val cardsFlow = MutableStateFlow(cards.associateBy { it.id })
        private var nextId = (cardsFlow.value.keys.maxOrNull() ?: 0) + 1

        override suspend fun fetchCardQuantityByDeckId(deckId: Int): Int {
            return fetchCardsByDeckId(deckId = deckId).size
        }

        override suspend fun fetchAllCards(): List<Card> {
            return cardsFlow.value.values.toList()
        }

        override suspend fun insertCard(card: Card) {
            val id = if (card.id == 0 || cardsFlow.value.containsKey(card.id)) nextId++ else card.id
            cardsFlow.value = cardsFlow.value + (id to card.copy(id = id))
        }

        override suspend fun insertCardAtPath(card: Card, rootEmailPath: String) {
            insertCard(card = card)
        }

        override fun fetchObservableCardById(cardId: Int): Flow<Card?> {
            return cardsFlow.map { cards -> cards[cardId] }
        }

        override fun fetchObservableCardsByDeckId(deckId: Int): Flow<List<Card>> {
            return cardsFlow.map { cards ->
                cards.values.filter { it.deckId == deckId }.sortedBy { it.id }
            }
        }

        override suspend fun fetchCardsByDeckId(deckId: Int): List<Card> {
            return cardsFlow.value.values.filter { it.deckId == deckId }.sortedBy { it.id }
        }

        override suspend fun deleteCard(cardId: Int) {
            cardsFlow.value = cardsFlow.value - cardId
        }

        override suspend fun removeCardsOfDeck(deckId: Int) {
            cardsFlow.value = cardsFlow.value.filterValues { it.deckId != deckId }
        }

        override suspend fun checkIfCardExists(foreignWord: String): List<Deck> {
            return emptyList()
        }
    }

    private class InMemoryDeckRepository(decks: List<Deck>) : IDeckRepository {

        private val decksFlow = MutableStateFlow(decks.associateBy { it.id })

        override fun fetchDeckSource(): Flow<List<Deck>> {
            return decksFlow.map { decks -> decks.values.sortedBy { it.id } }
        }

        override suspend fun fetchAllDecks(): List<Deck> {
            return decksFlow.value.values.toList()
        }

        override fun fetchObservableDeckById(deckId: Int): Flow<Deck?> {
            return decksFlow.map { decks -> decks[deckId] }
        }

        override suspend fun insertDeck(deck: Deck) {
            decksFlow.value = decksFlow.value + (deck.id to deck)
        }

        override suspend fun insertDeckAtPath(deck: Deck, rootEmailPath: String) {
            insertDeck(deck = deck)
        }

        override suspend fun removeDeck(deckId: Int) {
            decksFlow.value = decksFlow.value - deckId
        }

        override suspend fun getDeckById(deckId: Int): Deck? {
            return decksFlow.value[deckId]
        }

        override suspend fun getCardQuantityInDeck(deckId: Int): Int {
            return decksFlow.value[deckId]?.cardQuantity ?: 0
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
