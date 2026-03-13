package com.kuts.klaf.di

import com.kuts.domain.managers.IDeckReviewNotifierManager
import com.kuts.klaf.cardManagement.common.ICambridgeWordDataProvider
import com.kuts.klaf.desktop.DesktopInMemoryDeckReviewStateStore
import com.kuts.klaf.desktop.DesktopNoOpCambridgeWordDataProvider
import com.kuts.klaf.desktop.DesktopNoOpDeckReviewNotifier
import org.koin.dsl.module

internal val presentationModule = module {
    single<IDeckReviewNotifierManager> { DesktopNoOpDeckReviewNotifier() }
    single<ICambridgeWordDataProvider> { DesktopNoOpCambridgeWordDataProvider() }

    registerCommonPresentationViewModels(
        deckReviewStateStoreFactory = { DesktopInMemoryDeckReviewStateStore() },
    )
}
