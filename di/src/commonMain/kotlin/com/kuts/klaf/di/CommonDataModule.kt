package com.kuts.klaf.di

import com.kuts.klaf.room.repositoryImplementations.RoomMnemonicImageUploader
import com.kuts.klaf.networking.klafServer.KlafServerImageRestClient
import com.kuts.domain.common.DataSynchronizationValidator
import com.kuts.domain.interactors.AuthenticationInteractor
import com.kuts.domain.repositories.ICardRepository
import com.kuts.domain.repositories.IDeckRepository
import com.kuts.domain.repositories.IIgnoredVocabularyWordRepository
import com.kuts.domain.repositories.IDeckReviewResultRepository

import com.kuts.domain.repositories.IMnemonicImageRemoteRepository
import com.kuts.domain.repositories.IStorageSaveVersionRepository
import com.kuts.domain.repositories.IStorageTransactionRepository
import com.kuts.domain.repositories.IVocabularySourceAnalysisRepository
import com.kuts.domain.repositories.IVocabularySourceRepository
import com.kuts.domain.repositories.IVocabularySourceTranscriptionRepository
import com.kuts.domain.useCases.AddNewCardIntoDeckUseCase
import com.kuts.domain.useCases.AddVocabularySourceItemsToDeckUseCase
import com.kuts.domain.useCases.AnalyzeVocabularySourceTextUseCase
import com.kuts.domain.useCases.TranscribeVocabularySourceAudioUseCase
import com.kuts.domain.useCases.BackupDataUseCase
import com.kuts.domain.useCases.CheckIfCardExistsUseCase
import com.kuts.domain.useCases.CreateDeckUseCase
import com.kuts.domain.useCases.CreateInterimDeckUseCase
import com.kuts.domain.useCases.CreateVocabularySourceUseCase
import com.kuts.domain.useCases.DeleteCardsFromDeckUseCase
import com.kuts.domain.useCases.FetchAllDecksUseCase
import com.kuts.domain.useCases.FetchAllCardsUseCase
import com.kuts.domain.useCases.FetchCardUseCase
import com.kuts.domain.useCases.FetchCardsUseCase
import com.kuts.domain.useCases.FetchDeckByIdUseCase
import com.kuts.domain.useCases.FetchDeckRepetitionInfoUseCase
import com.kuts.domain.useCases.FetchDeckSourceUseCase
import com.kuts.domain.useCases.FetchIgnoredVocabularyWordsUseCase
import com.kuts.domain.useCases.FetchMnemonicAssociationUseCase
import com.kuts.domain.useCases.FetchMnemonicImageUseCase
import com.kuts.domain.useCases.FetchVocabularySourceByIdUseCase
import com.kuts.domain.useCases.FetchVocabularySourceItemsUseCase
import com.kuts.domain.useCases.FetchVocabularySourcesUseCase
import com.kuts.domain.useCases.FetchWordAutocompleteUseCase
import com.kuts.domain.useCases.FetchWordInfoUseCase
import com.kuts.domain.useCases.FetchWordMeaningInsightsUseCase
import com.kuts.domain.useCases.ObserveKlafServerConnectionStateUseCase
import com.kuts.domain.useCases.ObserveAllVocabularySourceItemsUseCase
import com.kuts.domain.useCases.ObserveVocabularySourceByIdUseCase
import com.kuts.domain.useCases.ObserveVocabularySourceItemsUseCase
import com.kuts.domain.useCases.ObserveVocabularySourcesUseCase
import com.kuts.domain.useCases.RemoveDeckUseCase
import com.kuts.domain.useCases.RemoveVocabularySourceUseCase
import com.kuts.domain.useCases.ReplaceVocabularySourceDraftItemsUseCase
import com.kuts.domain.useCases.RenameDeckUseCase
import com.kuts.domain.useCases.SaveCardRemotelyUseCase
import com.kuts.domain.useCases.SaveCompletedDeckReviewUseCase
import com.kuts.domain.useCases.SaveDeckRemotelyUseCase
import com.kuts.domain.useCases.SaveDeckReviewInfoUseCase
import com.kuts.domain.useCases.RetryKlafServerConnectionUseCase
import com.kuts.domain.useCases.SaveVocabularySourceItemsUseCase
import com.kuts.domain.useCases.SaveVocabularySourceChangesUseCase
import com.kuts.domain.useCases.SynchronizeLocalAndRemoteDataUseCase
import com.kuts.domain.useCases.TransferCardsToDeckUseCase
import com.kuts.domain.useCases.TransferDataOfOldAppKlafUseCase
import com.kuts.domain.useCases.UpdateCardUseCase
import com.kuts.domain.useCases.UpdateDeckUseCase
import com.kuts.domain.useCases.UpdateVocabularySourceUseCase
import com.kuts.klaf.networking.klafServer.KlafServerVocabularySourceAnalysisRepository
import com.kuts.klaf.networking.klafServer.KlafServerVocabularySourceTranscriptionRepository
import com.kuts.klaf.room.repositoryImplementations.CardRepositoryRoom
import com.kuts.klaf.room.repositoryImplementations.DeckRepositoryRoom
import com.kuts.klaf.room.repositoryImplementations.IgnoredVocabularyWordRepositoryRoom
import com.kuts.klaf.networking.klafServer.AccountDeviceIdentity
import com.kuts.klaf.networking.klafServer.KlafServerSyncRestClient
import com.kuts.klaf.room.databases.RoomDatabaseSource
import com.kuts.klaf.room.databases.ActiveLocalRoomDatabase
import com.kuts.klaf.room.repositoryImplementations.RoomDeckReviewResultRepository
import com.kuts.klaf.room.repositoryImplementations.ManualRoomSyncCoordinator
import com.kuts.klaf.room.repositoryImplementations.RoomSyncDeltaApplier
import com.kuts.klaf.room.repositoryImplementations.RoomSyncOutbox

