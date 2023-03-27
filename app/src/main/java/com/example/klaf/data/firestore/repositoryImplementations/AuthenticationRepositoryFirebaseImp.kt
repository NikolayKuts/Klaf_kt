package com.example.klaf.data.firestore.repositoryImplementations

import com.example.domain.common.LoadingError
import com.example.domain.common.LoadingState
import com.example.domain.repositories.AuthenticationRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthenticationRepositoryFirebaseImp @Inject constructor(
    private val auth: FirebaseAuth,
) : AuthenticationRepository {

    sealed interface FirebaseLoadingError : LoadingError {

        object NoUserRecord : FirebaseLoadingError

        object InvalidPassword : FirebaseLoadingError

        object NetworkError : FirebaseLoadingError

        object CommonError : FirebaseLoadingError
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
            is FirebaseAuthInvalidUserException -> FirebaseLoadingError.NoUserRecord
            is FirebaseAuthInvalidCredentialsException -> FirebaseLoadingError.InvalidPassword
            is FirebaseNetworkException -> FirebaseLoadingError.NetworkError
            else -> FirebaseLoadingError.CommonError
        }

        emit(LoadingState.Error(value = errorType))
    }

    override fun signUpUserWithEmailAndPassword(): Flow<LoadingState<Unit>> = flow {
//        auth.createUserWithEmailAndPassword(email, password).await()
    }
}