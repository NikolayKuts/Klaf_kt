package com.kuts.domain.repositories

import com.kuts.domain.common.AuthenticationAction
import com.kuts.domain.common.LoadingState
import com.kuts.domain.entities.AuthenticationState
import kotlinx.coroutines.flow.Flow

interface IAuthenticationRepository {

    sealed interface IAuthenticationError

    sealed interface ISigningInError : IAuthenticationError {
        data object NoUserRecord : ISigningInError
        data object InvalidPassword : ISigningInError
        data object NetworkError : ISigningInError
        data object CommonError : ISigningInError
    }

    sealed interface ISigningUpError : IAuthenticationError {
        data object EmailAlreadyInUse : ISigningUpError
        data object NetworkError : ISigningUpError
        data object CommonError : ISigningUpError
    }

    sealed interface ISigningOutError : IAuthenticationError {
        data object CommonError : ISigningOutError
    }

    sealed interface IAccountDeletingError : IAuthenticationError {
        data object CommonError : IAccountDeletingError
        data object NetworkError : IAccountDeletingError
        data object RecentLoginRequired : IAccountDeletingError
    }

    val authenticationState: Flow<AuthenticationState>

    fun signInWithEmailAndPassword(
        email: String,
        password: String,
    ): Flow<LoadingState<AuthenticationAction, IAuthenticationError>>

    fun signUpWithEmailAndPassword(
        email: String,
        password: String,
    ): Flow<LoadingState<AuthenticationAction, IAuthenticationError>>

    fun signOut(): Flow<LoadingState<Unit, IAuthenticationError>>

    fun deleteProfile(): Flow<LoadingState<Unit, IAuthenticationError>>

    fun reauthenticateWithEmailAndPassword(
        email: String,
        password: String,
    ): Flow<LoadingState<AuthenticationAction, IAuthenticationError>>
}
