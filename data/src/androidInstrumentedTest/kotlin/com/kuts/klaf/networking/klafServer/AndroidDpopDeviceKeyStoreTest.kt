package com.kuts.klaf.networking.klafServer

import java.security.KeyStore
import java.security.Signature
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class AndroidDpopDeviceKeyStoreTest {

    @Test
    fun keyIsNonexportablePersistentAndSignsDeviceProof() = runBlocking {
        val alias = "klaf-dpop-instrumented-${UUID.randomUUID()}"
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        try {
            val first = AndroidDpopDeviceKeyStore(alias).loadOrCreate()
            val second = AndroidDpopDeviceKeyStore(alias).loadOrCreate()
            assertEquals(first.publicJwk, second.publicJwk)
            assertNull(keyStore.getKey(alias, null).encoded)
            val input = "device-proof".encodeToByteArray()
            val rawSignature = second.sign(input)
            assertEquals(64, rawSignature.size)
            val publicKey = keyStore.getCertificate(alias).publicKey
            assertTrue(Signature.getInstance("SHA256withECDSA").apply {
                initVerify(publicKey)
                update(input)
            }.verify(rawToDer(rawSignature)))
        } finally {
            keyStore.deleteEntry(alias)
        }
    }

    private fun rawToDer(raw: ByteArray): ByteArray {
        fun integer(bytes: ByteArray): ByteArray {
            val stripped = bytes.dropWhile { it == 0.toByte() }.ifEmpty { listOf(0.toByte()) }.toByteArray()
            val positive = if (stripped[0].toInt() and 0x80 != 0) byteArrayOf(0) + stripped else stripped
            return byteArrayOf(0x02, positive.size.toByte()) + positive
        }
        val content = integer(raw.copyOfRange(0, 32)) + integer(raw.copyOfRange(32, 64))
        return byteArrayOf(0x30, content.size.toByte()) + content
    }
}
