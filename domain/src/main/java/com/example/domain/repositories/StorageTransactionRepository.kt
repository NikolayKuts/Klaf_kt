package com.example.domain.repositories

interface StorageTransactionRepository {

    suspend fun <R> performWithTransaction(block: suspend () -> R)
}