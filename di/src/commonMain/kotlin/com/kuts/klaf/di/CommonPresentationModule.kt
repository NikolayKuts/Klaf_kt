package com.kuts.klaf.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.kuts.domain.common.ICoroutineContextProvider
import com.kuts.klaf.authentication.AuthenticationViewModel
import com.kuts.klaf.authentication.BaseAuthenticationViewModel
import com.kuts.klaf.cardManagement.cardAddition.CardAdditionViewModel
import com.kuts.klaf.cardManagement.cardEditing.CardEditingViewModel
import com.kuts.klaf.cardTransferring.common.BaseCardTransferringViewModel
import com.kuts.klaf.cardTransferring.common.CardTransferringViewModel
import com.kuts.klaf.cardViewing.CardViewingViewModel
import com.kuts.klaf.common.RepetitionTimer
import com.kuts.klaf.common.localStore.AppLocalStore
import com.kuts.klaf.deckList.common.BaseDeckListViewModel
import com.kuts.klaf.deckList.common.DeckListViewModel
import com.kuts.klaf.deckManagment.BaseDeckManagementViewModel
import com.kuts.klaf.deckManagment.DeckManagementViewModel
import com.kuts.klaf.deckRepetition.BaseDeckReviewViewModel
import com.kuts.klaf.deckRepetition.DeckReviewViewModel
import com.kuts.klaf.deckRepetitionInfo.DeckRepetitionInfoViewModel
import com.kuts.klaf.webContent.WebContentViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

internal val commonPresentationModule = module {
    single<DataStore<Preferences>>(qualifier = named(name = APP_PREFERENCES_DATA_STORE)) {
        get<IAppPreferencesDataStoreFactory>().create()
    }
    single {
        AppLocalStore(
            dataStore = get(qualifier = named(name = APP_PREFERENCES_DATA_STORE)),
        )
    }
    factory { RepetitionTimer(coroutineContextProvider = get()) }

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
            observeWordInsightsProviderState = get(),
            setWordInsightsProviderUseCase = get(),
            coroutineContextProvider = get(),
        )
    }

    viewModel<BaseDeckReviewViewModel> { params ->
        DeckReviewViewModel(
            deckId = params.get(),
            stateStore = get<IDeckReviewStateStoreFactory>().create(),
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
            deckReviewScheduler = get(),
            deckReviewNotifier = get(),
            crashlytics = get(),
            coroutineContextProvider = get(),
        )
    }

    viewModel<CardAdditionViewModel> { params ->
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

    viewModel<CardEditingViewModel> { params ->
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

    viewModel { params ->
        WebContentViewModel(
            source = params.get(),
        )
    }
}
