package com.kuts.klaf.networking.klafServer

import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class EcdsaDerSignatureTest {

    @Test
    fun `DER ECDSA converts to verifiable JOSE P1363 signature`() {
        val pair = KeyPairGenerator.getInstance("EC").apply {
            initialize(ECGenParameterSpec("secp256r1"))
        }.generateKeyPair()
        val message = "proof-signing-input".encodeToByteArray()
        repeat(64) {
            val der = Signature.getInstance("SHA256withECDSA").apply {
                initSign(pair.private)
                update(message)
            }.sign()
            val jose = EcdsaDerSignature.toJoseRaw(der)
            assertEquals(64, jose.size)
            assertTrue(Signature.getInstance("SHA256withECDSAinP1363Format").apply {
                initVerify(pair.public)
                update(message)
            }.verify(jose))
        }
    }

    @Test
    fun `malformed or noncanonical DER is rejected`() {
        val malformed = listOf(
            byteArrayOf(),
            byteArrayOf(0x30, 0x00),
            byteArrayOf(0x31, 0x06, 0x02, 0x01, 0x01, 0x02, 0x01, 0x01),
            byteArrayOf(0x30, 0x07, 0x02, 0x02, 0x00, 0x01, 0x02, 0x01, 0x01),
            byteArrayOf(0x30, 0x06, 0x02, 0x01, 0xff.toByte(), 0x02, 0x01, 0x01),
        )
        for (candidate in malformed) {
            assertFailsWith<IllegalArgumentException> { EcdsaDerSignature.toJoseRaw(candidate) }
        }
    }
}
