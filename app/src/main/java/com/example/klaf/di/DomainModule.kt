package com.example.klaf.di

import com.example.klaf.data.common.OldAppKlafDataTransferRepositoryImpl
import com.example.klaf.data.dataStore.implementations.DataStoreDeckRepetitionInfoRepositoryImpl
import com.example.klaf.data.firestore.repositoryImplementations.DeckRepositoryFirestoreImp
import com.example.klaf.data.firestore.repositoryImplementations.CardRepositoryFirestoreImp
import com.example.klaf.data.firestore.repositoryImplementations.StorageSaveVersionRepositoryFirestoreImp
import com.example.klaf.data.firestore.repositoryImplementations.WordAutocompleteFirestoreImp
import com.example.klaf.data.room.repositoryImplementations.CardRepositoryRoomImp
import com.example.klaf.data.room.repositoryImplementations.DeckRepositoryRoomImp
import com.example.klaf.data.room.repositoryImplementations.StorageSaveVersionRepositoryRoomImp
import com.example.klaf.data.room.repositoryImplementations.StorageTransactionRepositoryRoomImp
import com.example.klaf.domain.repositories.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier

@Qualifier
annotation class DeckRepositoryRoomImp

@Qualifier
annotation class DeckRepositoryFirestoreImp

@Qualifier
annotation class CardRepositoryRoomImp

@Qualifier
annotation class CardRepositoryFirestoreImp

@Qualifier
annotation class StorageSaveVersionRepositoryRoomImp

@Qualifier
annotation class StorageSaveVersionRepositoryFirestoreImp

@Module
@InstallIn(SingletonComponent::class)
interface DomainModule {

    @Binds
    @com.example.klaf.di.DeckRepositoryRoomImp
    fun bindRoomDeckRepository(repository: DeckRepositoryRoomImp): DeckRepository

    @Binds
    @com.example.klaf.di.CardRepositoryRoomImp
    fun bindRoomCardRepository(repository: CardRepositoryRoomImp): CardRepository

    @Binds
    @com.example.klaf.di.DeckRepositoryFirestoreImp
    fun bindFirestoreDeckRepository(repository: DeckRepositoryFirestoreImp): DeckRepository

    @Binds
    @com.example.klaf.di.CardRepositoryFirestoreImp
    fun bindFirestoreCardRepository(repository: CardRepositoryFirestoreImp): CardRepository

    @Binds
    @com.example.klaf.di.StorageSaveVersionRepositoryRoomImp
    fun bindRoomStorageSaveVersionRepository(
        repository: StorageSaveVersionRepositoryRoomImp
    ): StorageSaveVersionRepository

    @Binds
    @com.example.klaf.di.StorageSaveVersionRepositoryFirestoreImp
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
    ) : WordAutocompleteRepository
}