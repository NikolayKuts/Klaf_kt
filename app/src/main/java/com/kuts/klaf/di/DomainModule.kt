package com.kuts.klaf.di

import com.kuts.domain.repositories.*
import com.kuts.klaf.data.common.OldAppKlafDataTransferRepositoryImpl
import com.kuts.klaf.data.dataStore.implementations.DataStoreDeckRepetitionInfoRepositoryImpl
import com.kuts.klaf.data.firestore.repositoryImplementations.*
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
        repositoryImp: NativeWordSuggestionRepositoryImp
    ): NativeWordSuggestionRepository
}