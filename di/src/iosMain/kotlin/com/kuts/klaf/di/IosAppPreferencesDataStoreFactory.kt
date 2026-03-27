package com.kuts.klaf.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.kuts.klaf.common.localStore.AppLocalStore
import okio.Path.Companion.toPath
import platform.Foundation.NSHomeDirectory

internal class IosAppPreferencesDataStoreFactory : IAppPreferencesDataStoreFactory {

    override fun create(): DataStore<Preferences> {
        return PreferenceDataStoreFactory.createWithPath(
            produceFile = {
                "${NSHomeDirectory()}/${AppLocalStore.DATA_STORE_NAME}.preferences_pb".toPath()
            },
        )
    }
}
