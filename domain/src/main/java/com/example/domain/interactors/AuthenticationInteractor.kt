package com.example.domain.interactors

import com.example.domain.common.AuthenticationAction
import com.example.domain.common.LoadingState
import com.example.domain.repositories.AuthenticationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AuthenticationInteractor @Inject constructor(
    private val authRepository: AuthenticationRepository
) {

    fun signInWithEmailAndPassword(
        email: String,
        password: String,
    ): Flow<LoadingState<AuthenticationAction>> {
        return authRepository.signInWithEmailAndPassword(email = email, password = password)
    }

    fun signUpWithEmailAndPassword(
        email: String,
        password: String,
    ): Flow<LoadingState<AuthenticationAction>> {
        return authRepository.signUpWithEmailAndPassword(email = email, password = password)
    }
}