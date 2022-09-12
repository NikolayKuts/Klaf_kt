package com.example.klaf.domain.repositories

interface StorageSaveVersionRepository {

    suspend fun fetchVersion(): Long?

    suspend fun insertVersion(version: Long)
}