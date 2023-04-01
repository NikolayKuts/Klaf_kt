package com.example.klaf.data.firestore.repositoryImplementations

import com.example.domain.common.LoadingError
import com.example.domain.common.LoadingState
import com.example.domain.repositories.AuthenticationRepository
import com.example.klaf.data.firestore.repositoryImplementations.AuthenticationRepositoryFirebaseImp.SigningUpLoadingError.*
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthenticationRepositoryFirebaseImp @Inject constructor(
    private val auth: FirebaseAuth,
) : AuthenticationRepository {

    sealed interface SigningInLoadingError : LoadingError {

        object NoUserRecord : SigningInLoadingError

        object InvalidPassword : SigningInLoadingError

        object NetworkError : SigningInLoadingError

        object CommonError : SigningInLoadingError
    }

    sealed interface SigningUpLoadingError : LoadingError {

        object EmailAlreadyInUse : SigningUpLoadingError

        object NetworkError : SigningUpLoadingError

        object CommonError : SigningUpLoadingError
    }

    override fun signInWithEmailAndPassword(
        email: String,
        password: String
    ): Flow<LoadingState<Unit>> = flow {
        emit(LoadingState.Loading)
        auth.signInWithEmailAndPassword(email, password).await()
        emit(LoadingState.Success(data = Unit))
    }.catch { error ->
        val errorType = when (error) {
            is FirebaseAuthInvalidUserException -> SigningInLoadingError.NoUserRecord
            is FirebaseAuthInvalidCredentialsException -> SigningInLoadingError.InvalidPassword
            is FirebaseNetworkException -> SigningInLoadingError.NetworkError
            else -> SigningInLoadingError.CommonError
        }

        emit(LoadingState.Error(value = errorType))
    }

    override fun signUpWithEmailAndPassword(
        email: String,
        password: String,
    ): Flow<LoadingState<Unit>> = flow {
        emit(LoadingState.Loading)
        auth.createUserWithEmailAndPassword(email, password).await()
        emit(LoadingState.Success(data = Unit))
    }.catch { error ->
        val errorType = when (error) {
            is FirebaseAuthUserCollisionException -> EmailAlreadyInUse
            is FirebaseNetworkException -> NetworkError
            else -> CommonError
        }

        emit(LoadingState.Error(value = errorType))
    }
}