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
import com.kuts.domain.common.DataSynchronizationValidator
import com.kuts.domain.common.ICoroutineContextProvider
import com.kuts.domain.entities.DeckRepetitionInfos
import com.kuts.domain.interactors.AuthenticationInteractor
import com.kuts.domain.managers.IAppMaintenanceManager
import com.kuts.domain.managers.IAudioPlayerManager
import com.kuts.domain.managers.IDeckReviewScheduler
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
import com.kuts.klaf.common.AppMaintenanceManager
import com.kuts.klaf.common.AppReopeningWorker
import com.kuts.klaf.common.CoroutineContextProvider
import com.kuts.klaf.common.DataSynchronizationWorker
import com.kuts.klaf.common.DeckRepetitionReminder
import com.kuts.klaf.common.DeckRepetitionReminderChecker
import com.kuts.klaf.common.DeckReviewRescheduler
import com.kuts.klaf.common.DeckReviewingReminder
import com.kuts.klaf.common.NetworkConnectivity
import com.kuts.klaf.common.OldAppKlafDataTransferRepository
import com.kuts.klaf.common.notifications.AppRestartNotifier
import com.kuts.klaf.common.notifications.DataSynchronizationNotifier
import com.kuts.klaf.common.notifications.NotificationChannelInitializer
import com.kuts.klaf.dataStore.DECK_REPETITION_INFO_FILE_NAME
import com.kuts.klaf.dataStore.DeckRepetitionInfosSerializer
import com.kuts.klaf.dataStore.implementations.DataStoreDeckRepetitionInfoRepository
import com.kuts.klaf.firestore.repositoryImplementations.AuthenticationRepositoryFirebase
import com.kuts.klaf.firestore.repositoryImplementations.CardRepositoryFirestore
import com.kuts.klaf.firestore.repositoryImplementations.CrashlyticsRepositoryFirebase
import com.kuts.klaf.firestore.repositoryImplementations.DeckRepositoryFirestore
import com.kuts.klaf.firestore.repositoryImplementations.StorageSaveVersionRepositoryFirestore
import com.kuts.klaf.firestore.repositoryImplementations.WordAutocompleteFirestore
import com.kuts.klaf.networking.CardAudioPlayer
import com.kuts.klaf.networking.yandexApi.YandexSecureHttpClientFactory
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
import org.koin.androidx.workmanager.dsl.worker
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module


private const val LOCAL_DECK_REPOSITORY = "local_deck_repository"
private const val REMOTE_DECK_REPOSITORY = "remote_deck_repository"
private const val LOCAL_CARD_REPOSITORY = "local_card_repository"
private const val REMOTE_CARD_REPOSITORY = "remote_card_repository"
private const val LOCAL_STORAGE_SAVE_VERSION_REPOSITORY = "local_storage_save_version_repository"
private const val REMOTE_STORAGE_SAVE_VERSION_REPOSITORY = "remote_storage_save_version_repository"

internal val dataModule = module {
    repositoryModule()
    useCaseModule()
    infrastructureModule()
    dataManagerBindings()
    workerModule()
}


