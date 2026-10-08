package com.kuts.klaf.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.kuts.domain.common.ICoroutineContextProvider
import com.kuts.domain.managers.VocabularySourceTranscriptionCoordinator
import com.kuts.klaf.authentication.AuthenticationViewModel
import com.kuts.klaf.authentication.AccountAuthenticationViewModel
import com.kuts.klaf.authentication.BaseAuthenticationViewModel
import com.kuts.klaf.room.databases.ActiveLocalRoomDatabase
import com.kuts.klaf.room.databases.RoomDatabaseSource
import com.kuts.klaf.cardManagement.cardAddition.CardAdditionViewModel
import com.kuts.klaf.cardManagement.cardEditing.CardEditingViewModel
import com.kuts.klaf.cardTransferring.common.BaseCardTransferringViewModel
import com.kuts.klaf.cardTransferring.common.CardTransferringViewModel
import com.kuts.klaf.cardViewing.CardViewingViewModel
import com.kuts.klaf.common.RepetitionTimer
import com.kuts.klaf.common.localStore.AppLocalStore
import com.kuts.klaf.deckList.common.BaseDeckListViewModel
import com.kuts.klaf.deckList.common.AccountDeckListGateway
import com.kuts.klaf.deckList.common.AccountSyncStatusGateway
import com.kuts.klaf.room.repositoryImplementations.RoomMnemonicImageDownloader
import com.kuts.klaf.deckList.conflictResolution.AccountConflictGateway
import com.kuts.klaf.networking.klafServer.SyncEventConnector
import com.kuts.klaf.networking.klafServer.KlafServerSyncRestClient
import com.kuts.klaf.networking.klafServer.AccountDeviceIdentity
import com.kuts.klaf.room.repositoryImplementations.RoomSyncConflictStore
import com.kuts.klaf.deckList.common.DeckListViewModel
import com.kuts.klaf.deckManagment.BaseDeckManagementViewModel
import com.kuts.klaf.deckManagment.DeckManagementViewModel
import com.kuts.klaf.deckRepetition.BaseDeckReviewViewModel
import com.kuts.klaf.deckRepetition.DeckReviewViewModel
import com.kuts.klaf.deckRepetitionInfo.DeckRepetitionInfoViewModel
import com.kuts.klaf.vocabularySource.VocabularySourceDetailViewModel
import com.kuts.klaf.vocabularySource.VocabularySourceListViewModel
import com.kuts.klaf.webContent.WebContentViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

internal val commonPresentationModule = module {
    single {
        VocabularySourceTranscriptionCoordinator(
            transcribe = get(),
            backgroundManager = get(),
            scope = CoroutineScope(SupervisorJob() + get<ICoroutineContextProvider>().io),
            onBackgroundFailure = { println("Transcription notification finish failed: ${it::class.simpleName}") },
        )
    }
    single<DataStore<Preferences>>(qualifier = named(name = APP_PREFERENCES_DATA_STORE)) {
        get<IAppPreferencesDataStoreFactory>().create()
    }
    single {
        AppLocalStore(
            dataStore = get(qualifier = named(name = APP_PREFERENCES_DATA_STORE)),
        )
    }
    factory { RepetitionTimer(coroutineContextProvider = get()) }
    single<AccountDeckListGateway> { RoomAccountDeckListGateway(accountSession = get(), coordinator = get()) }
    single<AccountSyncStatusGateway> {
        val databaseSource = get<ActiveLocalRoomDatabase>()
        val identity = get<AccountDeviceIdentity>()
        val rest = get<KlafServerSyncRestClient>()
        RoomAccountSyncStatusGateway(
            databaseSource = databaseSource,
            identity = identity,
            connector = get<SyncEventConnector>(),
            coordinator = get(),
            hasMissingImages = get<RoomMnemonicImageDownloader>()::hasMissingImages,
            historyReader = AccountSyncHistoryReader(
                selectedEmail = { databaseSource.selection.value.accountEmail },
                deviceId = { identity.current().id },
                fetch = rest::recentHistory,
            ),
        )
    }
    single<AccountConflictGateway> {
        RoomAccountConflictGateway(
            databaseSource = get(),
            conflictStore = RoomSyncConflictStore(get()),
            coordinator = get(),
        )
    }

    viewModel<BaseAuthenticationViewModel> {
        if (get<RoomDatabaseSource>() is ActiveLocalRoomDatabase) {
            AccountAuthenticationViewModel(accountSession = get(), coroutineContextProvider = get())
        } else {
            AuthenticationViewModel(
                authenticationInteractor = get(),
                coroutineContextProvider = get<ICoroutineContextProvider>(),
            )
        }
    }

    viewModel<BaseDeckListViewModel> {
        val accountMode = get<RoomDatabaseSource>() is ActiveLocalRoomDatabase
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
            observeKlafServerConnectionState = get(),
            retryKlafServerConnectionUseCase = get(),
            coroutineContextProvider = get(),
            accountGateway = if (accountMode) get() else null,
            accountStatusGateway = if (accountMode) get() else null,
            accountSession = if (accountMode) get() else null,
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
            saveCompletedReview = get(),
            deleteCardsFromDeck = get(),
            deckReviewScheduler = get(),
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
            fetchMnemonicAssociation = get(),
            fetchMnemonicImage = get(),
            mnemonicImageAssetRepository = get(),
            mnemonicGenerationBackgroundManager = get(),
            fetchWordAutocomplete = get(),
            fetchWordInfo = get(),
            speechRecognitionManager = get(),
            microphonePermissionManager = get(),
            crashlytics = get(),
            fetchDeckById = get(),
            observeKlafServerConnectionState = get(),
            coroutineContextProvider = get(),
        )
    }

    viewModel<CardEditingViewModel> { params ->
        CardEditingViewModel(
            deckId = params.get(),
            cardId = params.get(),
            fetchCard = get(),
            updateCard = get(),
            fetchMnemonicAssociation = get(),
            fetchMnemonicImage = get(),
            mnemonicImageAssetRepository = get(),
            mnemonicGenerationBackgroundManager = get(),
            fetchWordMeaningInsights = get(),
            checkIfWordExists = get(),
            audioPlayer = get(),
            cambridgeWordDataProvider = get(),
            fetchWordAutocomplete = get(),
            fetchWordInfo = get(),
            speechRecognitionManager = get(),
            microphonePermissionManager = get(),
            crashlytics = get(),
            fetchDeckById = get(),
            observeKlafServerConnectionState = get(),
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

    viewModel {
        VocabularySourceListViewModel(
            observeVocabularySources = get(),
            observeAllVocabularySourceItems = get(),
            createVocabularySource = get(),
            removeVocabularySource = get(),
        )
    }

    viewModel { params ->
        VocabularySourceDetailViewModel(
            sourceId = params.get(),
            observeVocabularySourceById = get(),
            observeVocabularySourceItems = get(),
            saveVocabularySourceChanges = get(),
            analyzeVocabularySourceText = get(),
            transcriptionCoordinator = get(),
            fetchAllCards = get(),
            fetchIgnoredVocabularyWords = get(),
            fetchDeckSource = get(),
            addVocabularySourceItemsToDeck = get(),
            vocabularySourceAnalysisBackgroundManager = get(),
            textToSpeechManager = get(),
        )
    }

    viewModel { params ->
        WebContentViewModel(
            source = params.get(),
        )
    }
}
