package com.kuts.domain.repositories

interface IStorageTransactionRepository {

    suspend fun <R> performWithTransaction(block: suspend () -> R)
}