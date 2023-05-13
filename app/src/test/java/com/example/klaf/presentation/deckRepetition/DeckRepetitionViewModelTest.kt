package com.example.klaf.presentation.deckRepetition

import androidx.work.WorkManager
import app.cash.turbine.test
import com.example.klaf.R
import com.example.klaf.common.MainDispatcherRule
import com.example.klaf.common.launchEventMassageIdEqualsTest
import com.example.klaf.data.common.notifications.DeckRepetitionNotifier
import com.example.klaf.data.networking.CardAudioPlayer
import com.example.domain.entities.Card
import com.example.domain.entities.Deck
import com.example.domain.enums.DifficultyRecallingLevel
import com.example.domain.repositories.CrashlyticsRepository
import com.example.domain.useCases.*
import com.example.klaf.data.firestore.repositoryImplementations.CrashlyticsRepositoryFirebaseImp
import com.example.klaf.presentation.common.RepetitionTimer
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class DeckRepetitionViewModelTest {

    @ExperimentalCoroutinesApi
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @ExperimentalCoroutinesApi
    @Test
    fun `getting problem message when getting deck is failed`() = runTest {
        val deckId = 111111
        val fetchDeckByIdUseCase: FetchDeckByIdUseCase = mockk {
            coEvery { this@mockk.invoke(deckId = deckId) } returns flow { throw Exception() }
        }
        val viewModel = createViewModel(
            deckId = deckId,
            fetchDeckById = fetchDeckByIdUseCase
        )

        verify(exactly = 1) { fetchDeckByIdUseCase.invoke(deckId = deckId) }
        launchEventMassageIdEqualsTest(
            eventMessageSource = viewModel,
            expectedMassageId = R.string.problem_with_fetching_deck
        )
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `getting problem message when fetching cards is failed`() = runTest {
        val deckId = 111111
        val fetchCardsUseCase: FetchCardsUseCase = mockk {
            every { invoke(deckId = deckId) } returns flow { throw Exception() }
        }
        val viewModel = createViewModel(
            deckId = deckId,
            fetchCards = fetchCardsUseCase
        )

        verify(exactly = 1) { fetchCardsUseCase.invoke(deckId = deckId) }
        launchEventMassageIdEqualsTest(
            eventMessageSource = viewModel,
            expectedMassageId = R.string.problem_with_fetching_cards
        )
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `get problem message when repetition cards is empty on start repeating`() = runTest {
        val deckId = 111111
        val fetchCardsUseCase: FetchCardsUseCase = mockk {
            every { invoke(deckId = deckId) } returns flow { emptyList<Card>() }
        }
        val viewModel = createViewModel(
            deckId = deckId,
            fetchCards = fetchCardsUseCase
        )
        val testJob = launchEventMassageIdEqualsTest(
            eventMessageSource = viewModel,
            expectedMassageId = R.string.problem_with_fetching_cards
        )

        coVerify { fetchCardsUseCase(deckId = deckId) }
        viewModel.startRepeating()
        testJob.join()
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `get problem message when repetition cards is empty on moveCardByDifficultyRecallingLevel`() =
        runTest {
            val deckId = 111111
            val fetchCardsUseCase: FetchCardsUseCase = mockk {
                every { invoke(deckId = deckId) } returns flow { emptyList<Card>() }
            }
            val viewModel = createViewModel(
                deckId = deckId,
                fetchCards = fetchCardsUseCase
            )
            val testJob = launchEventMassageIdEqualsTest(
                eventMessageSource = viewModel,
                expectedMassageId = R.string.problem_with_fetching_cards
            )

            coVerify { fetchCardsUseCase(deckId = deckId) }
            viewModel.moveCardByDifficultyRecallingLevel(level = DifficultyRecallingLevel.EASY)
            testJob.join()
        }

    @ExperimentalCoroutinesApi
    @Test
    fun `get problem message if deleting card is failed`() = runTest {
        val cardId = 222222
        val deckId = 999999
        val deleteCardsFromDeckUseCase: DeleteCardsFromDeckUseCase = mockk {
            coEvery {
                this@mockk.invoke(deckId = deckId, cardIds = intArrayOf(cardId))
            } throws Exception()
        }
        val viewModel = createViewModel(deleteCardFromDeck = deleteCardsFromDeckUseCase)
        val testJob = launchEventMassageIdEqualsTest(
            eventMessageSource = viewModel,
            expectedMassageId = R.string.problem_with_removing_card
        )

        viewModel.deleteCard(cardId = cardId, deckId = deckId)
        coVerify(exactly = 1) {
            deleteCardsFromDeckUseCase(deckId = deckId, cardIds = intArrayOf(cardId))
        }
        testJob.join()
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `getting deck from observable deck source`() = runTest {
        val deckId = 3333333
        val expectedDeck = Deck(name = "first", creationDate = 100000L, id = deckId)
        val fetchDeckByIdUseCase: FetchDeckByIdUseCase = mockk {
            coEvery { this@mockk.invoke(deckId = deckId) } returns flow { emit(expectedDeck) }
        }
        val viewModel = createViewModel(
            deckId = deckId,
            fetchDeckById = fetchDeckByIdUseCase
        )

        verify(exactly = 1) { fetchDeckByIdUseCase.invoke(deckId = deckId) }
        viewModel.deck.test {
            Assert.assertEquals(expectedDeck, awaitItem())
        }
    }

    private fun createViewModel(
        deckId: Int = mockk(relaxed = true),
        fetchCards: FetchCardsUseCase = mockk(relaxed = true),
        fetchDeckById: FetchDeckByIdUseCase = mockk(relaxed = true),
        timer: RepetitionTimer = mockk(relaxed = true),
        audioPlayer: CardAudioPlayer = mockk(relaxed = true),
        updateDeck: UpdateDeckUseCase = mockk(relaxed = true),
        deleteCardFromDeck: DeleteCardsFromDeckUseCase = mockk(relaxed = true),
        workManager: WorkManager = mockk(relaxed = true),
        saveDeckRepetitionInfo: SaveDeckRepetitionInfoUseCase = mockk(relaxed = true),
        deckRepetitionNotifier: DeckRepetitionNotifier = mockk(relaxed = true),
        crashlyticsRepository: CrashlyticsRepository = mockk(relaxed = true)
    ): BaseDeckRepetitionViewModel = DeckRepetitionViewModel(
        deckId = deckId,
        fetchCards = fetchCards,
        fetchDeckById = fetchDeckById,
        timer = timer,
        audioPlayer = audioPlayer,
        updateDeck = updateDeck,
        deleteCardsFromDeck = deleteCardFromDeck,
        workManager = workManager,
        saveDeckRepetitionInfo = saveDeckRepetitionInfo,
        deckRepetitionNotifier = deckRepetitionNotifier,
        crashlytics = crashlyticsRepository,
    )
}