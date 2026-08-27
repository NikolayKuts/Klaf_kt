package com.kuts.klaf.di

import com.kuts.domain.managers.IDeckReviewNotifierManager
import com.kuts.klaf.cardManagement.common.ICambridgeWordDataProvider
import com.kuts.klaf.common.permissions.IMicrophonePermissionManager
import com.kuts.klaf.ios.IosNoOpCambridgeWordDataProvider
import com.kuts.klaf.ios.IosNoOpDeckReviewNotifier
import com.kuts.klaf.ios.IosNoOpMicrophonePermissionManager
import org.koin.dsl.module

internal val iosPresentationModule = module {
    single<IAppPreferencesDataStoreFactory> { IosAppPreferencesDataStoreFactory() }
    factory<IDeckReviewStateStoreFactory> { IosDeckReviewStateStoreFactory() }
    single<IDeckReviewNotifierManager> { IosNoOpDeckReviewNotifier() }
    single<ICambridgeWordDataProvider> { IosNoOpCambridgeWordDataProvider() }
    single<IMicrophonePermissionManager> { IosNoOpMicrophonePermissionManager() }
}
