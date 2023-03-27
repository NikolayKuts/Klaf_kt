package com.example.domain.repositories

import com.example.domain.common.LoadingState
import kotlinx.coroutines.flow.Flow

interface AuthenticationRepository {

    fun signInWithEmailAndPassword(email: String, password: String): Flow<LoadingState<Unit>>

    fun signUpWithEmailAndPassword(email: String, password: String): Flow<LoadingState<Unit>>
}