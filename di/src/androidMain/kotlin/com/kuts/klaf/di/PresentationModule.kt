package com.kuts.klaf.di

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import com.kuts.domain.managers.IDeckReviewNotifierManager
import com.kuts.klaf.common.localStore.AppLocalStore
import com.kuts.klaf.common.notifications.DeckReviewNotifier
import com.kuts.klaf.common.permissions.INotificationPermissionBinder
import com.kuts.klaf.common.permissions.INotificationPermissionManager
import com.kuts.klaf.common.permissions.MokoNotificationPermissionManager
import com.kuts.klaf.deckRepetition.savedStateHandle.DeckReviewSavedStateHandleStateStore
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

internal val presentationModule = module {
    single {
        DeckReviewNotifier(
            context = androidContext(),
            notificationManager = get(),
        )
    }
    single<IDeckReviewNotifierManager> { get<DeckReviewNotifier>() }
    single {
        AppLocalStore(
            dataStore = PreferenceDataStoreFactory.create(
                produceFile = {
                    androidContext().preferencesDataStoreFile(AppLocalStore.DATA_STORE_NAME)
                },
            ),
        )
    }
    single { MokoNotificationPermissionManager(context = androidContext(), appLocalStore = get()) }
    single<INotificationPermissionManager> { get<MokoNotificationPermissionManager>() }
    single<INotificationPermissionBinder> { get<MokoNotificationPermissionManager>() }

    registerCommonPresentationViewModels(
        deckReviewStateStoreFactory = { DeckReviewSavedStateHandleStateStore(handle = get()) },
    )
}
