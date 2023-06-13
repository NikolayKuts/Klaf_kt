package com.kuts.klaf.data.firestore.repositoryImplementations

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.*
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import com.kuts.domain.common.AuthenticationAction
import com.kuts.domain.common.LoadingError
import com.kuts.domain.common.LoadingState
import com.kuts.domain.common.catchWithCrashlyticsReport
import com.kuts.domain.entities.AuthenticationState
import com.kuts.domain.repositories.AuthenticationRepository
import com.kuts.domain.repositories.CrashlyticsRepository
import com.kuts.klaf.data.firestore.repositoryImplementations.AuthenticationRepositoryFirebaseImp.SigningUpLoadingError.*
import com.kuts.klaf.presentation.common.log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthenticationRepositoryFirebaseImp @Inject constructor(
    private val auth: FirebaseAuth,
    private val crashlytics: CrashlyticsRepository,
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

    sealed interface AccountDeletingError : LoadingError {

        object CommonError : AccountDeletingError

        object NetworkError : AccountDeletingError

        object RecentLoginRequired : AccountDeletingError
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
    }.catchWithCrashlyticsReport(crashlytics = crashlytics) { error ->
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
    }.catchWithCrashlyticsReport(crashlytics = crashlytics) { error ->
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
    }.catchWithCrashlyticsReport(crashlytics = crashlytics) {
        emit(value = LoadingState.Error(value = SigningOutLoadingError.CommonError))
    }

    override fun deleteProfile(): Flow<LoadingState<Unit>> = flow {
        val user = auth.currentUser
            ?: throw RuntimeException("Trying user deleting when current user is null")

        emit(value = LoadingState.Loading)
        user.delete().await()
        emit(value = LoadingState.Success(data = Unit))
    }.catchWithCrashlyticsReport(crashlytics = crashlytics) { throwable ->
        val error = when (throwable) {
            is FirebaseNetworkException -> AccountDeletingError.NetworkError
            is FirebaseAuthRecentLoginRequiredException -> AccountDeletingError.RecentLoginRequired
            else -> AccountDeletingError.CommonError
        }

        emit(value = LoadingState.Error(value = error))
    }

    override fun reauthenticateWithEmailAndPassword(
        email: String,
        password: String,
    ): Flow<LoadingState<AuthenticationAction>> = flow {
//        auth.currentUser?.reauthenticate(
//            EmailAuthProvider.getCredential(
//                auth.currentUser?.email!!,
//                ""
//            )
//        )
    }
}