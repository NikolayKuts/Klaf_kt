package com.kuts.klaf.di

import com.kuts.domain.common.DataSynchronizationValidator
import com.kuts.domain.interactors.AuthenticationInteractor
import com.kuts.domain.repositories.ICardRepository
import com.kuts.domain.repositories.IDeckRepository
import com.kuts.domain.repositories.IStorageSaveVersionRepository
import com.kuts.domain.repositories.IStorageTransactionRepository
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
import com.kuts.domain.useCases.FetchWordMeaningInsightsUseCase
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
import com.kuts.klaf.room.repositoryImplementations.CardRepositoryRoom
import com.kuts.klaf.room.repositoryImplementations.DeckRepositoryRoom
import com.kuts.klaf.room.repositoryImplementations.StorageSaveVersionRepositoryRoom
import com.kuts.klaf.room.repositoryImplementations.StorageTransactionRepositoryRoom
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

    single<IStorageTransactionRepository> { StorageTransactionRepositoryRoom(roomDatabase = get()) }
}

private fun Module.commonUseCaseModule() {
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
        FetchWordMeaningInsightsUseCase(
            wordMeaningInsightsRepository = get(),
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
