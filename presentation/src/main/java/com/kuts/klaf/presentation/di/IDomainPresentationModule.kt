package com.kuts.klaf.presentation.di

import com.kuts.domain.managers.IDeckReviewNotifierManager
import com.kuts.klaf.presentation.common.notifications.DeckReviewNotifier
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface IDomainPresentationModule {

    @Binds
    fun bindDeckReviewNotifierManager(
        manager: DeckReviewNotifier,
    ): IDeckReviewNotifierManager
}
