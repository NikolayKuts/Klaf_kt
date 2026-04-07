package com.kuts.klaf.presentation.deckRepetition

import androidx.lifecycle.SavedStateHandle
import com.kuts.domain.common.ICoroutineContextProvider
import com.kuts.domain.common.LoadingState
import com.kuts.domain.entities.Card
import com.kuts.domain.entities.Deck
import com.kuts.domain.entities.DeckRepetitionInfo
import com.kuts.domain.entities.StorageSaveVersion
import com.kuts.domain.enums.DifficultyRecallingLevel.EASY
import com.kuts.domain.managers.IAudioPlayerManager
import com.kuts.domain.managers.IDeckReviewNotifierManager
import com.kuts.domain.managers.IDeckReviewScheduler
import com.kuts.domain.repositories.ICardRepository
import com.kuts.domain.repositories.ICrashlyticsRepository
import com.kuts.domain.repositories.IDeckRepetitionInfoRepository
import com.kuts.domain.repositories.IDeckRepository
import com.kuts.domain.repositories.IStorageSaveVersionRepository
import com.kuts.domain.repositories.IStorageTransactionRepository
import com.kuts.domain.useCases.DeleteCardsFromDeckUseCase
import com.kuts.domain.useCases.FetchCardsUseCase
import com.kuts.domain.useCases.FetchDeckByIdUseCase
import com.kuts.domain.useCases.SaveDeckReviewInfoUseCase
import com.kuts.domain.useCases.UpdateDeckUseCase
import com.kuts.klaf.common.MainDispatcherRule
import com.kuts.klaf.common.RepetitionTimer
import com.kuts.klaf.deckRepetition.DeckReviewViewModel
import com.kuts.klaf.deckRepetition.savedStateHandle.DeckReviewSavedStateHandleStateStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalCoroutinesApi::class)
class DeckReviewViewModelRegressionTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `editing the first card before answering can finish repetition before full pass`() =
        runTest(context = mainDispatcherRule.dispatcher) {
            val deckId = 1
            val initialCards = listOf(
                createCard(deckId = deckId, id = 1),
                createCard(deckId = deckId, id = 2),
                createCard(deckId = deckId, id = 3),
                createCard(deckId = deckId, id = 4),
            )
            val deck = Deck(
                name = "Test deck",
                creationDate = 1L,
                cardQuantity = initialCards.size,
                id = deckId,
            )

            val stateStore = DeckReviewSavedStateHandleStateStore(SavedStateHandle()).apply {
                repetitionCards.value = initialCards
            }
            val coroutineContextProvider = TestCoroutineContextProvider(io = mainDispatcherRule.dispatcher)
            val deckRepository = FakeDeckRepository(initialDeck = deck)
            val cardRepository = FakeCardRepository(initialCards = initialCards)
            val repetitionInfoRepository = FakeDeckRepetitionInfoRepository()

            val viewModel = DeckReviewViewModel(
                deckId = deckId,
                stateStore = stateStore,
                fetchCards = FetchCardsUseCase(cardRepository = cardRepository),
                fetchDeckById = FetchDeckByIdUseCase(deckRepository = deckRepository),
                timer = RepetitionTimer(coroutineContextProvider = coroutineContextProvider),
                audioPlayer = FakeAudioPlayerManager(),
                updateDeck = UpdateDeckUseCase(
                    deckRepository = deckRepository,
                    localStorageSaveVersionRepository = FakeStorageSaveVersionRepository(),
                    localStorageTransactionRepository = FakeStorageTransactionRepository(),
                    coroutineContextProvider = coroutineContextProvider,
                ),
                deleteCardsFromDeck = DeleteCardsFromDeckUseCase(
                    deckRepository = deckRepository,
                    cardRepository = cardRepository,
                    localStorageSaveVersionRepository = FakeStorageSaveVersionRepository(),
                    localStorageTransactionRepository = FakeStorageTransactionRepository(),
                    coroutineContextProvider = coroutineContextProvider,
                ),
                deckReviewScheduler = FakeDeckReviewScheduler(),
                saveDeckReviewInfo = SaveDeckReviewInfoUseCase(
                    deckRepetitionInfoRepository = repetitionInfoRepository,
                    coroutineContextProvider = coroutineContextProvider,
                ),
                deckReviewNotifier = FakeDeckReviewNotifierManager(),
                crashlytics = FakeCrashlyticsRepository(),
                coroutineContextProvider = coroutineContextProvider,
            )

            runCurrent()
            viewModel.startRepeating()
            runCurrent()
            viewModel.pauseTimerCounting()
            runCurrent()

            repeat(times = 3) { iteration ->
                val editedFirstCard = initialCards[0].copy(
                    foreignWord = "foreign-1-edited-$iteration",
                )

                // This simulates an edit-return path where the source emits cards in
                // a different order, but the active review queue must remain unchanged.
                cardRepository.emitCards(
                    cards = listOf(
                        initialCards[1],
                        initialCards[3],
                        editedFirstCard,
                        initialCards[2],
                    )
                )
                runCurrent()

                assertEquals(
                    listOf(1, 2, 3, 4),
                    stateStore.repetitionCards.value.map { it.id }
                )
                assertEquals(
                    editedFirstCard.foreignWord,
                    stateStore.repetitionCards.value.first().foreignWord
                )
            }

            viewModel.moveCardByDifficultyRecallingLevel(level = EASY)
            runCurrent()
            viewModel.pauseTimerCounting()
            runCurrent()
            assertTrue(deckRepository.insertedDecks.isEmpty())

            viewModel.moveCardByDifficultyRecallingLevel(level = EASY)
            runCurrent()
            viewModel.pauseTimerCounting()
            runCurrent()

            assertTrue(
                "Repetition should still be active after only two answers out of four cards",
                deckRepository.insertedDecks.isEmpty()
            )
        }

    private fun createCard(deckId: Int, id: Int): Card {
        return Card(
            deckId = deckId,
            nativeWord = "native-$id",
            foreignWord = "foreign-$id",
            ipa = emptyList(),
            id = id,
        )
    }
}

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

