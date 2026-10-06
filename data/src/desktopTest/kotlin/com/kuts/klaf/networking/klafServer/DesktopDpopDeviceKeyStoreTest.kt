package com.kuts.klaf.networking.klafServer

import java.nio.file.Files
import java.security.AlgorithmParameters
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECParameterSpec
import java.security.spec.ECPoint
import java.security.spec.ECPublicKeySpec
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.TimeUnit
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.jsonPrimitive

class DesktopDpopDeviceKeyStoreTest {

    @Test
    fun `device key survives restart and signs verifiable ES256 raw signatures`() = runBlocking {
        val directory = Files.createTempDirectory("klaf-device-key-")
        val protector = TestProtector()
        val first = DesktopDpopDeviceKeyStore(directory, protector).loadOrCreate()
        val second = DesktopDpopDeviceKeyStore(directory, protector).loadOrCreate()
        assertEquals(first.publicJwk, second.publicJwk)
        val input = "device-bound-request".encodeToByteArray()
        val signature = second.sign(input)
        assertEquals(64, signature.size)

        val parameters = AlgorithmParameters.getInstance("EC").apply {
            init(ECGenParameterSpec("secp256r1"))
        }.getParameterSpec(ECParameterSpec::class.java)
        val x = decodeCoordinate(first.publicJwk["x"]!!.jsonPrimitive.content)
        val y = decodeCoordinate(first.publicJwk["y"]!!.jsonPrimitive.content)
        val publicKey = KeyFactory.getInstance("EC").generatePublic(ECPublicKeySpec(ECPoint(x, y), parameters))
        assertTrue(Signature.getInstance("SHA256withECDSAinP1363Format").apply {
            initVerify(publicKey)
            update(input)
        }.verify(signature))
        val protectedBytes = Files.readAllBytes(directory.resolve("security/device-key.bin"))
        assertFalse(protectedBytes.decodeToString().contains("PRIVATE KEY"))
    }

    @Test
    fun `existing protected key cannot be silently replaced after unlock failure`() = runBlocking {
        val directory = Files.createTempDirectory("klaf-device-key-lost-")
        DesktopDpopDeviceKeyStore(directory, TestProtector()).loadOrCreate()
        val keyFile = directory.resolve("security/device-key.bin")
        val original = Files.readAllBytes(keyFile)

        assertFailsWith<DeviceKeyUnavailableException> {
            DesktopDpopDeviceKeyStore(directory, TestProtector(0x55)).loadOrCreate()
        }
        assertTrue(original.contentEquals(Files.readAllBytes(keyFile)))
    }

    @Test
    fun `missing device key cannot be regenerated while account credentials remain`() = runBlocking {
        val directory = Files.createTempDirectory("klaf-device-key-missing-")
        DesktopProtectedSessionStore(directory, TestProtector()).write(ProtectedAuthSession(
            "https://one.test", "alice@example.test", "desktop-a", "access", "refresh",
        ))

        assertFailsWith<DeviceKeyUnavailableException> {
            DesktopDpopDeviceKeyStore(directory, TestProtector()).loadOrCreate()
        }
        Unit
    }

    @Test
    fun `concurrent processes converge on one durable device key`() = runBlocking {
        val directory = Files.createTempDirectory("klaf-device-key-race-")
        val protector = TestProtector(barrier = CyclicBarrier(2))
        val keys = coroutineScope {
            (1..2).map {
                async(Dispatchers.IO) { DesktopDpopDeviceKeyStore(directory, protector).loadOrCreate().publicJwk }
            }.awaitAll()
        }
        assertEquals(1, keys.toSet().size)
        assertEquals(keys.first(), DesktopDpopDeviceKeyStore(directory, protector).loadOrCreate().publicJwk)
    }

    @Test
    fun `Windows DPAPI protects and reopens a temporary device key`() = runBlocking {
        if (!System.getProperty("os.name").startsWith("Windows")) return@runBlocking
        val directory = Files.createTempDirectory("klaf-dpapi-key-")
        val first = DesktopDpopDeviceKeyStore(directory).loadOrCreate()
        val second = DesktopDpopDeviceKeyStore(directory).loadOrCreate()
        assertEquals(first.publicJwk, second.publicJwk)
        assertEquals(64, second.sign("dpapi-reopened".encodeToByteArray()).size)
        assertTrue(Files.size(directory.resolve("security/device-key.bin")) > 64)
    }

    private fun decodeCoordinate(value: String): java.math.BigInteger = java.math.BigInteger(1,
        Base64.UrlSafe.decode(value + "=".repeat((4 - value.length % 4) % 4)))

    private class TestProtector(
        private val mask: Int = 0x29,
        private val barrier: CyclicBarrier? = null,
    ) : DesktopDeviceKeyProtector {
        override fun protect(data: ByteArray): ByteArray {
            barrier?.let { runCatching { it.await(200, TimeUnit.MILLISECONDS) } }
            return xor(data)
        }
        override fun unprotect(data: ByteArray) = xor(data)
        private fun xor(data: ByteArray) = data.map { (it.toInt() xor mask).toByte() }.toByteArray()
    }
}
