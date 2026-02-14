package com.kuts.klaf.di

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
import com.kuts.domain.managers.IAppMaintenanceManager
import com.kuts.klaf.data.common.AppMaintenanceManager
import com.kuts.klaf.data.common.OldAppKlafDataTransferRepository
import com.kuts.klaf.data.dataStore.implementations.DataStoreDeckRepetitionInfoRepository
import com.kuts.klaf.data.firestore.repositoryImplementations.AuthenticationRepositoryFirebase
import com.kuts.klaf.data.firestore.repositoryImplementations.CardRepositoryFirestore
import com.kuts.klaf.data.firestore.repositoryImplementations.CrashlyticsRepositoryFirebase
import com.kuts.klaf.data.firestore.repositoryImplementations.DeckRepositoryFirestore
import com.kuts.klaf.data.firestore.repositoryImplementations.StorageSaveVersionRepositoryFirestore
import com.kuts.klaf.data.firestore.repositoryImplementations.WordAutocompleteFirestore
import com.kuts.klaf.data.networking.yandexApi.YandexWordInfoProvider
import com.kuts.klaf.data.room.repositoryImplementations.CardRepositoryRoom
import com.kuts.klaf.data.room.repositoryImplementations.DeckRepositoryRoom
import com.kuts.klaf.data.room.repositoryImplementations.StorageSaveVersionRepositoryRoom
import com.kuts.klaf.data.room.repositoryImplementations.StorageTransactionRepositoryRoom
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface IDomainModule {

    @Binds
    fun bindAppMaintenanceManager(
        manager: AppMaintenanceManager,
    ): IAppMaintenanceManager

    @Binds
    @com.kuts.domain.common.LocalDeckRepository
    fun bindRoomDeckRepository(repository: DeckRepositoryRoom): IDeckRepository

    @Binds
    @com.kuts.domain.common.LocalCardRepository
    fun bindRoomCardRepository(repository: CardRepositoryRoom): ICardRepository

    @Binds
    @com.kuts.domain.common.RemoteDeckRepository
    fun bindFirestoreDeckRepository(repository: DeckRepositoryFirestore): IDeckRepository

    @Binds
    @com.kuts.domain.common.RemoteCardRepository
    fun bindFirestoreCardRepository(repository: CardRepositoryFirestore): ICardRepository

    @Binds
    @com.kuts.domain.common.LocalStorageSaveVersionRepository
    fun bindRoomStorageSaveVersionRepository(
        repository: StorageSaveVersionRepositoryRoom
    ): IStorageSaveVersionRepository

    @Binds
    @com.kuts.domain.common.RemoteStorageSaveVersionRepository
    fun bindFirestoreStorageSaveVersionRepository(
        repository: StorageSaveVersionRepositoryFirestore
    ): IStorageSaveVersionRepository

    @Binds
    fun bindOldAppKlafTransferRepository(
        repository: OldAppKlafDataTransferRepository,
    ): IOldAppKlafDataTransferRepository

    @Binds
    fun bindStorageTransactionRepository(
        repository: StorageTransactionRepositoryRoom
    ): IStorageTransactionRepository

    @Binds
    fun bindDeckRepetitionInfoRepository(
        repository: DataStoreDeckRepetitionInfoRepository
    ): IDeckRepetitionInfoRepository

    @Binds
    fun bindWordAutocompleteRepository(
        repository: WordAutocompleteFirestore
    ): IWordAutocompleteRepository

    @Binds
    fun bindAuthenticationRepository(
        repository: AuthenticationRepositoryFirebase
    ): IAuthenticationRepository

    @Binds
    fun bindCrashlyticsRepository(
        repository: CrashlyticsRepositoryFirebase
    ): ICrashlyticsRepository

    @Binds
    fun bindNativeWordSuggestionRepository(
        repositoryImp: YandexWordInfoProvider
    ): IWordInfoRepository
}
