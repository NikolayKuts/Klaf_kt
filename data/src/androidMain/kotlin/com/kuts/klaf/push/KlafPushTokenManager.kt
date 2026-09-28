package com.kuts.klaf.push

import com.google.firebase.messaging.FirebaseMessaging
import com.kuts.domain.common.ICoroutineContextProvider
import com.lib.lokdroid.core.logD
import com.lib.lokdroid.core.logE
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class KlafPushTokenManager(
    private val firebaseMessaging: FirebaseMessaging,
    private val coroutineContextProvider: ICoroutineContextProvider,
) {

    private val mutableToken = MutableStateFlow<String?>(value = null)

    val token: StateFlow<String?> = mutableToken.asStateFlow()

    suspend fun refreshToken(): String? = withContext(context = coroutineContextProvider.io) {
        try {
            firebaseMessaging.token.await()
                .also { token -> saveToken(token = token) }
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) throw throwable
            logE("Klaf push token refresh failed: ${throwable::class.simpleName}")
            null
        }
    }

    fun saveToken(token: String) {
        val normalizedToken = token.trim()
        if (normalizedToken.isBlank()) return

        mutableToken.value = normalizedToken
        logD(
            "Klaf push token updated: length=${normalizedToken.length}, " +
                "hash=${normalizedToken.hashCode()}",
        )
    }
}