import com.kuts.klaf.room.repositoryImplementations.StorageSaveVersionRepositoryRoom
import com.kuts.klaf.room.repositoryImplementations.StorageTransactionRepositoryRoom
import com.kuts.klaf.room.repositoryImplementations.VocabularySourceRepositoryRoom
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

internal const val LOCAL_DECK_REPOSITORY = "local_deck_repository"
internal const val REMOTE_DECK_REPOSITORY = "remote_deck_repository"
internal const val LOCAL_CARD_REPOSITORY = "local_card_repository"
internal const val REMOTE_CARD_REPOSITORY = "remote_card_repository"
internal const val LOCAL_STORAGE_SAVE_VERSION_REPOSITORY = "local_storage_save_version_repository"
internal const val REMOTE_STORAGE_SAVE_VERSION_REPOSITORY = "remote_storage_save_version_repository"

internal val commonDataModule = module {
    commonRepositoryModule()
    commonUseCaseModule()
}

private fun Module.commonRepositoryModule() {
    single<com.kuts.domain.managers.IClientSessionScope> {
        get<com.kuts.klaf.networking.klafServer.IKlafServerSession>()
    }
    single<IDeckRepository>(
        qualifier = named(name = LOCAL_DECK_REPOSITORY),
    ) {
        DeckRepositoryRoom(databaseSource = get<RoomDatabaseSource>())
    }
    single<ICardRepository>(
        qualifier = named(name = LOCAL_CARD_REPOSITORY),
    ) {
        CardRepositoryRoom(databaseSource = get<RoomDatabaseSource>())
    }
    single<IStorageSaveVersionRepository>(
        qualifier = named(name = LOCAL_STORAGE_SAVE_VERSION_REPOSITORY),
    ) {
        StorageSaveVersionRepositoryRoom(databaseSource = get<RoomDatabaseSource>())
    }
    single<IVocabularySourceRepository> {
        VocabularySourceRepositoryRoom(databaseSource = get<RoomDatabaseSource>())
    }
    single<IIgnoredVocabularyWordRepository> {
        IgnoredVocabularyWordRepositoryRoom(databaseSource = get<RoomDatabaseSource>())
    }
    single<IVocabularySourceAnalysisRepository> {
        KlafServerVocabularySourceAnalysisRepository(klafServerSession = get())
    }
    single<IVocabularySourceTranscriptionRepository> {
        KlafServerVocabularySourceTranscriptionRepository(klafServerSession = get())
    }

    single<IStorageTransactionRepository> { StorageTransactionRepositoryRoom(databaseSource = get<RoomDatabaseSource>()) }
    single { RoomSyncOutbox(databaseSource = get()) }
    single { RoomSyncDeltaApplier(databaseSource = get(), outbox = get()) }
    single {
        RoomMnemonicImageUploader(
            assets = get(), upload = get<KlafServerImageRestClient>()::upload,
        )
    }
    single {
        val identity = get<AccountDeviceIdentity>()
        val rest = get<KlafServerSyncRestClient>()
        ManualRoomSyncCoordinator(
            databaseSource = get(),
            outbox = get(),
            applier = get(),
            deviceIdProvider = { identity.current().id },
            sendRequest = rest::sync,
            prepareImages = get<RoomMnemonicImageUploader>()::prepare,
            fetchBootstrap = rest::bootstrap,
            confirmAppliedRevision = { email, deviceId, revision ->
                rest.confirmAppliedRevision(email, deviceId, revision)
                Unit
            },
        )
    }
    single<IDeckReviewResultRepository> {
        val databaseSource = get<RoomDatabaseSource>()
        if (databaseSource is ActiveLocalRoomDatabase) {
            RoomDeckReviewResultRepository(databaseSource)
        } else {
            LegacyDeckReviewResultRepository(updateDeck = get(), saveDeckReviewInfo = get())
        }
    }
}

