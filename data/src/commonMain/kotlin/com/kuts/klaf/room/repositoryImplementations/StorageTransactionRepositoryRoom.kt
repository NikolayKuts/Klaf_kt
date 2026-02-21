package com.kuts.klaf.room.repositoryImplementations

import com.kuts.klaf.room.databases.KlafRoomDatabase
import com.kuts.klaf.room.databases.performInTransaction
import com.kuts.domain.repositories.IStorageTransactionRepository

class StorageTransactionRepositoryRoom(
    private val roomDatabase: KlafRoomDatabase,
) : IStorageTransactionRepository {

    override suspend fun <R> performWithTransaction(block: suspend () -> R) {
        roomDatabase.performInTransaction(block = block)
    }
}
