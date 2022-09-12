package com.example.klaf.data.room.repositoryImplementations

import com.example.klaf.data.room.databases.KlafRoomDatabase
import com.example.klaf.data.room.entities.StorageSaveVersion
import com.example.klaf.domain.repositories.StorageSaveVersionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class StorageSaveVersionRepositoryRoomImp @Inject constructor(
    private val database: KlafRoomDatabase
) : StorageSaveVersionRepository {

    override suspend fun fetchVersion(): Long? = withContext(Dispatchers.IO) {
        database.storageSaveVersionDao().getStorageSaveVersion()?.version
    }

    override suspend fun insertVersion(version: Long) {
        withContext(Dispatchers.IO) {
            database.storageSaveVersionDao()
                .insertStorageSaveVersion(saveVersion = StorageSaveVersion(version = version))
        }
    }
}