private fun Module.commonUseCaseModule() {
    commonVocabularySourceUseCaseModule()

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
            localMnemonicImageAssetRepository = get(),
            remoteDeckRepository = get(qualifier = named(name = REMOTE_DECK_REPOSITORY)),
            remoteCardRepository = get(qualifier = named(name = REMOTE_CARD_REPOSITORY)),
            remoteStorageSaveVersionRepository = get(
                qualifier = named(name = REMOTE_STORAGE_SAVE_VERSION_REPOSITORY),
            ),
            remoteMnemonicImageRepository = get(),
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
        FetchAllCardsUseCase(
            cardRepository = get(qualifier = named(name = LOCAL_CARD_REPOSITORY)),
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
        FetchMnemonicAssociationUseCase(
            mnemonicAssociationRepository = get(),
            coroutineContextProvider = get(),
        )
    }
    factory {
        FetchMnemonicImageUseCase(
            mnemonicImageRepository = get(),
            coroutineContextProvider = get(),
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
        FetchWordMeaningInsightsUseCase(
            wordMeaningInsightsRepository = get(),
            coroutineContextProvider = get(),
        )
    }
    factory { ObserveKlafServerConnectionStateUseCase(klafServerConnectionManager = get()) }
    factory {
        RetryKlafServerConnectionUseCase(
            klafServerConnectionManager = get(),
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
        SaveCompletedDeckReviewUseCase(repository = get(), coroutineContextProvider = get())
    }
    factory {
        SynchronizeLocalAndRemoteDataUseCase(
            localDeckRepository = get(qualifier = named(name = LOCAL_DECK_REPOSITORY)),
            localCardRepository = get(qualifier = named(name = LOCAL_CARD_REPOSITORY)),
            localStorageSaveVersionRepository = get(
                qualifier = named(name = LOCAL_STORAGE_SAVE_VERSION_REPOSITORY),
            ),
            localMnemonicImageAssetRepository = get(),
            remoteDeckRepository = get(qualifier = named(name = REMOTE_DECK_REPOSITORY)),
            remoteCardRepository = get(qualifier = named(name = REMOTE_CARD_REPOSITORY)),
            remoteStorageSaveVersionRepository = get(
                qualifier = named(name = REMOTE_STORAGE_SAVE_VERSION_REPOSITORY),
            ),
            remoteMnemonicImageRepository = get<IMnemonicImageRemoteRepository>(),
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

private fun Module.commonVocabularySourceUseCaseModule() {
    factory {
        AnalyzeVocabularySourceTextUseCase(
            vocabularySourceAnalysisRepository = get(),
            coroutineContextProvider = get(),
        )
    }
    factory {
        AddVocabularySourceItemsToDeckUseCase(
            cardRepository = get(qualifier = named(name = LOCAL_CARD_REPOSITORY)),
            deckRepository = get(qualifier = named(name = LOCAL_DECK_REPOSITORY)),
            vocabularySourceRepository = get(),
            localStorageSaveVersionRepository = get(
                qualifier = named(name = LOCAL_STORAGE_SAVE_VERSION_REPOSITORY),
            ),
            localStorageTransactionRepository = get(),
            coroutineContextProvider = get(),
        )
    }
    factory {
        CreateVocabularySourceUseCase(
            vocabularySourceRepository = get(),
            localStorageSaveVersionRepository = get(
                qualifier = named(name = LOCAL_STORAGE_SAVE_VERSION_REPOSITORY),
            ),
            localStorageTransactionRepository = get(),
            coroutineContextProvider = get(),
        )
    }
    factory {
        FetchVocabularySourceByIdUseCase(
            vocabularySourceRepository = get(),
            coroutineContextProvider = get(),
        )
    }
    factory {
        FetchVocabularySourceItemsUseCase(
            vocabularySourceRepository = get(),
            coroutineContextProvider = get(),
        )
    }
    factory {
        FetchVocabularySourcesUseCase(
            vocabularySourceRepository = get(),
            coroutineContextProvider = get(),
        )
    }
    factory {
        FetchIgnoredVocabularyWordsUseCase(
            ignoredVocabularyWordRepository = get(),
            coroutineContextProvider = get(),
        )
    }
    factory { ObserveVocabularySourceByIdUseCase(vocabularySourceRepository = get()) }
    factory { ObserveAllVocabularySourceItemsUseCase(vocabularySourceRepository = get()) }
    factory { ObserveVocabularySourceItemsUseCase(vocabularySourceRepository = get()) }
    factory { ObserveVocabularySourcesUseCase(vocabularySourceRepository = get()) }
    factory {
        RemoveVocabularySourceUseCase(
            vocabularySourceRepository = get(),
            localStorageSaveVersionRepository = get(
                qualifier = named(name = LOCAL_STORAGE_SAVE_VERSION_REPOSITORY),
            ),
            localStorageTransactionRepository = get(),
            coroutineContextProvider = get(),
        )
    }
    factory {
        SaveVocabularySourceItemsUseCase(
            vocabularySourceRepository = get(),
            localStorageSaveVersionRepository = get(
                qualifier = named(name = LOCAL_STORAGE_SAVE_VERSION_REPOSITORY),
            ),
            localStorageTransactionRepository = get(),
            coroutineContextProvider = get(),
        )
    }
    factory {
        SaveVocabularySourceChangesUseCase(
            vocabularySourceRepository = get(),
            ignoredVocabularyWordRepository = get(),
            localStorageSaveVersionRepository = get(
                qualifier = named(name = LOCAL_STORAGE_SAVE_VERSION_REPOSITORY),
            ),
            localStorageTransactionRepository = get(),
            coroutineContextProvider = get(),
        )
    }
    factory {
        ReplaceVocabularySourceDraftItemsUseCase(
            vocabularySourceRepository = get(),
            localStorageSaveVersionRepository = get(
                qualifier = named(name = LOCAL_STORAGE_SAVE_VERSION_REPOSITORY),
            ),
            localStorageTransactionRepository = get(),
            coroutineContextProvider = get(),
        )
    }
    factory {
        UpdateVocabularySourceUseCase(
            vocabularySourceRepository = get(),
            localStorageSaveVersionRepository = get(
                qualifier = named(name = LOCAL_STORAGE_SAVE_VERSION_REPOSITORY),
            ),
            localStorageTransactionRepository = get(),
            coroutineContextProvider = get(),
        )
    }
    factory {
        TranscribeVocabularySourceAudioUseCase(
            repository = get(),
        )
    }
}
