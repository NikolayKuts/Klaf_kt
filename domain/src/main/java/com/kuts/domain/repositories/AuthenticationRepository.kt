package com.kuts.domain.repositories

import com.kuts.domain.common.AuthenticationAction
import com.kuts.domain.common.LoadingState
import com.kuts.domain.entities.AuthenticationState
import kotlinx.coroutines.flow.Flow

interface AuthenticationRepository {

    interface AuthenticationError

    val authenticationState: Flow<AuthenticationState>

    fun signInWithEmailAndPassword(
        email: String,
        password: String,
    ): Flow<LoadingState<AuthenticationAction, AuthenticationError>>

    fun signUpWithEmailAndPassword(
        email: String,
        password: String,
    ): Flow<LoadingState<AuthenticationAction, AuthenticationError>>

    fun signOut(): Flow<LoadingState<Unit, AuthenticationError>>

    fun deleteProfile(): Flow<LoadingState<Unit, AuthenticationError>>

    fun reauthenticateWithEmailAndPassword(
        email: String,
        password: String,
    ): Flow<LoadingState<AuthenticationAction, AuthenticationError>>
}