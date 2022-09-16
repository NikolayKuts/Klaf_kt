package com.example.klaf.data.room.repositoryImplementations

import com.example.klaf.data.room.databases.KlafRoomDatabase
import com.example.klaf.data.room.entities.RoomStorageSaveVersion
import com.example.klaf.data.room.mapToDomainEntity
import com.example.klaf.data.room.mapToRoomEntity
import com.example.klaf.domain.entities.StorageSaveVersion
import com.example.klaf.domain.repositories.StorageSaveVersionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class StorageSaveVersionRepositoryRoomImp @Inject constructor(
    private val database: KlafRoomDatabase
) : StorageSaveVersionRepository {

    override suspend fun fetchVersion(): StorageSaveVersion? = withContext(Dispatchers.IO) {
        database.storageSaveVersionDao()
            .getStorageSaveVersion()
            ?.mapToDomainEntity()
    }

    override suspend fun insertVersion(version: StorageSaveVersion) {
        withContext(Dispatchers.IO) {
            database.storageSaveVersionDao()
                .insertStorageSaveVersion(saveVersion = version.mapToRoomEntity())
        }
    }

    override suspend fun increaseVersion() {
        withContext(Dispatchers.IO) {
            val oldVersion = fetchVersion()?.version ?: StorageSaveVersion.INITIAL_SAVE_VERSION

            database.storageSaveVersionDao()
                .insertStorageSaveVersion(
                    saveVersion = RoomStorageSaveVersion(version = oldVersion + 1)
                )
        }
    }
}