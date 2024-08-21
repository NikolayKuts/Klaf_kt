package com.kuts.klaf.data.firestore.repositoryImplementations

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.kuts.domain.common.AuthenticationAction
import com.kuts.domain.common.LoadingError
import com.kuts.domain.common.LoadingState
import com.kuts.domain.common.catchWithCrashlyticsReport
import com.kuts.domain.entities.AuthenticationState
import com.kuts.domain.repositories.AuthenticationRepository
import com.kuts.domain.repositories.CrashlyticsRepository
import com.kuts.klaf.data.firestore.repositoryImplementations.AuthenticationRepositoryFirebaseImp.SigningUpLoadingError.CommonError
import com.kuts.klaf.data.firestore.repositoryImplementations.AuthenticationRepositoryFirebaseImp.SigningUpLoadingError.EmailAlreadyInUse
import com.kuts.klaf.data.firestore.repositoryImplementations.AuthenticationRepositoryFirebaseImp.SigningUpLoadingError.NetworkError
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

        data object NoUserRecord : SigningInLoadingError

        data object InvalidPassword : SigningInLoadingError

        data object NetworkError : SigningInLoadingError

        data object CommonError : SigningInLoadingError
    }

    sealed interface SigningUpLoadingError : LoadingError {

        data object EmailAlreadyInUse : SigningUpLoadingError

        data object NetworkError : SigningUpLoadingError

        data object CommonError : SigningUpLoadingError
    }

    sealed interface SigningOutLoadingError : LoadingError {

        data object CommonError : SigningOutLoadingError
    }

    sealed interface AccountDeletingError : LoadingError {

        data object CommonError : AccountDeletingError

        data object NetworkError : AccountDeletingError

        data object RecentLoginRequired : AccountDeletingError
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
        TODO("implement")
//        auth.currentUser?.reauthenticate(
//            EmailAuthProvider.getCredential(
//                auth.currentUser?.email!!,
//                ""
//            )
//        )
    }
}