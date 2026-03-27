package com.kuts.klaf.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.kuts.klaf.common.localStore.AppLocalStore

internal class AndroidAppPreferencesDataStoreFactory(
    private val context: Context,
) : IAppPreferencesDataStoreFactory {

    override fun create(): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = {
            context.preferencesDataStoreFile(AppLocalStore.DATA_STORE_NAME)
        },
    )
}
