package com.kuts.klaf.di

import com.kuts.domain.managers.IDeckReviewNotifierManager
import com.kuts.klaf.cardManagement.common.ICambridgeWordDataProvider
import com.kuts.klaf.ios.IosInMemoryDeckReviewStateStore
import com.kuts.klaf.ios.IosNoOpCambridgeWordDataProvider
import com.kuts.klaf.ios.IosNoOpDeckReviewNotifier
import org.koin.dsl.module

internal val presentationModule = module {
    single<IDeckReviewNotifierManager> { IosNoOpDeckReviewNotifier() }
    single<ICambridgeWordDataProvider> { IosNoOpCambridgeWordDataProvider() }

    registerCommonPresentationViewModels(
        deckReviewStateStoreFactory = { IosInMemoryDeckReviewStateStore() },
    )
}
