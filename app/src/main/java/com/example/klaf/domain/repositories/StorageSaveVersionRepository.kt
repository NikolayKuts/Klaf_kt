package com.example.klaf.domain.repositories

import com.example.klaf.domain.entities.StorageSaveVersion

interface StorageSaveVersionRepository {

    suspend fun fetchVersion(): StorageSaveVersion?

    suspend fun insertVersion(version: StorageSaveVersion)

    suspend fun increaseVersion()
}