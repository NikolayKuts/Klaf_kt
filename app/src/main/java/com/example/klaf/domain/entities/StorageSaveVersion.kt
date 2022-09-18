package com.example.klaf.domain.entities

data class StorageSaveVersion(val version: Long = INITIAL_SAVE_VERSION) {


    companion object {

        const val INITIAL_SAVE_VERSION = 0L
    }
}
