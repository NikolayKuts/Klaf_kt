package com.kuts.klaf.networking.klafServer

import android.content.Context
import com.kuts.domain.managers.IAccountSession
import com.kuts.klaf.room.databases.ActiveLocalRoomDatabase
import com.kuts.klaf.room.repositoryImplementations.GuestAccountDataTransfer
import io.ktor.client.HttpClient
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Keeps Android Keystore initialization off app startup and out of the UI thread. */
fun createAndroidSecureAccountSession(
    serverOrigin: String,
    httpClient: HttpClient,
    context: Context,
    localDatabase: ActiveLocalRoomDatabase,
    guestTransfer: GuestAccountDataTransfer,
    deviceProvider: suspend () -> AccountDevice,
    pendingEnrollment: PendingSignUpAttemptStore,
): IAccountSession {
    val mutex = Mutex()
    val keys = AndroidDpopDeviceKeyStore(storageRoot = context.filesDir)
    val sessions = AndroidProtectedSessionStore(context.filesDir)
    var cached: SecureAccountFlow? = null
    return SecureServerAccountSession(
        authProvider = {
            mutex.withLock {
                cached ?: SecureAccountFlow(
                    serverOrigin,
                    SecureAccountRestClient(serverOrigin, httpClient,
                        DpopProofFactory(keys.loadOrCreate(), AndroidDpopProofPrimitives())),
                    sessions,
                ).also { cached = it }
            }
        },
        localDatabase = localDatabase,
        guestTransfer = guestTransfer,
        deviceProvider = deviceProvider,
        pendingStore = pendingEnrollment,
        removeLocalCredentials = { email -> sessions.remove(serverOrigin, email) },
    )
}

fun createAndroidAuthenticatedRequestSigner(
    serverOrigin: String,
    context: Context,
    httpClient: HttpClient,
): KlafAuthenticatedRequestSigner {
    val mutex = Mutex()
    val keys = AndroidDpopDeviceKeyStore(storageRoot = context.filesDir)
    var cached: SecureRequestAuthorizer? = null
    return object : KlafAuthenticatedRequestSigner {
        override suspend fun headers(email: String, method: String, url: String, nonce: String?): AccessProofHeaders {
            val authorizer = mutex.withLock {
                cached ?: run {
                    val primitives = AndroidDpopProofPrimitives()
                    val proofs = DpopProofFactory(keys.loadOrCreate(), primitives)
                    SecureRequestAuthorizer(serverOrigin,
                        AndroidProtectedSessionStore(context.filesDir), proofs,
                        SecureAccountRestClient(serverOrigin, httpClient, proofs),
                        primitives::nowEpochSeconds, primitives::nextJti,
                    ).also { cached = it }
                }
            }
            return authorizer.headers(email, method, url, nonce)
        }
    }
}
