package com.kuts.domain.repositories

import com.kuts.domain.entities.StorageSaveVersion

interface StorageSaveVersionRepository {

    suspend fun fetchVersion(): StorageSaveVersion?

    suspend fun insertVersion(version: StorageSaveVersion)

    suspend fun increaseVersion()
}