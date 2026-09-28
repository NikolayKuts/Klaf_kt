package com.kuts.klaf.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.kuts.klaf.common.localStore.AppLocalStore
import java.io.File
import okio.Path.Companion.toPath

internal class DesktopAppPreferencesDataStoreFactory(
    private val appDir: File = File(System.getProperty("user.home"), ".klaf_kt"),
) : IAppPreferencesDataStoreFactory {

    override fun create(): DataStore<Preferences> {
        return PreferenceDataStoreFactory.createWithPath(
            produceFile = { appLocalStoreFilePath().toPath() },
        )
    }

    private fun appLocalStoreFilePath(): String {
        if (!appDir.exists()) {
            appDir.mkdirs()
        }

        return File(appDir, "${AppLocalStore.DATA_STORE_NAME}.preferences_pb").absolutePath
    }
}