private fun Module.repositoryModule() {
    single<IDeckRepository>(
        qualifier = named(name = LOCAL_DECK_REPOSITORY),
    ) {
        DeckRepositoryRoom(roomDatabase = get())
    }
    single<ICardRepository>(
        qualifier = named(name = LOCAL_CARD_REPOSITORY),
    ) {
        CardRepositoryRoom(roomDatabase = get())
    }
    single<IStorageSaveVersionRepository>(
        qualifier = named(name = LOCAL_STORAGE_SAVE_VERSION_REPOSITORY),
    ) {
        StorageSaveVersionRepositoryRoom(database = get())
    }

    single<IDeckRepository>(
        qualifier = named(name = REMOTE_DECK_REPOSITORY),
    ) {
        DeckRepositoryFirestore(
            firestore = get(),
            auth = get(),
        )
    }
    single<ICardRepository>(
        qualifier = named(name = REMOTE_CARD_REPOSITORY),
    ) {
        CardRepositoryFirestore(
            firestore = get(),
            auth = get(),
        )
    }
    single<IStorageSaveVersionRepository>(
        qualifier = named(name = REMOTE_STORAGE_SAVE_VERSION_REPOSITORY),
    ) {
        StorageSaveVersionRepositoryFirestore(
            firestore = get(),
            auth = get(),
        )
    }

    single<IStorageTransactionRepository> { StorageTransactionRepositoryRoom(roomDatabase = get()) }
    single<IDeckRepetitionInfoRepository> { DataStoreDeckRepetitionInfoRepository(dataStore = get()) }
    single<IWordAutocompleteRepository> { WordAutocompleteFirestore(firestore = get()) }
    single<ICrashlyticsRepository> { CrashlyticsRepositoryFirebase(firebaseCrashlytics = get()) }
    single<IAuthenticationRepository> {
        AuthenticationRepositoryFirebase(
            auth = get(),
            crashlytics = get(),
        )
    }
    single<IWordInfoRepository> {
        YandexWordInfoProvider(
            client = YandexSecureHttpClientFactory(
                context = androidContext(),
            ).create(),
        )
    }
    single<IOldAppKlafDataTransferRepository> {
        OldAppKlafDataTransferRepository(
            context = androidContext(),
            deckRepository = get(qualifier = named(name = LOCAL_DECK_REPOSITORY)),
            cardRepository = get(qualifier = named(name = LOCAL_CARD_REPOSITORY)),
        )
    }
}

