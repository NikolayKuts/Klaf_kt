package com.kuts.klaf.data.room.repositoryImplementations

import androidx.room.withTransaction
import com.kuts.klaf.data.room.databases.KlafRoomDatabase
import com.kuts.domain.repositories.IStorageTransactionRepository
import javax.inject.Inject

class StorageTransactionRepositoryRoom @Inject constructor(
    private val roomDatabase: KlafRoomDatabase,
) : IStorageTransactionRepository {

    override suspend fun <R> performWithTransaction(block: suspend () -> R) {
        roomDatabase.withTransaction(block = block)
    }
}