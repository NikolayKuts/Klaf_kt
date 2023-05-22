package com.kuts.domain.repositories

import com.kuts.domain.common.AuthenticationAction
import com.kuts.domain.common.LoadingState
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

    fun signOut() : Flow<LoadingState<Unit>>
}