private fun Module.useCaseModule() {
    factory { DataSynchronizationValidator() }
    factory { AuthenticationInteractor(authRepository = get()) }

    factory {
        AddNewCardIntoDeckUseCase(
            deckRepository = get(qualifier = named(name = LOCAL_DECK_REPOSITORY)),
            cardRepository = get(qualifier = named(name = LOCAL_CARD_REPOSITORY)),
            localStorageSaveVersionRepository = get(
                qualifier = named(name = LOCAL_STORAGE_SAVE_VERSION_REPOSITORY),
            ),
            localStorageTransactionRepository = get(),
            coroutineContextProvider = get(),
        )
    }
    factory {
        BackupDataUseCase(
            localDeckRepository = get(qualifier = named(name = LOCAL_DECK_REPOSITORY)),
            localCardRepository = get(qualifier = named(name = LOCAL_CARD_REPOSITORY)),
            localStorageSaveVersionRepository = get(
                qualifier = named(name = LOCAL_STORAGE_SAVE_VERSION_REPOSITORY),
            ),
            remoteDeckRepository = get(qualifier = named(name = REMOTE_DECK_REPOSITORY)),
            remoteCardRepository = get(qualifier = named(name = REMOTE_CARD_REPOSITORY)),
            remoteStorageSaveVersionRepository = get(
                qualifier = named(name = REMOTE_STORAGE_SAVE_VERSION_REPOSITORY),
            ),
            dataSynchronizationValidator = get(),
            coroutineContextProvider = get(),
        )
    }
    factory {
        CheckIfCardExistsUseCase(
            cardRepository = get(qualifier = named(name = LOCAL_CARD_REPOSITORY)),
            coroutineContextProvider = get(),
        )
    }
    factory {
        CreateDeckUseCase(
            deckRepository = get(qualifier = named(name = LOCAL_DECK_REPOSITORY)),
            localStorageSaveVersionRepository = get(
                qualifier = named(name = LOCAL_STORAGE_SAVE_VERSION_REPOSITORY),
            ),
            localStorageTransactionRepository = get(),
            coroutineContextProvider = get(),
        )
    }
    factory {
        CreateInterimDeckUseCase(
            deckRepository = get(qualifier = named(name = LOCAL_DECK_REPOSITORY)),
            localStorageSaveVersionRepository = get(
                qualifier = named(name = LOCAL_STORAGE_SAVE_VERSION_REPOSITORY),
            ),
            localStorageTransactionRepository = get(),
            coroutineContextProvider = get(),
        )
    }
    factory {
        DeleteCardsFromDeckUseCase(
            deckRepository = get(qualifier = named(name = LOCAL_DECK_REPOSITORY)),
            cardRepository = get(qualifier = named(name = LOCAL_CARD_REPOSITORY)),
            localStorageSaveVersionRepository = get(
                qualifier = named(name = LOCAL_STORAGE_SAVE_VERSION_REPOSITORY),
            ),
            localStorageTransactionRepository = get(),
            coroutineContextProvider = get(),
        )
    }
    factory {
        FetchAllDecksUseCase(
            deckRepository = get(qualifier = named(name = LOCAL_DECK_REPOSITORY)),
            coroutineContextProvider = get(),
        )
    }
    factory {
        FetchCardUseCase(
            cardRepository = get(qualifier = named(name = LOCAL_CARD_REPOSITORY)),
        )
    }
    factory {
        FetchCardsUseCase(
            cardRepository = get(qualifier = named(name = LOCAL_CARD_REPOSITORY)),
        )
    }
    factory {
        FetchDeckByIdUseCase(
            deckRepository = get(qualifier = named(name = LOCAL_DECK_REPOSITORY)),
        )
    }
    factory { FetchDeckRepetitionInfoUseCase(deckRepetitionInfoRepository = get()) }
    factory {
        FetchDeckSourceUseCase(
            deckRepository = get(qualifier = named(name = LOCAL_DECK_REPOSITORY)),
        )
    }
    factory {
        FetchWordAutocompleteUseCase(
            wordAutocompleteRepository = get(),
            coroutineContextProvider = get(),
        )
    }
    factory {
        FetchWordInfoUseCase(
            wordInfoRepository = get(),
            coroutineContextProvider = get(),
        )
    }
    factory {
        RemoveDeckUseCase(
            deckRepository = get(qualifier = named(name = LOCAL_DECK_REPOSITORY)),
            cardRepository = get(qualifier = named(name = LOCAL_CARD_REPOSITORY)),
            localStorageSaveVersionRepository = get(
                qualifier = named(name = LOCAL_STORAGE_SAVE_VERSION_REPOSITORY),
            ),
            localStorageTransactionRepository = get(),
            deckRepetitionInfoRepository = get(),
            coroutineContextProvider = get(),
        )
    }
    factory {
        RenameDeckUseCase(
            deckRepository = get(qualifier = named(name = LOCAL_DECK_REPOSITORY)),
            localStorageSaveVersionRepository = get(
                qualifier = named(name = LOCAL_STORAGE_SAVE_VERSION_REPOSITORY),
            ),
            localStorageTransactionRepository = get(),
            coroutineContextProvider = get(),
        )
    }
    factory {
        SaveCardRemotelyUseCase(
            cardRepository = get(qualifier = named(name = REMOTE_CARD_REPOSITORY)),
            coroutineContextProvider = get(),
        )
    }
    factory {
        SaveDeckRemotelyUseCase(
            deckRepository = get(qualifier = named(name = REMOTE_DECK_REPOSITORY)),
            coroutineContextProvider = get(),
        )
    }
    factory {
        SaveDeckReviewInfoUseCase(
            deckRepetitionInfoRepository = get(),
            coroutineContextProvider = get(),
        )
    }
    factory {
        SynchronizeLocalAndRemoteDataUseCase(
            localDeckRepository = get(qualifier = named(name = LOCAL_DECK_REPOSITORY)),
            localCardRepository = get(qualifier = named(name = LOCAL_CARD_REPOSITORY)),
            localStorageSaveVersionRepository = get(
                qualifier = named(name = LOCAL_STORAGE_SAVE_VERSION_REPOSITORY),
            ),
            remoteDeckRepository = get(qualifier = named(name = REMOTE_DECK_REPOSITORY)),
            remoteCardRepository = get(qualifier = named(name = REMOTE_CARD_REPOSITORY)),
            remoteStorageSaveVersionRepository = get(
                qualifier = named(name = REMOTE_STORAGE_SAVE_VERSION_REPOSITORY),
            ),
            dataSynchronizationValidator = get(),
            coroutineContextProvider = get(),
        )
    }
    factory {
        TransferCardsToDeckUseCase(
            cardRepository = get(qualifier = named(name = LOCAL_CARD_REPOSITORY)),
            deckRepository = get(qualifier = named(name = LOCAL_DECK_REPOSITORY)),
            localStorageSaveVersionRepository = get(
                qualifier = named(name = LOCAL_STORAGE_SAVE_VERSION_REPOSITORY),
            ),
            localStorageTransactionRepository = get(),
            coroutineContextProvider = get(),
        )
    }
    factory {
        TransferDataOfOldAppKlafUseCase(
            oldAppKlafDataTransferRepository = get(),
            coroutineContextProvider = get(),
        )
    }
    factory {
        UpdateCardUseCase(
            cardRepository = get(qualifier = named(name = LOCAL_CARD_REPOSITORY)),
            localStorageSaveVersionRepository = get(
                qualifier = named(name = LOCAL_STORAGE_SAVE_VERSION_REPOSITORY),
            ),
            localStorageTransactionRepository = get(),
            coroutineContextProvider = get(),
        )
    }
    factory {
        UpdateDeckUseCase(
            deckRepository = get(qualifier = named(name = LOCAL_DECK_REPOSITORY)),
            localStorageSaveVersionRepository = get(
                qualifier = named(name = LOCAL_STORAGE_SAVE_VERSION_REPOSITORY),
            ),
            localStorageTransactionRepository = get(),
            coroutineContextProvider = get(),
        )
    }
}


