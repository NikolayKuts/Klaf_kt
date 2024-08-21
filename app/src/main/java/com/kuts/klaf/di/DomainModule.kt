package com.kuts.klaf.di

import com.kuts.domain.repositories.AuthenticationRepository
import com.kuts.domain.repositories.CardRepository
import com.kuts.domain.repositories.CrashlyticsRepository
import com.kuts.domain.repositories.DeckRepetitionInfoRepository
import com.kuts.domain.repositories.DeckRepository
import com.kuts.domain.repositories.OldAppKlafDataTransferRepository
import com.kuts.domain.repositories.StorageSaveVersionRepository
import com.kuts.domain.repositories.StorageTransactionRepository
import com.kuts.domain.repositories.WordAutocompleteRepository
import com.kuts.domain.repositories.WordInfoRepository
import com.kuts.klaf.data.common.OldAppKlafDataTransferRepositoryImpl
import com.kuts.klaf.data.dataStore.implementations.DataStoreDeckRepetitionInfoRepositoryImpl
import com.kuts.klaf.data.firestore.repositoryImplementations.AuthenticationRepositoryFirebaseImp
import com.kuts.klaf.data.firestore.repositoryImplementations.CardRepositoryFirestoreImp
import com.kuts.klaf.data.firestore.repositoryImplementations.CrashlyticsRepositoryFirebaseImp
import com.kuts.klaf.data.firestore.repositoryImplementations.DeckRepositoryFirestoreImp
import com.kuts.klaf.data.firestore.repositoryImplementations.StorageSaveVersionRepositoryFirestoreImp
import com.kuts.klaf.data.firestore.repositoryImplementations.WordAutocompleteFirestoreImp
import com.kuts.klaf.data.networking.yandexApi.YandexWordInfoProvider
import com.kuts.klaf.data.room.repositoryImplementations.CardRepositoryRoomImp
import com.kuts.klaf.data.room.repositoryImplementations.DeckRepositoryRoomImp
import com.kuts.klaf.data.room.repositoryImplementations.StorageSaveVersionRepositoryRoomImp
import com.kuts.klaf.data.room.repositoryImplementations.StorageTransactionRepositoryRoomImp
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface DomainModule {

    @Binds
    @com.kuts.domain.common.LocalDeckRepositoryImp
    fun bindRoomDeckRepository(repository: DeckRepositoryRoomImp): DeckRepository

    @Binds
    @com.kuts.domain.common.LocalCardRepositoryImp
    fun bindRoomCardRepository(repository: CardRepositoryRoomImp): CardRepository

    @Binds
    @com.kuts.domain.common.RemoteDeckRepositoryImp
    fun bindFirestoreDeckRepository(repository: DeckRepositoryFirestoreImp): DeckRepository

    @Binds
    @com.kuts.domain.common.RemoteCardRepositoryImp
    fun bindFirestoreCardRepository(repository: CardRepositoryFirestoreImp): CardRepository

    @Binds
    @com.kuts.domain.common.LocalStorageSaveVersionRepositoryImp
    fun bindRoomStorageSaveVersionRepository(
        repository: StorageSaveVersionRepositoryRoomImp
    ): StorageSaveVersionRepository

    @Binds
    @com.kuts.domain.common.RemoteStorageSaveVersionRepositoryImp
    fun bindFirestoreStorageSaveVersionRepository(
        repository: StorageSaveVersionRepositoryFirestoreImp
    ): StorageSaveVersionRepository

    @Binds
    fun bindOldAppKlafTransferRepository(
        repository: OldAppKlafDataTransferRepositoryImpl,
    ): OldAppKlafDataTransferRepository

    @Binds
    fun bindStorageTransactionRepository(
        repository: StorageTransactionRepositoryRoomImp
    ): StorageTransactionRepository

    @Binds
    fun bindDeckRepetitionInfoRepository(
        repository: DataStoreDeckRepetitionInfoRepositoryImpl
    ): DeckRepetitionInfoRepository

    @Binds
    fun bindWordAutocompleteRepository(
        repository: WordAutocompleteFirestoreImp
    ): WordAutocompleteRepository

    @Binds
    fun bindAuthenticationRepository(
        repository: AuthenticationRepositoryFirebaseImp
    ): AuthenticationRepository

    @Binds
    fun bindCrashlyticsRepository(
        repository: CrashlyticsRepositoryFirebaseImp
    ): CrashlyticsRepository

    @Binds
    fun bindNativeWordSuggestionRepository(
        repositoryImp: YandexWordInfoProvider
    ): WordInfoRepository
}