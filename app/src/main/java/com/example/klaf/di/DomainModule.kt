package com.example.klaf.di

import com.example.klaf.data.OldAppKlafDataTransferRepositoryImpl
import com.example.klaf.data.room.repositoryImplementations.CardRepositoryRoomImpl
import com.example.klaf.data.room.repositoryImplementations.DeckRepositoryRoomImp
import com.example.klaf.domain.repositories.CardRepository
import com.example.klaf.domain.repositories.DeckRepository
import com.example.klaf.domain.repositories.OldAppKlafDataTransferRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface DomainModule {

    @Binds
    fun bindDeckRepository(repository: DeckRepositoryRoomImp): DeckRepository

    @Binds
    fun bindCardRepository(repository: CardRepositoryRoomImpl): CardRepository

    @Binds
    fun bindOldAppKlafTransferRepository(
        repository: OldAppKlafDataTransferRepositoryImpl
    ) : OldAppKlafDataTransferRepository
}