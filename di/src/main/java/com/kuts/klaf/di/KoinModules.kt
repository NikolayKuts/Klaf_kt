package com.kuts.klaf.di

import android.app.NotificationManager
import android.content.Context
import android.net.ConnectivityManager
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import androidx.work.WorkManager
import com.cambridge.dictionary.client.CambridgeClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.kuts.domain.entities.DeckRepetitionInfos
import com.kuts.domain.interactors.AuthenticationInteractor
import com.kuts.domain.managers.IAudioPlayerManager
import com.kuts.domain.managers.IAppMaintenanceManager
import com.kuts.domain.managers.IDeckReviewNotifierManager
import com.kuts.domain.managers.IDeckReviewScheduler as DomainDeckReviewScheduler
import com.kuts.domain.repositories.IAuthenticationRepository
import com.kuts.domain.repositories.ICardRepository
import com.kuts.domain.repositories.ICrashlyticsRepository
import com.kuts.domain.repositories.IDeckRepetitionInfoRepository
import com.kuts.domain.repositories.IDeckRepository
import com.kuts.domain.repositories.IOldAppKlafDataTransferRepository
import com.kuts.domain.repositories.IStorageSaveVersionRepository
import com.kuts.domain.repositories.IStorageTransactionRepository
import com.kuts.domain.repositories.IWordAutocompleteRepository
import com.kuts.domain.repositories.IWordInfoRepository
import com.kuts.domain.useCases.AddNewCardIntoDeckUseCase
import com.kuts.domain.useCases.BackupDataUseCase
import com.kuts.domain.useCases.CheckIfCardExistsUseCase
import com.kuts.domain.useCases.CreateDeckUseCase
import com.kuts.domain.useCases.CreateInterimDeckUseCase
import com.kuts.domain.useCases.DeleteCardsFromDeckUseCase
import com.kuts.domain.useCases.FetchAllDecksUseCase
import com.kuts.domain.useCases.FetchCardUseCase
import com.kuts.domain.useCases.FetchCardsUseCase
import com.kuts.domain.useCases.FetchDeckByIdUseCase
import com.kuts.domain.useCases.FetchDeckRepetitionInfoUseCase
import com.kuts.domain.useCases.FetchDeckSourceUseCase
import com.kuts.domain.useCases.FetchWordAutocompleteUseCase
import com.kuts.domain.useCases.FetchWordInfoUseCase
import com.kuts.domain.useCases.RemoveDeckUseCase
import com.kuts.domain.useCases.RenameDeckUseCase
import com.kuts.domain.useCases.SaveCardRemotelyUseCase
import com.kuts.domain.useCases.SaveDeckRemotelyUseCase
import com.kuts.domain.useCases.SaveDeckReviewInfoUseCase
import com.kuts.domain.useCases.SynchronizeLocalAndRemoteDataUseCase
import com.kuts.domain.useCases.TransferCardsToDeckUseCase
import com.kuts.domain.useCases.TransferDataOfOldAppKlafUseCase
import com.kuts.domain.useCases.UpdateCardUseCase
import com.kuts.domain.useCases.UpdateDeckUseCase
import com.kuts.domain.common.DataSynchronizationValidator
import com.kuts.klaf.authentication.AuthenticationViewModel
import com.kuts.klaf.cardManagement.cardAddition.CardAdditionViewModel
import com.kuts.klaf.cardManagement.cardEditing.CardEditingViewModel
import com.kuts.klaf.cardTransferring.common.BaseCardTransferringViewModel
import com.kuts.klaf.cardTransferring.common.CardTransferringViewModel
import com.kuts.klaf.cardViewing.CardViewingViewModel
import com.kuts.klaf.common.AppMaintenanceManager
import com.kuts.klaf.common.AppReopeningWorker
import com.kuts.klaf.common.DataSynchronizationWorker
import com.kuts.klaf.common.DeckRepetitionReminder
import com.kuts.klaf.common.DeckRepetitionReminderChecker
import com.kuts.klaf.common.DeckReviewRescheduler
import com.kuts.klaf.common.DeckReviewingReminder
import com.kuts.klaf.common.IDeckReviewScheduler as DataDeckReviewScheduler
import com.kuts.klaf.common.NetworkConnectivity
import com.kuts.klaf.common.OldAppKlafDataTransferRepository
import com.kuts.klaf.common.RepetitionTimer
import com.kuts.klaf.common.notifications.AppRestartNotifier
import com.kuts.klaf.common.notifications.DataSynchronizationNotifier
import com.kuts.klaf.common.notifications.NotificationChannelInitializer
import com.kuts.klaf.common.notifications.DeckReviewNotifier
import com.kuts.klaf.dataStore.DECK_REPETITION_INFO_FILE_NAME
import com.kuts.klaf.dataStore.DeckRepetitionInfosSerializer
import com.kuts.klaf.dataStore.implementations.DataStoreDeckRepetitionInfoRepository
import com.kuts.klaf.deckList.common.BaseDeckListViewModel
import com.kuts.klaf.deckList.common.DeckListViewModel
import com.kuts.klaf.deckManagment.BaseDeckManagementViewModel
import com.kuts.klaf.deckManagment.DeckManagementViewModel
import com.kuts.klaf.deckRepetition.BaseDeckReviewViewModel
import com.kuts.klaf.deckRepetition.DeckReviewViewModel
import com.kuts.klaf.deckRepetitionInfo.DeckRepetitionInfoViewModel
import com.kuts.klaf.firestore.repositoryImplementations.AuthenticationRepositoryFirebase
import com.kuts.klaf.firestore.repositoryImplementations.CardRepositoryFirestore
import com.kuts.klaf.firestore.repositoryImplementations.CrashlyticsRepositoryFirebase
import com.kuts.klaf.firestore.repositoryImplementations.DeckRepositoryFirestore
import com.kuts.klaf.firestore.repositoryImplementations.StorageSaveVersionRepositoryFirestore
import com.kuts.klaf.firestore.repositoryImplementations.WordAutocompleteFirestore
import com.kuts.klaf.networking.CardAudioPlayer
import com.kuts.klaf.networking.yandexApi.YandexWordInfoProvider
import com.kuts.klaf.room.databases.KlafRoomDatabase
import com.kuts.klaf.room.repositoryImplementations.CardRepositoryRoom
import com.kuts.klaf.room.repositoryImplementations.DeckRepositoryRoom
import com.kuts.klaf.room.repositoryImplementations.StorageSaveVersionRepositoryRoom
import com.kuts.klaf.room.repositoryImplementations.StorageTransactionRepositoryRoom
import com.lib.lokdroid.core.LoKdroid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.workmanager.dsl.worker
import org.koin.core.qualifier.named
import org.koin.dsl.module

