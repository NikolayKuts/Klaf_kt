package com.kuts.klaf.di

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import com.kuts.domain.managers.IDeckReviewNotifierManager
import com.kuts.klaf.authentication.AuthenticationViewModel
import com.kuts.klaf.authentication.BaseAuthenticationViewModel
import com.kuts.klaf.cardManagement.cardAddition.CardAdditionViewModel
import com.kuts.klaf.cardManagement.cardEditing.CardEditingViewModel
import com.kuts.klaf.cardTransferring.common.BaseCardTransferringViewModel
import com.kuts.klaf.cardTransferring.common.CardTransferringViewModel
import com.kuts.klaf.cardViewing.CardViewingViewModel
import com.kuts.klaf.common.RepetitionTimer
import com.kuts.klaf.common.localStore.AppLocalStore
import com.kuts.klaf.common.notifications.DeckReviewNotifier
import com.kuts.klaf.common.permissions.INotificationPermissionBinder
import com.kuts.klaf.common.permissions.INotificationPermissionManager
import com.kuts.klaf.common.permissions.MokoNotificationPermissionManager
import com.kuts.klaf.deckList.common.BaseDeckListViewModel
import com.kuts.klaf.deckList.common.DeckListViewModel
import com.kuts.klaf.deckManagment.BaseDeckManagementViewModel
import com.kuts.klaf.deckManagment.DeckManagementViewModel
import com.kuts.klaf.deckRepetition.BaseDeckReviewViewModel
import com.kuts.klaf.deckRepetition.DeckReviewViewModel
import com.kuts.klaf.deckRepetitionInfo.DeckRepetitionInfoViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.dsl.module

internal val presentationModule = module {
    factory { RepetitionTimer() }
    single {
        DeckReviewNotifier(
            context = androidContext(),
            notificationManager = get(),
        )
    }
    single<IDeckReviewNotifierManager> { get<DeckReviewNotifier>() }
    single {
        AppLocalStore(
            dataStore = PreferenceDataStoreFactory.create(
                produceFile = {
                    androidContext().preferencesDataStoreFile(AppLocalStore.DATA_STORE_NAME)
                },
            ),
        )
    }
    single { MokoNotificationPermissionManager(context = androidContext(), appLocalStore = get()) }
    single<INotificationPermissionManager> { get<MokoNotificationPermissionManager>() }
    single<INotificationPermissionBinder> { get<MokoNotificationPermissionManager>() }
    viewModels()
}

private fun Module.viewModels() {
    viewModel<BaseAuthenticationViewModel> { AuthenticationViewModel(authenticationInteractor = get()) }

    viewModel<BaseDeckListViewModel> {
        DeckListViewModel(
            fetchDeckSource = get(),
            createInterimDeck = get(),
            createDeck = get(),
            renameDeck = get(),
            removeDeck = get(),
            fetchCardsUseCase = get(),
            auth = get(),
            crashlytics = get(),
            appMaintenanceManager = get(),
            authenticationInteractor = get(),
        )
    }

    viewModel<BaseDeckReviewViewModel> { params ->
        DeckReviewViewModel(
            deckId = params.get(),
            handle = get(),
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
        )
    }

    viewModel<BaseDeckManagementViewModel> { params ->
        DeckManagementViewModel(
            deckId = params.get(),
            fetchDeckById = get(),
            updateDeck = get(),
            crashlytics = get(),
        )
    }

    viewModel<CardAdditionViewModel> { params ->
        CardAdditionViewModel(
            deckId = params.get(),
            smartSelectedWord = params.getOrNull(),
            addNewCardIntoDeck = get(),
            checkIfWordExists = get(),
            audioPlayer = get(),
            cambridgeClient = get(),
            fetchWordAutocomplete = get(),
            fetchWordInfo = get(),
            crashlytics = get(),
            fetchDeckById = get(),
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
            cambridgeClient = get(),
            fetchWordAutocomplete = get(),
            fetchWordInfo = get(),
            crashlytics = get(),
            fetchDeckById = get(),
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
