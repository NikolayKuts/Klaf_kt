package com.example.klaf.presentation.deckRepetition

import androidx.annotation.StringRes
import androidx.work.WorkManager
import app.cash.turbine.test
import com.example.klaf.R
import com.example.klaf.common.MainDispatcherRule
import com.example.klaf.common.launchEventMassageIdEqualsTest
import com.example.klaf.data.networking.CardAudioPlayer
import com.example.klaf.domain.useCases.*
import com.example.klaf.presentation.common.RepetitionTimer
import com.example.klaf.presentation.deckList.common.DeckListViewModel
import dagger.assisted.Assisted
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class DeckRepetitionViewModelTest {

    @ExperimentalCoroutinesApi
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @ExperimentalCoroutinesApi
    @Test
    fun `getting problem message when getting deck is failed`() = runTest() {
        val deckId = 111111
        val fetchDeckByIdUseCase: FetchDeckByIdUseCase = mockk() {
            coEvery { this@mockk.invoke(deckId = deckId) } returns flow { throw Exception() }
        }
        val viewModel = createViewModel(
            deckId = deckId,
            fetchDeckById = fetchDeckByIdUseCase
        )

        verify(exactly = 1) { fetchDeckByIdUseCase.invoke(deckId = deckId) }

        launchEventMassageIdEqualsTest(
            viewModel = viewModel,
            expectedMassageId = R.string.problem_with_fetching_deck
        )
    }


    private fun createViewModel(
        deckId: Int = mockk(relaxed = true),
        fetchCards: FetchCardsUseCase = mockk(relaxed = true),
        fetchDeckById: FetchDeckByIdUseCase = mockk(relaxed = true),
        timer: RepetitionTimer = mockk(relaxed = true),
        audioPlayer: CardAudioPlayer = mockk(relaxed = true),
        updateDeck: UpdateDeckUseCase = mockk(relaxed = true),
        deleteCardFromDeck: DeleteCardFromDeckUseCase = mockk(relaxed = true),
        workManager: WorkManager = mockk(relaxed = true),
    ): BaseDeckRepetitionViewModel = DeckRepetitionViewModel(
        deckId = deckId,
        fetchCards = fetchCards,
        fetchDeckById = fetchDeckById,
        timer = timer,
        audioPlayer = audioPlayer,
        updateDeck = updateDeck,
        deleteCardFromDeck = deleteCardFromDeck,
        workManager = workManager
    )
}