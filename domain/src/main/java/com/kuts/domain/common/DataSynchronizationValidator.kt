package com.kuts.domain.common

import com.kuts.domain.common.StorageSaveVersionValidationData
import com.kuts.domain.common.StorageSaveVersionValidationData.Companion.UNDEFINED_SAVE_VERSION
import javax.inject.Inject

class DataSynchronizationValidator @Inject constructor() {

    fun areSaveVersionUndefined(validationData: StorageSaveVersionValidationData): Boolean {
        return with(validationData) {
            (localSaveVersion == UNDEFINED_SAVE_VERSION)
                    && (remoteSaveVersion == UNDEFINED_SAVE_VERSION)
        }
    }

    fun shouldLocalStorageBeUpdated(validationData: StorageSaveVersionValidationData): Boolean {
        return with(validationData) {
            (localSaveVersion == UNDEFINED_SAVE_VERSION
                    && remoteSaveVersion != UNDEFINED_SAVE_VERSION)
                    || (localSaveVersion != UNDEFINED_SAVE_VERSION
                    && remoteSaveVersion != UNDEFINED_SAVE_VERSION
                    && localSaveVersion < remoteSaveVersion)
        }
    }

    fun shouldRemoteStorageBeUpdated(validationData: StorageSaveVersionValidationData): Boolean {
        return with(validationData) {
            (localSaveVersion != UNDEFINED_SAVE_VERSION
                    && remoteSaveVersion == UNDEFINED_SAVE_VERSION)
                    || (localSaveVersion != UNDEFINED_SAVE_VERSION
                    && remoteSaveVersion != UNDEFINED_SAVE_VERSION
                    && localSaveVersion > remoteSaveVersion)
        }
    }
}

