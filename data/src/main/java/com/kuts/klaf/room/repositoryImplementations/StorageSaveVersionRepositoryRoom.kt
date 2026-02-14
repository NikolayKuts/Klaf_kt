package com.kuts.klaf.room.repositoryImplementations

import com.kuts.klaf.room.databases.KlafRoomDatabase
import com.kuts.klaf.room.entities.RoomStorageSaveVersion
import com.kuts.klaf.room.toDomainEntity
import com.kuts.klaf.room.toRoomEntity
import com.kuts.domain.entities.StorageSaveVersion
import com.kuts.domain.repositories.IStorageSaveVersionRepository
import javax.inject.Inject

class StorageSaveVersionRepositoryRoom @Inject constructor(
    private val database: KlafRoomDatabase,
) : IStorageSaveVersionRepository {

    override suspend fun fetchVersion(): StorageSaveVersion? {
        return database.storageSaveVersionDao()
            .getStorageSaveVersion()
            ?.toDomainEntity()
    }

    override suspend fun insertVersion(version: StorageSaveVersion) {
        database.storageSaveVersionDao()
            .insertStorageSaveVersion(saveVersion = version.toRoomEntity())
    }

    override suspend fun insertVersionAtPath(
        version: StorageSaveVersion,
        rootEmailPath: String
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun increaseVersion() {
        val oldVersion = fetchVersion()?.version ?: StorageSaveVersion.INITIAL_SAVE_VERSION

        database.storageSaveVersionDao()
            .insertStorageSaveVersion(
                saveVersion = RoomStorageSaveVersion(version = oldVersion + 1)
            )
    }
}