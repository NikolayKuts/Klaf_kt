package com.example.domain.repositories

import kotlinx.coroutines.flow.Flow

interface DataSynchronizationRepository {

    fun synchronize(): Flow<String>
}