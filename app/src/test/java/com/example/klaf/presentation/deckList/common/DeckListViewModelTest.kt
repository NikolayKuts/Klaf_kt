package com.example.klaf.presentation.deckList.common

import androidx.work.WorkManager
import app.cash.turbine.test
import com.example.klaf.common.MainDispatcherRule
import com.example.klaf.domain.entities.Deck
import com.example.klaf.domain.useCases.*
import com.example.klaf.presentation.deckList.dataSynchronization.DataSynchronizationNotifier
import com.example.klaf.presentation.deckRepetition.DeckRepetitionNotifier
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import com.example.klaf.R
import io.mockk.verify

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
        val firstDeck = Deck(name = "first", creationDate = 100000L)
        val secondDeck = Deck(name = "second", creationDate = 20000L)
        val deckList = listOf(firstDeck, secondDeck)

        val viewModel = createViewModel(
            fetchDeckSource = mockk() {
                every { this@mockk.invoke() } returns flow { emit(deckList) }
            }
        )

        viewModel.deckSource.test() {
            assertEquals(deckList, awaitItem())
        }
    }

    private fun createViewModel(
        fetchDeckSource: FetchDeckSourceUseCase = mockk() {
            every { this@mockk.invoke() } returns flow { emit(emptyList()) }
        },
        createDeck: CreateDeckUseCase = mockk(),
        renameDeck: RenameDeckUseCase = mockk(),
        removeDeck: RemoveDeckUseCase = mockk(),
        deleteAllCardsOfDeck: DeleteAllCardsOfDeck = mockk(),
        createInterimDeck: CreateInterimDeckUseCase = mockk(relaxed = true),
        deckRepetitionNotifier: DeckRepetitionNotifier = mockk(relaxed = true),
        dataSynchronizationNotifier: DataSynchronizationNotifier = mockk(relaxed = true),
        workManager: WorkManager = mockk(relaxed = true),
    ): DeckListViewModel {
        return DeckListViewModel(
            fetchDeckSource = fetchDeckSource,
            createDeck = createDeck,
            renameDeck = renameDeck,
            removeDeck = removeDeck,
            deleteAllCardsOfDeck = deleteAllCardsOfDeck,
            createInterimDeck = createInterimDeck,
            deckRepetitionNotifier = deckRepetitionNotifier,
            dataSynchronizationNotifier = dataSynchronizationNotifier,
            workManager = workManager
        )
    }
}