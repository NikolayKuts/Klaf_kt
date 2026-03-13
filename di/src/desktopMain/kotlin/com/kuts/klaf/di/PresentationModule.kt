package com.kuts.klaf.di

import com.kuts.domain.common.CardRepetitionOrder
import com.kuts.domain.common.CardSide
import com.kuts.domain.common.ICoroutineContextProvider
import com.kuts.domain.common.LoadingState
import com.kuts.domain.common.UnitSurrogate
import com.kuts.domain.entities.Card
import com.kuts.domain.managers.IDeckReviewNotifierManager
import com.kuts.klaf.authentication.AuthenticationViewModel
import com.kuts.klaf.authentication.BaseAuthenticationViewModel
import com.kuts.klaf.cardManagement.cardAddition.CardAdditionViewModel
import com.kuts.klaf.cardManagement.cardEditing.CardEditingViewModel
import com.kuts.klaf.cardManagement.common.CambridgeWordData
import com.kuts.klaf.cardManagement.common.ICambridgeWordDataProvider
import com.kuts.klaf.cardTransferring.common.BaseCardTransferringViewModel
import com.kuts.klaf.cardTransferring.common.CardTransferringViewModel
import com.kuts.klaf.cardViewing.CardViewingViewModel
import com.kuts.klaf.common.ButtonState
import com.kuts.klaf.common.RepetitionTimer
import com.kuts.klaf.deckList.common.BaseDeckListViewModel
import com.kuts.klaf.deckList.common.DeckListViewModel
import com.kuts.klaf.deckManagment.BaseDeckManagementViewModel
import com.kuts.klaf.deckManagment.DeckManagementViewModel
import com.kuts.klaf.deckRepetition.BaseDeckReviewViewModel
import com.kuts.klaf.deckRepetition.DeckReviewState
import com.kuts.klaf.deckRepetition.IDeckReviewStateStore
import com.kuts.klaf.deckRepetition.DeckReviewViewModel
import com.kuts.klaf.deckRepetition.RepetitionScreenState
import com.kuts.klaf.deckRepetition.RepetitionScreenState.StartState
import com.kuts.klaf.deckRepetitionInfo.DeckRepetitionInfoViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

internal val presentationModule = module {
    factory { RepetitionTimer(coroutineContextProvider = get()) }
    single<IDeckReviewNotifierManager> { NoOpDeckReviewNotifier() }
    single<ICambridgeWordDataProvider> { NoOpCambridgeWordDataProvider() }
    viewModels()
}

private fun Module.viewModels() {
    viewModel<BaseAuthenticationViewModel> {
        AuthenticationViewModel(
            authenticationInteractor = get(),
            coroutineContextProvider = get<ICoroutineContextProvider>(),
        )
    }

    viewModel<BaseDeckListViewModel> {
        DeckListViewModel(
            fetchDeckSource = get(),
            createInterimDeck = get(),
            createDeck = get(),
            renameDeck = get(),
            removeDeck = get(),
            fetchCardsUseCase = get(),
            authenticationSessionManager = get(),
            crashlytics = get(),
            appMaintenanceManager = get(),
            authenticationInteractor = get(),
            coroutineContextProvider = get(),
        )
    }

    viewModel<BaseDeckReviewViewModel> { params ->
        DeckReviewViewModel(
            deckId = params.get(),
            stateStore = InMemoryDeckReviewStateStore(),
            fetchCards = get(),
            fetchDeckById = get(),
            timer = get(),
            audioPlayer = get(),
            updateDeck = get(),
            deleteCardsFromDeck = get(),
            deckReviewScheduler = get(),
            saveDeckReviewInfo = get(),
            deckReviewNotifier = get(),
            crashlytics = get(),
            coroutineContextProvider = get(),
        )
    }

    viewModel<BaseDeckManagementViewModel> { params ->
        DeckManagementViewModel(
            deckId = params.get(),
            fetchDeckById = get(),
            updateDeck = get(),
            crashlytics = get(),
            coroutineContextProvider = get(),
        )
    }

    viewModel { params ->
        CardAdditionViewModel(
            deckId = params.get(),
            smartSelectedWord = params.getOrNull(),
            addNewCardIntoDeck = get(),
            checkIfWordExists = get(),
            audioPlayer = get(),
            cambridgeWordDataProvider = get(),
            fetchWordAutocomplete = get(),
            fetchWordInfo = get(),
            crashlytics = get(),
            fetchDeckById = get(),
            coroutineContextProvider = get(),
        )
    }

    viewModel { params ->
        CardEditingViewModel(
            deckId = params.get(),
            cardId = params.get(),
            fetchCard = get(),
            updateCard = get(),
            fetchWordMeaningInsights = get(),
            checkIfWordExists = get(),
            audioPlayer = get(),
            cambridgeWordDataProvider = get(),
            fetchWordAutocomplete = get(),
            fetchWordInfo = get(),
            crashlytics = get(),
            fetchDeckById = get(),
            coroutineContextProvider = get(),
        )
    }

    viewModel<BaseCardTransferringViewModel> { params ->
        CardTransferringViewModel(
            sourceDeckId = params.get(),
            fetchDeckById = get(),
            fetchCards = get(),
            deleteCardsFromDeckUseCase = get(),
            fetchDeckSource = get(),
            audioPlayer = get(),
            moveCardsToDeck = get(),
            crashlytics = get(),
            coroutineContextProvider = get(),
        )
    }

    viewModel { params ->
        CardViewingViewModel(
            deckId = params.get(),
            fetchDeckById = get(),
            fetchCards = get(),
            crashlytics = get(),
        )
    }

    viewModel { params ->
        DeckRepetitionInfoViewModel(
            deckId = params.get(),
            fetchDeckRepetitionInfo = get(),
            crashlytics = get(),
        )
    }
}

private class NoOpDeckReviewNotifier : IDeckReviewNotifierManager {
    override fun showNotification(deckName: String, deckId: Int) = Unit
    override fun showCommonNotification() = Unit
    override fun removeNotificationFromNotificationBar(deckId: Int) = Unit
}

private class NoOpCambridgeWordDataProvider : ICambridgeWordDataProvider {
    override suspend fun fetchWordData(word: String): CambridgeWordData? = null
}

private class InMemoryDeckReviewStateStore : IDeckReviewStateStore {
    override val mainButtonState = MutableStateFlow(ButtonState.UNPRESSED)
    override val screenState = MutableSharedFlow<RepetitionScreenState>(replay = 1).apply {
        tryEmit(StartState)
    }
    override val cardDeletingState = MutableStateFlow<LoadingState<UnitSurrogate, UnitSurrogate>>(
        value = LoadingState.Non
    )
    override val repetitionCards = MutableStateFlow<List<Card>>(emptyList())
    override val cardSide = MutableStateFlow(CardSide.FRONT)
    override val repetitionOrder = MutableStateFlow(CardRepetitionOrder.NATIVE_TO_FOREIGN)
    override val reviewedCardIds = MutableStateFlow<Set<Int>>(emptySet())
    override var savedTime: Long = 0L
    override val deckReviewState = MutableStateFlow(DeckReviewState())
    override var startRepetitionCard: Card? = null
    override var lastRepetitionCard: Card? = null
    override var isAllCardsRepeated: Boolean = false
    override var isWaitingForFinish: Boolean = false
    override val savedProgressCards = MutableStateFlow<List<Card>>(emptyList())
    override var timerTime: Long = 0L
}