private const val LOCAL_DECK_REPOSITORY = "local_deck_repository"
private const val REMOTE_DECK_REPOSITORY = "remote_deck_repository"
private const val LOCAL_CARD_REPOSITORY = "local_card_repository"
private const val REMOTE_CARD_REPOSITORY = "remote_card_repository"
private const val LOCAL_STORAGE_SAVE_VERSION_REPOSITORY = "local_storage_save_version_repository"
private const val REMOTE_STORAGE_SAVE_VERSION_REPOSITORY = "remote_storage_save_version_repository"

private val infrastructureModule = module {
    single { KlafRoomDatabase.getInstance(context = androidContext()) }
    single { WorkManager.getInstance(androidContext()) }

    single { DeckReviewingReminder(context = androidContext()) }
    single<DomainDeckReviewScheduler> { get<DeckReviewingReminder>() }
    single<DataDeckReviewScheduler> { get<DeckReviewingReminder>() }

    single { FirebaseFirestore.getInstance() }
    single { FirebaseAuth.getInstance() }
    single<FirebaseCrashlytics> { Firebase.crashlytics }

    single<DataStore<DeckRepetitionInfos>> {
        DataStoreFactory.create(
            serializer = DeckRepetitionInfosSerializer,
            corruptionHandler = null,
            migrations = listOf(),
            scope = CoroutineScope(context = Dispatchers.IO + SupervisorJob()),
            produceFile = { androidContext().dataStoreFile(fileName = DECK_REPETITION_INFO_FILE_NAME) },
        )
    }

    single {
        androidContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }
    single {
        androidContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    single { CambridgeClient }
    single { LoKdroid }
}

private val repositoryModule = module {
    single<IDeckRepository>(named(LOCAL_DECK_REPOSITORY)) {
        DeckRepositoryRoom(roomDatabase = get())
    }
    single<ICardRepository>(named(LOCAL_CARD_REPOSITORY)) {
        CardRepositoryRoom(roomDatabase = get())
    }
    single<IStorageSaveVersionRepository>(named(LOCAL_STORAGE_SAVE_VERSION_REPOSITORY)) {
        StorageSaveVersionRepositoryRoom(database = get())
    }

    single<IDeckRepository>(named(REMOTE_DECK_REPOSITORY)) {
        DeckRepositoryFirestore(firestore = get(), auth = get())
    }
    single<ICardRepository>(named(REMOTE_CARD_REPOSITORY)) {
        CardRepositoryFirestore(firestore = get(), auth = get())
    }
    single<IStorageSaveVersionRepository>(named(REMOTE_STORAGE_SAVE_VERSION_REPOSITORY)) {
        StorageSaveVersionRepositoryFirestore(firestore = get(), auth = get())
    }

    single<IStorageTransactionRepository> { StorageTransactionRepositoryRoom(roomDatabase = get()) }
    single<IDeckRepetitionInfoRepository> { DataStoreDeckRepetitionInfoRepository(dataStore = get()) }
    single<IWordAutocompleteRepository> { WordAutocompleteFirestore(firestore = get()) }
    single<ICrashlyticsRepository> { CrashlyticsRepositoryFirebase(firebaseCrashlytics = get()) }
    single<IAuthenticationRepository> {
        AuthenticationRepositoryFirebase(auth = get(), crashlytics = get())
    }
    single<IWordInfoRepository> { YandexWordInfoProvider(context = androidContext()) }
    single<IOldAppKlafDataTransferRepository> {
        OldAppKlafDataTransferRepository(
            context = androidContext(),
            deckRepository = get(named(LOCAL_DECK_REPOSITORY)),
            cardRepository = get(named(LOCAL_CARD_REPOSITORY)),
        )
    }
}

private val managerModule = module {
    single { NotificationChannelInitializer(context = androidContext(), notificationManager = get()) }
    single { AppRestartNotifier(context = androidContext()) }
    single { DataSynchronizationNotifier(context = androidContext()) }
    single { DeckReviewNotifier(context = androidContext(), notificationManager = get()) }
    single { NetworkConnectivity(connectivityManager = get()) }

    single<IDeckReviewNotifierManager> { get<DeckReviewNotifier>() }
    single<IAppMaintenanceManager> {
        AppMaintenanceManager(
            workManager = get(),
            notificationChannelInitializer = get(),
            networkConnectivity = get(),
        )
    }

    factory<IAudioPlayerManager> { CardAudioPlayer(crashlytics = get()) }
}

private val domainModule = module {
    factory { DataSynchronizationValidator() }
    factory { AuthenticationInteractor(authRepository = get()) }

    factory {
        AddNewCardIntoDeckUseCase(
            deckRepository = get(named(LOCAL_DECK_REPOSITORY)),
            cardRepository = get(named(LOCAL_CARD_REPOSITORY)),
            localStorageSaveVersionRepository = get(named(LOCAL_STORAGE_SAVE_VERSION_REPOSITORY)),
            localStorageTransactionRepository = get(),
        )
    }
    factory {
        BackupDataUseCase(
            localDeckRepository = get(named(LOCAL_DECK_REPOSITORY)),
            localCardRepository = get(named(LOCAL_CARD_REPOSITORY)),
            localStorageSaveVersionRepository = get(named(LOCAL_STORAGE_SAVE_VERSION_REPOSITORY)),
            remoteDeckRepository = get(named(REMOTE_DECK_REPOSITORY)),
            remoteCardRepository = get(named(REMOTE_CARD_REPOSITORY)),
            remoteStorageSaveVersionRepository = get(named(REMOTE_STORAGE_SAVE_VERSION_REPOSITORY)),
            dataSynchronizationValidator = get(),
        )
    }
    factory { CheckIfCardExistsUseCase(cardRepository = get(named(LOCAL_CARD_REPOSITORY))) }
    factory {
        CreateDeckUseCase(
            deckRepository = get(named(LOCAL_DECK_REPOSITORY)),
            localStorageSaveVersionRepository = get(named(LOCAL_STORAGE_SAVE_VERSION_REPOSITORY)),
            localStorageTransactionRepository = get(),
        )
    }
    factory {
        CreateInterimDeckUseCase(
            deckRepository = get(named(LOCAL_DECK_REPOSITORY)),
            localStorageSaveVersionRepository = get(named(LOCAL_STORAGE_SAVE_VERSION_REPOSITORY)),
            localStorageTransactionRepository = get(),
        )
    }
    factory {
        DeleteCardsFromDeckUseCase(
            deckRepository = get(named(LOCAL_DECK_REPOSITORY)),
            cardRepository = get(named(LOCAL_CARD_REPOSITORY)),
            localStorageSaveVersionRepository = get(named(LOCAL_STORAGE_SAVE_VERSION_REPOSITORY)),
            localStorageTransactionRepository = get(),
        )
    }
    factory { FetchAllDecksUseCase(deckRepository = get(named(LOCAL_DECK_REPOSITORY))) }
    factory { FetchCardUseCase(cardRepository = get(named(LOCAL_CARD_REPOSITORY))) }
    factory { FetchCardsUseCase(cardRepository = get(named(LOCAL_CARD_REPOSITORY))) }
    factory { FetchDeckByIdUseCase(deckRepository = get(named(LOCAL_DECK_REPOSITORY))) }
    factory { FetchDeckRepetitionInfoUseCase(deckRepetitionInfoRepository = get()) }
    factory { FetchDeckSourceUseCase(deckRepository = get(named(LOCAL_DECK_REPOSITORY))) }
    factory { FetchWordAutocompleteUseCase(wordAutocompleteRepository = get()) }
    factory { FetchWordInfoUseCase(wordInfoRepository = get()) }
    factory {
        RemoveDeckUseCase(
            deckRepository = get(named(LOCAL_DECK_REPOSITORY)),
            cardRepository = get(named(LOCAL_CARD_REPOSITORY)),
            localStorageSaveVersionRepository = get(named(LOCAL_STORAGE_SAVE_VERSION_REPOSITORY)),
            localStorageTransactionRepository = get(),
            deckRepetitionInfoRepository = get(),
        )
    }
    factory {
        RenameDeckUseCase(
            deckRepository = get(named(LOCAL_DECK_REPOSITORY)),
            localStorageSaveVersionRepository = get(named(LOCAL_STORAGE_SAVE_VERSION_REPOSITORY)),
            localStorageTransactionRepository = get(),
        )
    }
    factory { SaveCardRemotelyUseCase(cardRepository = get(named(REMOTE_CARD_REPOSITORY))) }
    factory { SaveDeckRemotelyUseCase(deckRepository = get(named(REMOTE_DECK_REPOSITORY))) }
    factory { SaveDeckReviewInfoUseCase(deckRepetitionInfoRepository = get()) }
    factory {
        SynchronizeLocalAndRemoteDataUseCase(
            localDeckRepository = get(named(LOCAL_DECK_REPOSITORY)),
            localCardRepository = get(named(LOCAL_CARD_REPOSITORY)),
            localStorageSaveVersionRepository = get(named(LOCAL_STORAGE_SAVE_VERSION_REPOSITORY)),
            remoteDeckRepository = get(named(REMOTE_DECK_REPOSITORY)),
            remoteCardRepository = get(named(REMOTE_CARD_REPOSITORY)),
            remoteStorageSaveVersionRepository = get(named(REMOTE_STORAGE_SAVE_VERSION_REPOSITORY)),
            dataSynchronizationValidator = get(),
        )
    }
    factory {
        TransferCardsToDeckUseCase(
            cardRepository = get(named(LOCAL_CARD_REPOSITORY)),
            deckRepository = get(named(LOCAL_DECK_REPOSITORY)),
            localStorageSaveVersionRepository = get(named(LOCAL_STORAGE_SAVE_VERSION_REPOSITORY)),
            localStorageTransactionRepository = get(),
        )
    }
    factory { TransferDataOfOldAppKlafUseCase(oldAppKlafDataTransferRepository = get()) }
    factory {
        UpdateCardUseCase(
            cardRepository = get(named(LOCAL_CARD_REPOSITORY)),
            localStorageSaveVersionRepository = get(named(LOCAL_STORAGE_SAVE_VERSION_REPOSITORY)),
            localStorageTransactionRepository = get(),
        )
    }
    factory {
        UpdateDeckUseCase(
            deckRepository = get(named(LOCAL_DECK_REPOSITORY)),
            localStorageSaveVersionRepository = get(named(LOCAL_STORAGE_SAVE_VERSION_REPOSITORY)),
            localStorageTransactionRepository = get(),
        )
    }
}

private val presentationModule = module {
    factory { RepetitionTimer() }

    viewModel { AuthenticationViewModel(authenticationInteractor = get()) }

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
            deckReviewScheduler = get<DomainDeckReviewScheduler>(),
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

private val workerModule = module {
    worker { AppReopeningWorker(get(), get(), get()) }
    worker { DeckRepetitionReminder(get(), get(), get()) }
    worker { DeckRepetitionReminderChecker(get(), get(), get(), get(), get()) }
    worker { DataSynchronizationWorker(get(), get(), get(), get(), get()) }
    worker { DeckReviewRescheduler(get(), get(), get(), get()) }
}

val appModules = listOf(
    infrastructureModule,
    repositoryModule,
    managerModule,
    domainModule,
    presentationModule,
    workerModule,
)
