package com.example.domain.repositories

import kotlinx.coroutines.flow.Flow

interface ObservationRepository<T> {

    fun observe(): Flow<T>
}