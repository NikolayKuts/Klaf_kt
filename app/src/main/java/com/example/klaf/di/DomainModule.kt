package com.example.klaf.di

import com.example.klaf.data.common.OldAppKlafDataTransferRepositoryImpl
import com.example.klaf.data.firestore.repositoryImplementations.DeckRepositoryFirestoreImp
import com.example.klaf.data.room.repositoryImplementations.CardRepositoryRoomImpl
import com.example.klaf.data.room.repositoryImplementations.DeckRepositoryRoomImp
import com.example.klaf.domain.repositories.CardRepository
import com.example.klaf.domain.repositories.DeckRepository
import com.example.klaf.domain.repositories.OldAppKlafDataTransferRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier

@Qualifier
annotation class DeckRepositoryRoomImp

@Qualifier
annotation class DeckRepositoryFirestoreImp

@Module
@InstallIn(SingletonComponent::class)
interface DomainModule {

    @Binds
    @com.example.klaf.di.DeckRepositoryRoomImp
    fun bindRoomDeckRepository(repository: DeckRepositoryRoomImp): DeckRepository

    @Binds
    fun bindCardRepository(repository: CardRepositoryRoomImpl): CardRepository

    @Binds
    @com.example.klaf.di.DeckRepositoryFirestoreImp
    fun bindFirestoreDeckRepository(repository: DeckRepositoryFirestoreImp): DeckRepository

    @Binds
    fun bindOldAppKlafTransferRepository(
        repository: OldAppKlafDataTransferRepositoryImpl
    ) : OldAppKlafDataTransferRepository
}