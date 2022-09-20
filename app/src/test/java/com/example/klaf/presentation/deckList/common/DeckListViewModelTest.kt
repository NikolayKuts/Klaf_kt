package com.example.klaf.presentation.deckList.common

import androidx.annotation.StringRes
import androidx.work.WorkManager
import app.cash.turbine.test
import com.example.klaf.R
import com.example.klaf.common.MainDispatcherRule
import com.example.klaf.domain.entities.Deck
import com.example.klaf.domain.useCases.*
import com.example.klaf.presentation.common.NotificationChannelInitializer
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.math.exp

class DeckListViewModelTest {

    @ExperimentalCoroutinesApi
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Before
    fun setUp() {
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `getting problem message when getting decks is failed`() = runTest() {
        val fetchDeckSourceUseCase: FetchDeckSourceUseCase = mockk() {
            every { this@mockk.invoke() } returns flow { throw Exception() }
        }
        val viewModel = createViewModel(fetchDeckSource = fetchDeckSourceUseCase)

        verify(exactly = 1) { fetchDeckSourceUseCase.invoke() }

        viewModel.testEventMassageIdEquals(expectedMassageId = R.string.problem_with_fetching_decks)
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `getting deck list from observable deck source`() = runTest {
        val deckList = listOf(
            Deck(name = "first", creationDate = 100000L),
            Deck(name = "second", creationDate = 20000L)
        )
        val fetchDeckSourceUseCase: FetchDeckSourceUseCase = mockk() {
            every { this@mockk.invoke() } returns flow { emit(deckList) }
        }

        val viewModel = createViewModel(fetchDeckSource = fetchDeckSourceUseCase)
        verify(exactly = 1) { fetchDeckSourceUseCase.invoke() }

        viewModel.deckSource.test() {
            assertEquals(deckList, awaitItem())
        }
    }

    @Test
    fun `initialize notification channels`() {
        val channelInitializer: NotificationChannelInitializer = mockk(relaxed = true)

        createViewModel(notificationChannelInitializer = channelInitializer)
        verify(exactly = 1) { channelInitializer.initialize() }
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `get a message that a deck with that name has already been created`() = runTest {
        val newDeck = Deck(name = "some_name", creationDate = 10000L)
        val viewModel = createViewModel(
            fetchDeckSource = mockk() {
                every { this@mockk.invoke() } returns flow { emit(listOf(newDeck)) }
            }
        )

        val testJob = launchEventMassageIdEqualsTest(
            viewModel = viewModel,
            expectedMassageId = R.string.such_deck_is_already_exist,
        )

        viewModel.createNewDeck(deckName = newDeck.name)
        testJob.join()
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `get a warning message when the name of the new deck is empty`() = runTest {
        val viewModel = createViewModel()
        val emptyDeckName = "     "

        val testJob = launchEventMassageIdEqualsTest(
            viewModel = viewModel,
            expectedMassageId = R.string.warning_deck_name_empty,
        )

        viewModel.createNewDeck(deckName = emptyDeckName)
        testJob.join()
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `get a success message when a deck has been created`() = runTest {
        val createDeckUseCase: CreateDeckUseCase = mockk(relaxed = true)
        val viewModel = createViewModel(createDeck = createDeckUseCase)
        val deckName = "deck_name"

        val testJob = launchEventMassageIdEqualsTest(
            viewModel = viewModel,
            expectedMassageId = R.string.deck_has_been_created,
        )

        viewModel.createNewDeck(deckName = deckName)
        coVerify(exactly = 1) { createDeckUseCase.invoke(deck = any()) }
        testJob.join()
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `get a warning message when creating deck is failed`() = runTest {
        val deckName = "deck_name"
        val createDeckUseCase: CreateDeckUseCase = mockk() {
            coEvery { this@mockk.invoke(any()) } throws Exception()
        }
        val viewModel = createViewModel(createDeck = createDeckUseCase)

        val testJob = launchEventMassageIdEqualsTest(
            viewModel = viewModel,
            expectedMassageId = R.string.problem_with_creating_deck
        )

        viewModel.createNewDeck(deckName = deckName)
        coVerify(exactly = 1) { createDeckUseCase.invoke(deck = any()) }
        testJob.join()
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `get a warning massage when deck name for deck renaming is empty`() = runTest {
        val viewModel = createViewModel()
        val oldDeck = Deck(name = "old_name", creationDate = 19999)
        val newEmptyName = "    "

        val testJob = launchEventMassageIdEqualsTest(
            viewModel = viewModel,
            expectedMassageId = R.string.type_new_deck_name
        )

        viewModel.renameDeck(deck = oldDeck, newName = newEmptyName)
        testJob.join()
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `get a massage while deck renaming when the deck name is not changed`() = runTest {
        val viewModel = createViewModel()
        val oldDeck = Deck(name = "old_name", creationDate = 19999)

        val testJob = launchEventMassageIdEqualsTest(
            viewModel = viewModel,
            expectedMassageId = R.string.deck_name_is_not_changed
        )

        viewModel.renameDeck(deck = oldDeck, newName = oldDeck.name)
        testJob.join()
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `get a massage while deck renaming that a deck with such name already exists`() = runTest {
        val existingDeckName = "existed_deck_name"
        val existingDeck = Deck(name = existingDeckName, creationDate = 22222222)
        val oldDeck = Deck(name = "old_name", creationDate = 1111111)
        val fetchDeckSourceUseCase: FetchDeckSourceUseCase = mockk() {
            every { this@mockk.invoke() } returns flow { emit(listOf(existingDeck)) }
        }
        val viewModel = createViewModel(fetchDeckSource = fetchDeckSourceUseCase)

        val testJob = launchEventMassageIdEqualsTest(
            viewModel = viewModel,
            expectedMassageId = R.string.such_deck_is_already_exist
        )

        viewModel.renameDeck(deck = oldDeck, newName = existingDeckName)
        testJob.join()
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `get a success message when a deck has been renamed`() = runTest {
        val oldDeck = Deck(name = "old_name", creationDate = 1111111)
        val newDeckName = "new_deck_name"
        val viewModel = createViewModel()

        val testJob = launchEventMassageIdEqualsTest(
            viewModel = viewModel,
            expectedMassageId = R.string.deck_has_been_renamed
        )

        viewModel.renameDeck(deck = oldDeck, newName = newDeckName)
        testJob.join()
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `get a message when deck renaming is failed`() = runTest {
        val oldDeck = Deck(name = "old_name", creationDate = 1111111)
        val newDeckName = "new_deck_name"
        val renameDeckUseCase: RenameDeckUseCase = mockk() {
            coEvery {
                this@mockk.invoke(oldDeck = oldDeck, name = newDeckName)
            } throws Exception()
        }
        val viewModel = createViewModel(renameDeck = renameDeckUseCase)

        val testJob = launchEventMassageIdEqualsTest(
            viewModel = viewModel,
            expectedMassageId = R.string.problem_with_renaming_deck
        )

        viewModel.renameDeck(deck = oldDeck, newName = newDeckName)
        testJob.join()
    }

    @ExperimentalCoroutinesApi
    private fun TestScope.launchEventMassageIdEqualsTest(
        viewModel: DeckListViewModel,
        @StringRes expectedMassageId: Int
    ): Job {
        return launch {
            viewModel.testEventMassageIdEquals(expectedMassageId = expectedMassageId)
        }
    }

    private suspend fun DeckListViewModel.testEventMassageIdEquals(
        @StringRes expectedMassageId: Int
    ) {
        this.eventMessage.test {
            val receivedMessageId = awaitItem().resId

            assertEquals(expectedMassageId, receivedMessageId)
        }
    }

    private fun createViewModel(
        fetchDeckSource: FetchDeckSourceUseCase = mockk() {
            every { this@mockk.invoke() } returns flow { emit(emptyList()) }
        },
        createDeck: CreateDeckUseCase = mockk(relaxed = true),
        renameDeck: RenameDeckUseCase = mockk(relaxed = true),
        removeDeck: RemoveDeckUseCase = mockk(relaxed = true),
        deleteAllCardsOfDeck: DeleteAllCardsOfDeck = mockk(relaxed = true),
        createInterimDeck: CreateInterimDeckUseCase = mockk(relaxed = true),
        notificationChannelInitializer: NotificationChannelInitializer = mockk(relaxed = true),
        workManager: WorkManager = mockk(relaxed = true),
    ): DeckListViewModel {
        return DeckListViewModel(
            fetchDeckSource = fetchDeckSource,
            createDeck = createDeck,
            renameDeck = renameDeck,
            removeDeck = removeDeck,
            deleteAllCardsOfDeck = deleteAllCardsOfDeck,
            createInterimDeck = createInterimDeck,
            notificationChannelInitializer = notificationChannelInitializer,
            workManager = workManager
        )
    }
}