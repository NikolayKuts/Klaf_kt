package com.kuts.klaf.di

import com.kuts.domain.managers.IDeckReviewNotifierManager
import com.kuts.domain.managers.IAccountScopedDeckReviewNotifier
import com.kuts.klaf.common.notifications.AndroidDeckReviewNotifier
import com.kuts.klaf.common.permissions.AndroidMokoMicrophonePermissionManager
import com.kuts.klaf.common.permissions.IMicrophonePermissionBinder
import com.kuts.klaf.common.permissions.IMicrophonePermissionManager
import com.kuts.klaf.common.permissions.INotificationPermissionBinder
import com.kuts.klaf.common.permissions.INotificationPermissionManager
import com.kuts.klaf.common.permissions.AndroidMokoNotificationPermissionManager
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

internal val androidPresentationModule = module {
    single<IAppPreferencesDataStoreFactory> {
        AndroidAppPreferencesDataStoreFactory(context = androidContext())
    }
    factory<IDeckReviewStateStoreFactory> {
        AndroidDeckReviewStateStoreFactory(handle = get())
    }
    single {
        AndroidDeckReviewNotifier(
            context = androidContext(),
            notificationManager = get(),
            reminderScope = get(),
        )
    }
    single<IDeckReviewNotifierManager> { get<AndroidDeckReviewNotifier>() }
    single<IAccountScopedDeckReviewNotifier> { get<AndroidDeckReviewNotifier>() }
    single { AndroidMokoNotificationPermissionManager(context = androidContext(), appLocalStore = get()) }
    single<INotificationPermissionManager> { get<AndroidMokoNotificationPermissionManager>() }
    single<INotificationPermissionBinder> { get<AndroidMokoNotificationPermissionManager>() }
    single { AndroidMokoMicrophonePermissionManager(context = androidContext()) }
    single<IMicrophonePermissionManager> { get<AndroidMokoMicrophonePermissionManager>() }
    single<IMicrophonePermissionBinder> { get<AndroidMokoMicrophonePermissionManager>() }
}
