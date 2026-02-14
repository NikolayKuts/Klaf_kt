package com.kuts.klaf.di

import com.kuts.domain.repositories.IOldAppKlafDataTransferRepository
import com.kuts.klaf.common.OldAppKlafDataTransferRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface DataOldAppBindingsModule {

    @Binds
    fun bindOldAppKlafTransferRepository(
        repository: OldAppKlafDataTransferRepository,
    ): IOldAppKlafDataTransferRepository
}
