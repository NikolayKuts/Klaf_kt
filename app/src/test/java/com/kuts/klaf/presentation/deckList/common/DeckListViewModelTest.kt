package com.kuts.klaf.presentation.deckList.common

import androidx.work.WorkManager
import app.cash.turbine.test
import com.google.firebase.auth.FirebaseAuth
import com.kuts.domain.entities.Deck
import com.kuts.domain.interactors.AuthenticationInteractor
import com.kuts.domain.repositories.CrashlyticsRepository
import com.kuts.domain.useCases.CreateDeckUseCase
import com.kuts.domain.useCases.CreateInterimDeckUseCase
import com.kuts.domain.useCases.FetchDeckSourceUseCase
import com.kuts.domain.useCases.RemoveDeckUseCase
import com.kuts.domain.useCases.RenameDeckUseCase
import com.kuts.klaf.R
import com.kuts.klaf.common.MainDispatcherRule
import com.kuts.klaf.common.launchEventMassageIdEqualsTest
import com.kuts.klaf.data.common.NetworkConnectivity
import com.kuts.klaf.data.common.notifications.NotificationChannelInitializer
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DeckListViewModelTest {

    @ExperimentalCoroutinesApi
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @ExperimentalCoroutinesApi
    @Test
    fun `get null when getting fetching is failed`() = runTest {
        val fetchDeckSourceUseCase: FetchDeckSourceUseCase = mockk {
            every { this@mockk.invoke() } returns flow { throw Exception() }
        }
        val viewModel = createViewModel(fetchDeckSource = fetchDeckSourceUseCase)

        verify(exactly = 1) { fetchDeckSourceUseCase.invoke() }
        assertEquals(null, viewModel.deckSource.value)
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `get empty list by default when fetching decks`() = runTest {
        val fetchDeckSourceUseCase: FetchDeckSourceUseCase = mockk {
            every { this@mockk.invoke() } returns flow { }
        }
        val viewModel = createViewModel(fetchDeckSource = fetchDeckSourceUseCase)
        assertEquals(true, viewModel.deckSource.value?.isEmpty())
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `getting deck list from observable deck source`() = runTest {
        val deckList = listOf(
            Deck(name = "first", creationDate = 100000L),
            Deck(name = "second", creationDate = 20000L)
        )
        val fetchDeckSourceUseCase: FetchDeckSourceUseCase = mockk {
            every { this@mockk.invoke() } returns flow { emit(deckList) }
        }
        val viewModel = createViewModel(fetchDeckSource = fetchDeckSourceUseCase)

        verify(exactly = 1) { fetchDeckSourceUseCase.invoke() }
        viewModel.deckSource.test {
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
            fetchDeckSource = mockk {
                every { this@mockk.invoke() } returns flow { emit(listOf(newDeck)) }
            }
        )
        val testJob = launchEventMassageIdEqualsTest(
            eventMessageSource = viewModel,
            expectedMassageId = R.string.such_deck_is_already_exist,
        )

        delay(100)
        viewModel.createNewDeck(deckName = newDeck.name)
        testJob.join()
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `get a warning message when the name of the new deck is empty`() = runTest {
        val viewModel = createViewModel()
        val emptyDeckName = "     "
        val testJob = launchEventMassageIdEqualsTest(
            eventMessageSource = viewModel,
            expectedMassageId = R.string.warning_deck_name_empty,
        )

        delay(100)
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
            eventMessageSource = viewModel,
            expectedMassageId = R.string.deck_has_been_created,
        )

        delay(100)
        viewModel.createNewDeck(deckName = deckName)
        coVerify(exactly = 1) { createDeckUseCase.invoke(deck = any()) }
        testJob.join()
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `get a warning message when creating deck is failed`() = runTest {
        val deckName = "deck_name"
        val createDeckUseCase: CreateDeckUseCase = mockk {
            coEvery { this@mockk.invoke(any()) } throws Exception()
        }
        val viewModel = createViewModel(createDeck = createDeckUseCase)
        val testJob = launchEventMassageIdEqualsTest(
            eventMessageSource = viewModel,
            expectedMassageId = R.string.problem_with_creating_deck
        )

        delay(100)
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
            eventMessageSource = viewModel,
            expectedMassageId = R.string.type_deck_name
        )

        delay(100)
        viewModel.renameDeck(deck = oldDeck, newName = newEmptyName)
        testJob.join()
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `get a massage while deck renaming when the deck name is not changed`() = runTest {
        val viewModel = createViewModel()
        val oldDeck = Deck(name = "old_name", creationDate = 19999)
        val testJob = launchEventMassageIdEqualsTest(
            eventMessageSource = viewModel,
            expectedMassageId = R.string.deck_name_is_not_changed
        )

        delay(100)
        viewModel.renameDeck(deck = oldDeck, newName = oldDeck.name)
        testJob.join()
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `get a massage while deck renaming that a deck with such name already exists`() = runTest {
        val existingDeckName = "existed_deck_name"
        val existingDeck = Deck(name = existingDeckName, creationDate = 22222222)
        val oldDeck = Deck(name = "old_name", creationDate = 1111111)
        val fetchDeckSourceUseCase: FetchDeckSourceUseCase = mockk {
            every { this@mockk.invoke() } returns flow { emit(listOf(existingDeck)) }
        }
        val viewModel = createViewModel(fetchDeckSource = fetchDeckSourceUseCase)
        val testJob = launchEventMassageIdEqualsTest(
            eventMessageSource = viewModel,
            expectedMassageId = R.string.such_deck_is_already_exist
        )

        delay(100)
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
            eventMessageSource = viewModel,
            expectedMassageId = R.string.deck_has_been_renamed
        )

        delay(100)
        viewModel.renameDeck(deck = oldDeck, newName = newDeckName)
        testJob.join()
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `get a message when deck renaming is failed`() = runTest {
        val oldDeck = Deck(name = "old_name", creationDate = 1111111)
        val newDeckName = "new_deck_name"
        val renameDeckUseCase: RenameDeckUseCase = mockk {
            coEvery {
                this@mockk.invoke(oldDeck = oldDeck, name = newDeckName)
            } throws Exception()
        }
        val viewModel = createViewModel(renameDeck = renameDeckUseCase)
        val testJob = launchEventMassageIdEqualsTest(
            eventMessageSource = viewModel,
            expectedMassageId = R.string.problem_with_renaming_deck
        )

        delay(100)
        viewModel.renameDeck(deck = oldDeck, newName = newDeckName)
        testJob.join()
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `get a success message when the deck has been deleted`() = runTest {
        val deckId = 11111
        val viewModel = createViewModel()

        launchEventMassageIdEqualsTest(
            eventMessageSource = viewModel,
            expectedMassageId = R.string.the_deck_has_been_removed
        )

        delay(100)
        viewModel.deleteDeck(deckId = deckId)
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `get a message when deck deleting is failed`() = runTest {
        val deckId = 11111
        val removeDeckUseCase: RemoveDeckUseCase = mockk {
            coEvery { this@mockk.invoke(deckId = any()) } throws Exception()
        }
        val viewModel = createViewModel(removeDeck = removeDeckUseCase)

        launchEventMassageIdEqualsTest(
            eventMessageSource = viewModel,
            expectedMassageId = R.string.problem_with_removing_deck
        )

        delay(100)
        viewModel.deleteDeck(deckId = deckId)
    }


    private fun createViewModel(
        fetchDeckSource: FetchDeckSourceUseCase = mockk {
            every { this@mockk.invoke() } returns flow { emit(emptyList()) }
        },
        createDeck: CreateDeckUseCase = mockk(relaxed = true),
        renameDeck: RenameDeckUseCase = mockk(relaxed = true),
        removeDeck: RemoveDeckUseCase = mockk(relaxed = true),
        createInterimDeck: CreateInterimDeckUseCase = mockk(relaxed = true),
        notificationChannelInitializer: NotificationChannelInitializer = mockk(relaxed = true),
        workManager: WorkManager = mockk(relaxed = true),
        auth: FirebaseAuth = mockk(relaxed = true),
        crashlyticsRepository: CrashlyticsRepository = mockk(relaxed = true),
        networkConnectivity: NetworkConnectivity = mockk(relaxed = true) ,
        authenticationInteractor: AuthenticationInteractor = mockk(relaxed = true)
    ): BaseDeckListViewModel {
        return DeckListViewModel(
            fetchDeckSource = fetchDeckSource,
            createDeck = createDeck,
            renameDeck = renameDeck,
            removeDeck = removeDeck,
            createInterimDeck = createInterimDeck,
            notificationChannelInitializer = notificationChannelInitializer,
            workManager = workManager,
            auth = auth,
            crashlytics = crashlyticsRepository,
            networkConnectivity = networkConnectivity,
            authenticationInteractor = authenticationInteractor
        )
    }
}