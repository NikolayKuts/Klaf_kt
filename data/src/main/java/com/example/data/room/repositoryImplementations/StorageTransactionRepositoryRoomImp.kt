package com.example.data.room.repositoryImplementations

import androidx.room.withTransaction
import com.example.data.room.databases.KlafRoomDatabase
import com.example.domain.repositories.StorageTransactionRepository
import javax.inject.Inject

class StorageTransactionRepositoryRoomImp @Inject constructor(
    private val roomDatabase: KlafRoomDatabase,
) : StorageTransactionRepository {

    override suspend fun <R> performWithTransaction(block: suspend () -> R) {
        roomDatabase.withTransaction(block = block)
    }
}