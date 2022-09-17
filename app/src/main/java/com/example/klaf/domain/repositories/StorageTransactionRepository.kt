package com.example.klaf.domain.repositories

interface StorageTransactionRepository {

    suspend fun <R> performWithTransaction(block: suspend () -> R)
}