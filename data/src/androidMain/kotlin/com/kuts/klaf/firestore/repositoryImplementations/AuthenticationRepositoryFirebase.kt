package com.kuts.klaf.firestore.repositoryImplementations

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.kuts.domain.common.AuthenticationAction
import com.kuts.domain.common.LoadingState
import com.kuts.domain.common.catchWithCrashlyticsReport
import com.kuts.domain.entities.AuthenticationState
import com.kuts.domain.repositories.IAuthenticationRepository
import com.kuts.domain.repositories.IAuthenticationRepository.IAccountDeletingError
import com.kuts.domain.repositories.IAuthenticationRepository.IAuthenticationError
import com.kuts.domain.repositories.IAuthenticationRepository.ISigningInError
import com.kuts.domain.repositories.IAuthenticationRepository.ISigningOutError
import com.kuts.domain.repositories.IAuthenticationRepository.ISigningUpError
import com.kuts.domain.repositories.ICrashlyticsRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

class AuthenticationRepositoryFirebase(
    private val auth: FirebaseAuth,
    private val crashlytics: ICrashlyticsRepository,
) : IAuthenticationRepository {

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
    ): Flow<LoadingState<AuthenticationAction, IAuthenticationError>> = flow {
        emit(LoadingState.Loading)
        auth.signInWithEmailAndPassword(
            email,
            password
        ).await()
        emit(LoadingState.Success(data = AuthenticationAction.SIGN_IN))
    }.catchWithCrashlyticsReport<LoadingState<AuthenticationAction, IAuthenticationError>>(
        crashlytics = crashlytics
    ) { error ->
        val errorType = when (error) {
            is FirebaseAuthInvalidUserException -> ISigningInError.NoUserRecord
            is FirebaseAuthInvalidCredentialsException -> ISigningInError.InvalidPassword
            is FirebaseNetworkException -> ISigningInError.NetworkError
            else -> ISigningInError.CommonError
        }

        emit(value = LoadingState.Error(value = errorType))
    }

    override fun signUpWithEmailAndPassword(
        email: String,
        password: String,
    ): Flow<LoadingState<AuthenticationAction, IAuthenticationError>> = flow {
        emit(LoadingState.Loading)
        auth.createUserWithEmailAndPassword(
            email,
            password
        ).await()
        emit(LoadingState.Success(data = AuthenticationAction.SIGN_UP))
    }.catchWithCrashlyticsReport<LoadingState<AuthenticationAction, IAuthenticationError>>(
        crashlytics = crashlytics
    ) { error ->
        val errorType = when (error) {
            is FirebaseAuthUserCollisionException -> ISigningUpError.EmailAlreadyInUse
            is FirebaseNetworkException -> ISigningUpError.NetworkError
            else -> ISigningUpError.CommonError
        }

        emit(value = LoadingState.Error(value = errorType))
    }

    override fun signOut(): Flow<LoadingState<Unit, IAuthenticationError>> = flow {
        emit(LoadingState.Loading)
        auth.signOut()
        emit(LoadingState.Success(data = Unit))
    }.catchWithCrashlyticsReport<LoadingState<Unit, IAuthenticationError>>(
        crashlytics = crashlytics
    ) {
        emit(value = LoadingState.Error(value = ISigningOutError.CommonError))
    }

    override fun deleteProfile(): Flow<LoadingState<Unit, IAuthenticationError>> = flow {
        val user = auth.currentUser
            ?: throw RuntimeException("Trying user deleting when current user is null")

        emit(value = LoadingState.Loading)
        user.delete().await()
        emit(value = LoadingState.Success(data = Unit))
    }.catchWithCrashlyticsReport<LoadingState<Unit, IAuthenticationError>>(
        crashlytics = crashlytics
    ) { throwable ->
        val error = when (throwable) {
            is FirebaseNetworkException -> IAccountDeletingError.NetworkError
            is FirebaseAuthRecentLoginRequiredException -> IAccountDeletingError.RecentLoginRequired
            else -> IAccountDeletingError.CommonError
        }

        emit(value = LoadingState.Error(value = error))
    }

    override fun reauthenticateWithEmailAndPassword(
        email: String,
        password: String,
    ): Flow<LoadingState<AuthenticationAction, IAuthenticationError>> = flow {
        TODO("implement")
//        auth.currentUser?.reauthenticate(
//            EmailAuthProvider.getCredential(
//                auth.currentUser?.email!!,
//                ""
//            )
//        )
    }
}
