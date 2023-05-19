package com.kuts.domain.repositories

interface StorageTransactionRepository {

    suspend fun <R> performWithTransaction(block: suspend () -> R)
}