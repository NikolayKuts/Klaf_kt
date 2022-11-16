package com.example.klaf.data.common

data class StorageSaveVersionValidationData(
    private val localStorageSaveVersion: Long?,
    private val remoteStorageSaveVersion: Long?,
) {

    companion object {

        const val UNDEFINED_SAVE_VERSION = -1L
    }

    val localSaveVersion: Long get() = localStorageSaveVersion ?: UNDEFINED_SAVE_VERSION
    val remoteSaveVersion: Long get() = remoteStorageSaveVersion ?: UNDEFINED_SAVE_VERSION
}
