package com.example.klaf.presentation.deckList.common

import androidx.work.WorkManager
import app.cash.turbine.test
import com.example.klaf.R
import com.example.klaf.common.MainDispatcherRule
import com.example.klaf.domain.entities.Deck
import com.example.klaf.domain.useCases.*
import com.example.klaf.presentation.common.NotificationChannelInitializer
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

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

        viewModel.eventMessage.test {
            val receivedMessageId = awaitItem().resId

            assertEquals(R.string.problem_with_fetching_decks, receivedMessageId)
        }
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

        val testJob = launch {
            viewModel.eventMessage.test {
                val messageId = awaitItem().resId

                assertEquals(R.string.such_deck_is_already_exist, messageId)
            }
        }

        viewModel.createNewDeck(deckName = newDeck.name)
        testJob.join()
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `get a warning message when the name of the new deck is empty`() = runTest {
        val viewModel = createViewModel()
        val emptyDeckName = "     "

        val testJob = launch {
            viewModel.eventMessage.test {
                val messageId = awaitItem().resId

                assertEquals(R.string.warning_deck_name_empty, messageId)
            }
        }

        viewModel.createNewDeck(deckName = emptyDeckName)
        testJob.join()
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `get a success message when a deck has been created`() = runTest {
        val viewModel = createViewModel()
        val deckName = "deck_name"

        val testJob = launch {
            viewModel.eventMessage.test {
                val messageId = awaitItem().resId

                assertEquals(R.string.deck_has_been_created, messageId)
            }
        }

        viewModel.createNewDeck(deckName = deckName)
        testJob.join()
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `get a problem message when creating deck is failed`() = runTest {
        val deckName = "deck_name"
        val createDeckUseCase: CreateDeckUseCase = mockk() {
            coEvery { this@mockk.invoke(any()) } throws Exception()
        }
        val viewModel = createViewModel(createDeck = createDeckUseCase)

        val testJob = launch {
            viewModel.eventMessage.test {
                val messageId = awaitItem().resId

                assertEquals(R.string.problem_with_creating_deck, messageId)
            }
        }

        viewModel.createNewDeck(deckName = deckName)
        coVerify { createDeckUseCase.invoke(deck = any()) }
        testJob.join()
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