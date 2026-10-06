package com.kuts.klaf.networking.klafServer

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

class DesktopProtectedSessionStoreTest {

    @Test
    fun `tokens persist encrypted and remain scoped by origin and account`() = runBlocking {
        val root = Files.createTempDirectory("klaf-session-store-")
        val store = DesktopProtectedSessionStore(root, TestProtector())
        val alice = ProtectedAuthSession("https://one.test", "alice@example.test", "desktop-a",
            "access-alice-secret", "refresh-alice-secret")
        val bob = alice.copy(accountEmail = "bob@example.test", accessToken = "access-bob-secret")
        store.write(alice)
        store.write(bob)

        val reopened = DesktopProtectedSessionStore(root, TestProtector())
        assertEquals(alice, reopened.read(alice.serverOrigin, alice.accountEmail))
        assertEquals(bob, reopened.read(bob.serverOrigin, bob.accountEmail))
        assertNull(reopened.read("https://other.test", alice.accountEmail))
        val protectedFiles = Files.list(root.resolve("security/sessions")).use { it.toList() }
            .filter { it.fileName.toString().endsWith(".bin") }
        assertEquals(2, protectedFiles.size)
        assertTrue(protectedFiles.none { path ->
            val contents = Files.readAllBytes(path).decodeToString()
            contents.contains("alice@example.test") || contents.contains("refresh-alice-secret")
        })
        reopened.remove(alice.serverOrigin, alice.accountEmail)
        assertNull(reopened.read(alice.serverOrigin, alice.accountEmail))
        assertEquals(bob, reopened.read(bob.serverOrigin, bob.accountEmail))
    }

    @Test
    fun `a corrupt protected session fails closed instead of looking signed out`() = runBlocking {
        val root = Files.createTempDirectory("klaf-session-corrupt-")
        val store = DesktopProtectedSessionStore(root, TestProtector())
        val session = ProtectedAuthSession("https://one.test", "alice@example.test", "desktop-a",
            "access", "refresh")
        store.write(session)
        Files.list(root.resolve("security/sessions")).use { files ->
            Files.write(files.filter { it.fileName.toString().endsWith(".bin") }.findFirst().orElseThrow(), byteArrayOf(1, 2, 3))
        }
        assertFailsWith<ProtectedSessionUnavailableException> {
            store.read(session.serverOrigin, session.accountEmail)
        }
        Unit
    }

    @Test
    fun `Windows DPAPI reopens temporary protected account credentials`() = runBlocking {
        if (!System.getProperty("os.name").startsWith("Windows")) return@runBlocking
        val root = Files.createTempDirectory("klaf-session-dpapi-")
        val session = ProtectedAuthSession("https://one.test", "alice@example.test", "desktop-a",
            "secret-access", "secret-refresh")
        DesktopProtectedSessionStore(root).write(session)
        assertEquals(session, DesktopProtectedSessionStore(root).read(session.serverOrigin, session.accountEmail))
    }

    @Test
    fun `refresh journal survives reopen and same-profile locks serialize separate store instances`() = runBlocking {
        val root = Files.createTempDirectory("klaf-session-refresh-")
        val first = DesktopProtectedSessionStore(root, TestProtector())
        val second = DesktopProtectedSessionStore(root, TestProtector())
        val session = ProtectedAuthSession("https://one.test", "alice@example.test", "desktop-a",
            "old-access", "old-refresh", "stable-refresh-operation-id")
        first.write(session)
        assertEquals(session, second.read(session.serverOrigin, session.accountEmail))

        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var secondEntered = false
        val firstJob = async(Dispatchers.IO) {
            first.withRefreshLock(session.serverOrigin, session.accountEmail) {
                entered.complete(Unit)
                release.await()
            }
        }
        entered.await()
        val secondJob = async(Dispatchers.IO) {
            second.withRefreshLock(session.serverOrigin, session.accountEmail) {
                secondEntered = true
            }
        }
        delay(100)
        assertEquals(false, secondEntered)
        release.complete(Unit)
        firstJob.await()
        secondJob.await()
        assertTrue(secondEntered)
    }

    private class TestProtector : DesktopDeviceKeyProtector {
        override fun protect(data: ByteArray) = data.map { (it.toInt() xor 0x41).toByte() }.toByteArray()
        override fun unprotect(data: ByteArray) = protect(data)
    }
}
