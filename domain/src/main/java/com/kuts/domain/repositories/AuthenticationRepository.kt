package com.kuts.domain.repositories

import com.kuts.domain.common.AuthenticationAction
import com.kuts.domain.common.LoadingState
import com.kuts.domain.entities.AuthenticationState
import kotlinx.coroutines.flow.Flow

interface AuthenticationRepository {

    val authenticationState: Flow<AuthenticationState>

    fun signInWithEmailAndPassword(
        email: String,
        password: String,
    ): Flow<LoadingState<AuthenticationAction>>

    fun signUpWithEmailAndPassword(
        email: String,
        password: String,
    ): Flow<LoadingState<AuthenticationAction>>

    fun signOut(): Flow<LoadingState<Unit>>

    fun deleteProfile(): Flow<LoadingState<Unit>>

    fun reauthenticateWithEmailAndPassword(
        email: String,
        password: String,
    ): Flow<LoadingState<AuthenticationAction>>
}