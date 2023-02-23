package com.example.klaf.di

import com.example.data.common.OldAppKlafDataTransferRepositoryImpl
import com.example.data.dataStore.implementations.DataStoreDeckRepetitionInfoRepositoryImpl
import com.example.data.firestore.repositoryImplementations.CardRepositoryFirestoreImp
import com.example.data.firestore.repositoryImplementations.DeckRepositoryFirestoreImp
import com.example.data.firestore.repositoryImplementations.StorageSaveVersionRepositoryFirestoreImp
import com.example.data.firestore.repositoryImplementations.WordAutocompleteFirestoreImp
import com.example.data.room.repositoryImplementations.CardRepositoryRoomImp
import com.example.data.room.repositoryImplementations.DeckRepositoryRoomImp
import com.example.data.room.repositoryImplementations.StorageSaveVersionRepositoryRoomImp
import com.example.data.room.repositoryImplementations.StorageTransactionRepositoryRoomImp
import com.example.domain.repositories.*
import com.example.klaf.presentation.common.notifications.DeckRepetitionNotifier
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface DomainModule {

    @Binds
    @com.example.domain.common.LocalDeckRepositoryImp
    fun bindRoomDeckRepository(repository: DeckRepositoryRoomImp): DeckRepository

    @Binds
    @com.example.domain.common.LocalCardRepositoryImp
    fun bindRoomCardRepository(repository: CardRepositoryRoomImp): CardRepository

    @Binds
    @com.example.domain.common.RemoteDeckRepositoryImp
    fun bindFirestoreDeckRepository(repository: DeckRepositoryFirestoreImp): DeckRepository

    @Binds
    @com.example.domain.common.RemoteCardRepositoryImp
    fun bindFirestoreCardRepository(repository: CardRepositoryFirestoreImp): CardRepository

    @Binds
    @com.example.domain.common.LocalStorageSaveVersionRepositoryImp
    fun bindRoomStorageSaveVersionRepository(
        repository: StorageSaveVersionRepositoryRoomImp
    ): StorageSaveVersionRepository

    @Binds
    @com.example.domain.common.RemoteStorageSaveVersionRepositoryImp
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
    fun bindDeckRepetitionNotifierRepository(
        notifier: DeckRepetitionNotifier
    ): DeckRepetitionNotifierRepository
}