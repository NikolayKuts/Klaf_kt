package com.kuts.klaf.di

import com.kuts.domain.managers.IDeckReviewNotifierManager
import com.kuts.klaf.cardManagement.common.ICambridgeWordDataProvider
import com.kuts.klaf.common.permissions.IMicrophonePermissionManager
import com.kuts.klaf.desktop.DesktopNoOpCambridgeWordDataProvider
import com.kuts.klaf.desktop.DesktopNoOpDeckReviewNotifier
import com.kuts.klaf.desktop.DesktopNoOpMicrophonePermissionManager
import org.koin.dsl.module

internal val desktopPresentationModule = module {
    single<IAppPreferencesDataStoreFactory> { DesktopAppPreferencesDataStoreFactory() }
    factory<IDeckReviewStateStoreFactory> { DesktopDeckReviewStateStoreFactory() }
    single<IDeckReviewNotifierManager> { DesktopNoOpDeckReviewNotifier() }
    single<ICambridgeWordDataProvider> { DesktopNoOpCambridgeWordDataProvider() }
    single<IMicrophonePermissionManager> { DesktopNoOpMicrophonePermissionManager() }
}
