package com.kuts.klaf.networking.klafServer

import com.kuts.domain.managers.IAccountSession
import com.kuts.klaf.room.databases.ActiveLocalRoomDatabase
import com.kuts.klaf.room.repositoryImplementations.GuestAccountDataTransfer
import io.ktor.client.HttpClient
import java.io.File
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Device key and protected credentials are loaded only when an account request starts. */
fun createDesktopSecureAccountSession(
    serverOrigin: String,
    httpClient: HttpClient,
    storageDirectory: File,
    localDatabase: ActiveLocalRoomDatabase,
    guestTransfer: GuestAccountDataTransfer,
    deviceProvider: suspend () -> AccountDevice,
    pendingEnrollment: PendingSignUpAttemptStore,
): IAccountSession {
    val mutex = Mutex()
    val keys = DesktopDpopDeviceKeyStore(storageDirectory.toPath())
    val sessions = DesktopProtectedSessionStore(storageDirectory.toPath())
    var cached: SecureAccountFlow? = null
    return SecureServerAccountSession(
        authProvider = {
            mutex.withLock {
                cached ?: SecureAccountFlow(
                    serverOrigin,
                    SecureAccountRestClient(serverOrigin, httpClient,
                        DpopProofFactory(keys.loadOrCreate(), DesktopDpopProofPrimitives())),
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

fun createDesktopAuthenticatedRequestSigner(
    serverOrigin: String,
    storageDirectory: File,
    httpClient: HttpClient,
): KlafAuthenticatedRequestSigner {
    val mutex = Mutex()
    val keys = DesktopDpopDeviceKeyStore(storageDirectory.toPath())
    var cached: SecureRequestAuthorizer? = null
    return object : KlafAuthenticatedRequestSigner {
        override suspend fun headers(email: String, method: String, url: String, nonce: String?): AccessProofHeaders {
            val authorizer = mutex.withLock {
                cached ?: run {
                    val primitives = DesktopDpopProofPrimitives()
                    val proofs = DpopProofFactory(keys.loadOrCreate(), primitives)
                    SecureRequestAuthorizer(serverOrigin,
                        DesktopProtectedSessionStore(storageDirectory.toPath()), proofs,
                        SecureAccountRestClient(serverOrigin, httpClient, proofs),
                        primitives::nowEpochSeconds, primitives::nextJti,
                    ).also { cached = it }
                }
            }
            return authorizer.headers(email, method, url, nonce)
        }
    }
}
