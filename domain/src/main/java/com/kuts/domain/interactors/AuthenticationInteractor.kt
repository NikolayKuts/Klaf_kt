package com.kuts.domain.interactors

import com.kuts.domain.common.AuthenticationAction
import com.kuts.domain.common.LoadingState
import com.kuts.domain.entities.AuthenticationState
import com.kuts.domain.repositories.IAuthenticationRepository
import com.kuts.domain.repositories.IAuthenticationRepository.IAuthenticationError
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AuthenticationInteractor @Inject constructor(
    private val authRepository: IAuthenticationRepository,
) {

    fun getObservableAuthenticationState(): Flow<AuthenticationState> {
        return authRepository.authenticationState
    }

    fun signInWithEmailAndPassword(
        email: String,
        password: String,
    ): Flow<LoadingState<AuthenticationAction, IAuthenticationError>> {
        return authRepository.signInWithEmailAndPassword(email = email, password = password)
    }

    fun signUpWithEmailAndPassword(
        email: String,
        password: String,
    ): Flow<LoadingState<AuthenticationAction, IAuthenticationError>> {
        return authRepository.signUpWithEmailAndPassword(email = email, password = password)
    }

    fun logOut(): Flow<LoadingState<Unit, IAuthenticationError>> = authRepository.signOut()

    fun deleteAccount(): Flow<LoadingState<Unit, IAuthenticationError>> = authRepository.deleteProfile()
}