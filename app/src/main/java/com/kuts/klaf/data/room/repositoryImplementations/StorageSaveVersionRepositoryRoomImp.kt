package com.kuts.klaf.data.room.repositoryImplementations

import com.kuts.klaf.data.room.databases.KlafRoomDatabase
import com.kuts.klaf.data.room.entities.RoomStorageSaveVersion
import com.kuts.klaf.data.room.toDomainEntity
import com.kuts.klaf.data.room.toRoomEntity
import com.kuts.domain.entities.StorageSaveVersion
import com.kuts.domain.repositories.StorageSaveVersionRepository
import javax.inject.Inject

class StorageSaveVersionRepositoryRoomImp @Inject constructor(
    private val database: KlafRoomDatabase,
) : StorageSaveVersionRepository {

    override suspend fun fetchVersion(): StorageSaveVersion? {
        return database.storageSaveVersionDao()
            .getStorageSaveVersion()
            ?.toDomainEntity()
    }

    override suspend fun insertVersion(version: StorageSaveVersion) {
        database.storageSaveVersionDao()
            .insertStorageSaveVersion(saveVersion = version.toRoomEntity())
    }

    override suspend fun increaseVersion() {
        val oldVersion = fetchVersion()?.version ?: StorageSaveVersion.INITIAL_SAVE_VERSION

        database.storageSaveVersionDao()
            .insertStorageSaveVersion(
                saveVersion = RoomStorageSaveVersion(version = oldVersion + 1)
            )
    }
}