package com.kuts.klaf.common.localStore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.kuts.klaf.common.booleanValue

class AppLocalStore(
    private val dataStore: DataStore<Preferences>,
) {

    companion object {

        private const val KEY_NOTIFICATION_PERMISSION_REQUESTED_ONCE =
            "notification_permission_requested_once"
        const val DATA_STORE_NAME = "app_local_store"
    }

    var notificationPermissionRequestedOnce: Boolean by dataStore.booleanValue(
        keyName = KEY_NOTIFICATION_PERMISSION_REQUESTED_ONCE,
        defaultValue = false,
    )
}
