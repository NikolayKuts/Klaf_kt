package com.kuts.klaf.data.firestore.repositoryImplementations

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.kuts.domain.common.AuthenticationAction
import com.kuts.domain.common.LoadingError
import com.kuts.domain.common.LoadingState
import com.kuts.domain.entities.AuthenticationState
import com.kuts.domain.repositories.AuthenticationRepository
import com.kuts.klaf.data.firestore.repositoryImplementations.AuthenticationRepositoryFirebaseImp.SigningUpLoadingError.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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

    sealed interface SigningOutLoadingError : LoadingError {

        object CommonError : SigningOutLoadingError
    }

    override val authenticationState: Flow<AuthenticationState> = callbackFlow {
        val authStateListener = AuthStateListener { firebaseAuth ->
            trySend(element = AuthenticationState(email = firebaseAuth.currentUser?.email))
        }

        auth.addAuthStateListener(authStateListener)

        awaitClose { auth.removeAuthStateListener(authStateListener) }
    }

    override fun signInWithEmailAndPassword(
        email: String,
        password: String,
    ): Flow<LoadingState<AuthenticationAction>> = flow {
        emit(LoadingState.Loading)
        auth.signInWithEmailAndPassword(
            email,
            password
        ).await()
        emit(LoadingState.Success(data = AuthenticationAction.SIGN_IN))
    }.catch { error ->
        val errorType = when (error) {
            is FirebaseAuthInvalidUserException -> SigningInLoadingError.NoUserRecord
            is FirebaseAuthInvalidCredentialsException -> SigningInLoadingError.InvalidPassword
            is FirebaseNetworkException -> SigningInLoadingError.NetworkError
            else -> SigningInLoadingError.CommonError
        }

        emit(value = LoadingState.Error(value = errorType))
    }

    override fun signUpWithEmailAndPassword(
        email: String,
        password: String,
    ): Flow<LoadingState<AuthenticationAction>> = flow {
        emit(LoadingState.Loading)
        auth.createUserWithEmailAndPassword(
            email,
            password
        ).await()
        emit(LoadingState.Success(data = AuthenticationAction.SIGN_UP))
    }.catch { error ->
        val errorType = when (error) {
            is FirebaseAuthUserCollisionException -> EmailAlreadyInUse
            is FirebaseNetworkException -> NetworkError
            else -> CommonError
        }

        emit(value = LoadingState.Error(value = errorType))
    }

    override fun signOut(): Flow<LoadingState<Unit>> = flow {
        emit(LoadingState.Loading)
        auth.signOut()
        emit(LoadingState.Success(data = Unit))
    }.catch {
        emit(value = LoadingState.Error(value = SigningOutLoadingError.CommonError))
    }

    override fun deleteProfile(): Flow<LoadingState<Unit>> = flow {
        auth.currentUser?.let { user ->
            emit(value = LoadingState.Loading)
            user.delete().await()
            emit(value = LoadingState.Success(data = Unit))
        }
    }.catch {
        TODO("Handle exceptions")
    }
}