package com.kuts.domain.entities

data class StorageSaveVersion(val version: Long = INITIAL_SAVE_VERSION) {

    companion object {

        const val INITIAL_SAVE_VERSION = 0L
    }
}
