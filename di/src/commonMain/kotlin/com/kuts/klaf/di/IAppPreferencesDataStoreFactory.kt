package com.kuts.klaf.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

internal interface IAppPreferencesDataStoreFactory {

    fun create(): DataStore<Preferences>
}
