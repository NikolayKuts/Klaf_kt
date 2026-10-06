package com.kuts.klaf.networking.klafServer

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import java.io.File
import java.security.KeyStore
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidProtectedSessionStoreTest {

    @Test
    fun missingDpopKeyDoesNotCreateNewIdentityForExistingAccountSession() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sessionAlias = "klaf-session-test-${UUID.randomUUID()}"
        val dpopAlias = "klaf-dpop-test-${UUID.randomUUID()}"
        val root = File(context.cacheDir, "protected-session-test-${UUID.randomUUID()}")
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        try {
            AndroidProtectedSessionStore(root, sessionAlias).write(ProtectedAuthSession(
                "https://one.test", "alice@example.test", "android-a", "access", "refresh",
            ))
            assertFailsWith<DeviceKeyUnavailableException> {
                AndroidDpopDeviceKeyStore(dpopAlias, storageRoot = root).loadOrCreate()
            }
            Unit
        } finally {
            keyStore.deleteEntry(sessionAlias)
            keyStore.deleteEntry(dpopAlias)
            root.deleteRecursively()
        }
    }

    @Test
    fun tokensUseNonexportableAesKeyAndAreScopedByProfile() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val alias = "klaf-session-test-${UUID.randomUUID()}"
        val root = File(context.cacheDir, "protected-session-test-${UUID.randomUUID()}")
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        try {
            val store = AndroidProtectedSessionStore(root, alias)
            val session = ProtectedAuthSession("https://one.test", "alice@example.test", "android-a",
                "access-secret", "refresh-secret")
            store.write(session)
            assertNull(keyStore.getKey(alias, null).encoded)
            assertEquals(session, AndroidProtectedSessionStore(root, alias).read(
                session.serverOrigin, session.accountEmail))
            assertNull(store.read("https://other.test", session.accountEmail))

            val protectedFile = File(root, "security/sessions").listFiles()!!
                .single { it.name.endsWith(".bin") }
            protectedFile.writeBytes(byteArrayOf(1, 2, 3))
            assertFailsWith<ProtectedSessionUnavailableException> {
                store.read(session.serverOrigin, session.accountEmail)
            }
            Unit
        } finally {
            keyStore.deleteEntry(alias)
            root.deleteRecursively()
        }
    }
}
