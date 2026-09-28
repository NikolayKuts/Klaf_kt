package com.kuts.klaf.room.repositoryImplementations

import com.kuts.klaf.room.databases.KlafRoomDatabase
import com.kuts.klaf.room.databases.RoomDatabaseSource
import com.kuts.klaf.room.databases.StaticRoomDatabaseSource
import com.kuts.klaf.room.entities.RoomStorageSaveVersion
import com.kuts.klaf.room.toDomainEntity
import com.kuts.klaf.room.toRoomEntity
import com.kuts.domain.entities.StorageSaveVersion
import com.kuts.domain.repositories.IStorageSaveVersionRepository

class StorageSaveVersionRepositoryRoom(
    private val databaseSource: RoomDatabaseSource,
) : IStorageSaveVersionRepository {

    constructor(database: KlafRoomDatabase) : this(StaticRoomDatabaseSource(database))

    private val database: KlafRoomDatabase
        get() = databaseSource.current()

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
        val currentDatabase = database
        val oldVersion = currentDatabase.storageSaveVersionDao().getStorageSaveVersion()?.version
            ?: StorageSaveVersion.INITIAL_SAVE_VERSION

        currentDatabase.storageSaveVersionDao()
            .insertStorageSaveVersion(
                saveVersion = RoomStorageSaveVersion(version = oldVersion + 1)
            )
    }
}
