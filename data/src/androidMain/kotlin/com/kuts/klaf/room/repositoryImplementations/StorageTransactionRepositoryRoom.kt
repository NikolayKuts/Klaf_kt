package com.kuts.klaf.room.repositoryImplementations

import androidx.room.withTransaction
import com.kuts.klaf.room.databases.KlafRoomDatabase
import com.kuts.domain.repositories.IStorageTransactionRepository

class StorageTransactionRepositoryRoom(
    private val roomDatabase: KlafRoomDatabase,
) : IStorageTransactionRepository {

    override suspend fun <R> performWithTransaction(block: suspend () -> R) {
        roomDatabase.withTransaction(block = block)
    }
}