private class FakeDeckReviewScheduler : IDeckReviewScheduler {

    override fun schedule(deckName: String, deckId: Int, atTime: Long) = Unit
}

private class FakeDeckReviewNotifierManager : IDeckReviewNotifierManager {

    override fun showNotification(deckName: String, deckId: Int) = Unit

    override fun showCommonNotification() = Unit

    override fun removeNotificationFromNotificationBar(deckId: Int) = Unit
}

private class FakeCrashlyticsRepository : ICrashlyticsRepository {

    override fun report(exception: Throwable) = Unit
}

private class FakeCardRepository(
    initialCards: List<Card>,
) : ICardRepository {

    private val cardsFlow = MutableStateFlow(initialCards)

    fun emitCards(cards: List<Card>) {
        cardsFlow.value = cards
    }

    override suspend fun fetchCardQuantityByDeckId(deckId: Int): Int {
        return cardsFlow.value.count { it.deckId == deckId }
    }

    override suspend fun fetchAllCards(): List<Card> {
        return cardsFlow.value
    }

    override suspend fun insertCard(card: Card) {
        cardsFlow.value = cardsFlow.value + card
    }

    override suspend fun insertCardAtPath(card: Card, rootEmailPath: String) {
        insertCard(card)
    }

    override fun fetchObservableCardById(cardId: Int): Flow<Card?> {
        return flowOf(cardsFlow.value.firstOrNull { it.id == cardId })
    }

    override fun fetchObservableCardsByDeckId(deckId: Int): Flow<List<Card>> {
        return cardsFlow
    }

    override suspend fun fetchCardsByDeckId(deckId: Int): List<Card> {
        return cardsFlow.value.filter { it.deckId == deckId }
    }

    override suspend fun deleteCard(cardId: Int) {
        cardsFlow.value = cardsFlow.value.filterNot { it.id == cardId }
    }

    override suspend fun removeCardsOfDeck(deckId: Int) {
        cardsFlow.value = cardsFlow.value.filterNot { it.deckId == deckId }
    }

    override suspend fun checkIfCardExists(foreignWord: String): List<Deck> {
        return emptyList()
    }
}

private class FakeDeckRepository(
    initialDeck: Deck,
) : IDeckRepository {

    private val deckFlow = MutableStateFlow<Deck?>(initialDeck)
    val insertedDecks = mutableListOf<Deck>()

    override fun fetchDeckSource(): Flow<List<Deck>> {
        return flowOf(deckFlow.value?.let(::listOf).orEmpty())
    }

    override suspend fun fetchAllDecks(): List<Deck> {
        return deckFlow.value?.let(::listOf).orEmpty()
    }

    override fun fetchObservableDeckById(deckId: Int): Flow<Deck?> {
        return deckFlow
    }

    override suspend fun insertDeck(deck: Deck) {
        insertedDecks += deck
        deckFlow.value = deck
    }

    override suspend fun insertDeckAtPath(deck: Deck, rootEmailPath: String) {
        insertDeck(deck)
    }

    override suspend fun removeDeck(deckId: Int) {
        if (deckFlow.value?.id == deckId) {
            deckFlow.value = null
        }
    }

    override suspend fun getDeckById(deckId: Int): Deck? {
        return deckFlow.value
    }

    override suspend fun getCardQuantityInDeck(deckId: Int): Int {
        return deckFlow.value?.cardQuantity ?: 0
    }
}

private class FakeStorageSaveVersionRepository : IStorageSaveVersionRepository {

    override suspend fun fetchVersion(): StorageSaveVersion? {
        return null
    }

    override suspend fun insertVersion(version: StorageSaveVersion) = Unit

    override suspend fun insertVersionAtPath(version: StorageSaveVersion, rootEmailPath: String) = Unit

    override suspend fun increaseVersion() = Unit
}

private class FakeStorageTransactionRepository : IStorageTransactionRepository {

    override suspend fun <R> performWithTransaction(block: suspend () -> R) {
        block()
    }
}

private class FakeDeckRepetitionInfoRepository : IDeckRepetitionInfoRepository {

    override fun fetchDeckRepetitionInfo(deckId: Int): Flow<DeckRepetitionInfo?> {
        return flowOf(null)
    }

    override suspend fun saveDeckRepetitionInfo(info: DeckRepetitionInfo) = Unit

    override suspend fun removeDeckRepetitionInfo(deckId: Int) = Unit
}
