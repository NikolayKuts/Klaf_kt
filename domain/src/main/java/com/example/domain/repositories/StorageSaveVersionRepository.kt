package com.example.domain.repositories

import com.example.domain.entities.StorageSaveVersion

interface StorageSaveVersionRepository {

    suspend fun fetchVersion(): StorageSaveVersion?

    suspend fun insertVersion(version: StorageSaveVersion)

    suspend fun increaseVersion()
}