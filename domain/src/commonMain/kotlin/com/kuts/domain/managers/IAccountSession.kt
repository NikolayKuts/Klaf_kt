package com.kuts.domain.managers

import kotlinx.coroutines.flow.Flow

sealed interface AccountSignInResult {

    data object SignedIn : AccountSignInResult

    data class DeviceRegistrationRequired(val email: String) : AccountSignInResult
}

interface IAccountSession {

    val selectedAccountEmail: Flow<String?>

    suspend fun signUp(email: String)

    suspend fun signIn(email: String): AccountSignInResult

    suspend fun registerDevice(email: String)

    suspend fun signOut()
}
