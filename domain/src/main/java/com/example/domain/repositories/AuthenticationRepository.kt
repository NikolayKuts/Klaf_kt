package com.example.domain.repositories

import com.example.domain.common.AuthenticationAction
import com.example.domain.common.LoadingState
import kotlinx.coroutines.flow.Flow

interface AuthenticationRepository {

    fun signInWithEmailAndPassword(
        email: String,
        password: String,
    ): Flow<LoadingState<AuthenticationAction>>

    fun signUpWithEmailAndPassword(
        email: String,
        password: String,
    ): Flow<LoadingState<AuthenticationAction>>
}