private fun Module.infrastructureModule() {
    single { KlafRoomDatabase.getInstance(context = androidContext()) }
    single { WorkManager.getInstance(androidContext()) }
    single<ICoroutineContextProvider> { CoroutineContextProvider() }

    single<IDeckReviewScheduler> { DeckReviewingReminder(context = androidContext()) }

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

private fun Module.dataManagerBindings() {
    single {
        NotificationChannelInitializer(
            context = androidContext(),
            notificationManager = get(),
        )
    }
    single { AppRestartNotifier(context = androidContext()) }
    single { DataSynchronizationNotifier(context = androidContext()) }
    single { NetworkConnectivity(connectivityManager = get()) }

    single<IAppMaintenanceManager> {
        AppMaintenanceManager(
            workManager = get(),
            notificationChannelInitializer = get(),
            networkConnectivity = get(),
        )
    }

    factory<IAudioPlayerManager> { CardAudioPlayer(crashlytics = get()) }
}


private fun Module.workerModule() {
    worker {
        AppReopeningWorker(
            application = get(),
            workerParams = get(),
            appRestartNotifier = get(),
        )
    }
    worker {
        DeckRepetitionReminder(
            appContext = get(),
            parameters = get(),
            deckReviewNotifier = get(),
        )
    }
    worker {
        DeckRepetitionReminderChecker(
            context = get(),
            params = get(),
            deckReviewNotifier = get(),
            fetchAllDecks = get(),
            crashlytics = get(),
        )
    }
    worker {
        DataSynchronizationWorker(
            appContext = get(),
            params = get(),
            synchronizeLocalAndRemoteData = get(),
            dataSynchronizationNotifier = get(),
            crashlytics = get(),
        )
    }
    worker {
        DeckReviewRescheduler(
            appContext = get(),
            parameters = get(),
            fetchAllDecksUseCase = get(),
            deckReviewingReminder = get<IDeckReviewScheduler>(),
        )
    }
}
