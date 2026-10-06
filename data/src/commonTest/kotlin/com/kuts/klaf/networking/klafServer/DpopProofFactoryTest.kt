package com.kuts.klaf.networking.klafServer

import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class DpopProofFactoryTest {

    private val publicKey = Json.parseToJsonElement(
        """{"kty":"EC","crv":"P-256","x":"x-coordinate","y":"y-coordinate"}""",
    ).jsonObject

    @Test
    fun `proof has public JWK canonical method URI nonce and access hash`() = runBlocking {
        val signingInput = mutableListOf<ByteArray>()
        val factory = DpopProofFactory(object : DpopDeviceKey {
            override val publicJwk = publicKey
            override suspend fun sign(input: ByteArray): ByteArray {
                signingInput += input
                return ByteArray(64) { it.toByte() }
            }
        }, object : DpopProofPrimitives {
            override fun nowEpochSeconds() = 1_800_000_000L
            override fun nextJti() = "unpredictable-unique-jti-123"
            override fun sha256(input: ByteArray) = ByteArray(32) { 42 }
        })

        val proof = factory.create("post", "https://example.test/api/v1/sync/bootstrap?email=alice#ignored",
            nonce = "server-nonce", accessToken = "short-lived-token")
        val parts = proof.split('.')
        assertEquals(3, parts.size)
        val header = Json.parseToJsonElement(decodePart(parts[0]).decodeToString()).jsonObject
        val claims = Json.parseToJsonElement(decodePart(parts[1]).decodeToString()).jsonObject
        assertEquals("dpop+jwt", header["typ"]?.jsonPrimitive?.content)
        assertEquals("ES256", header["alg"]?.jsonPrimitive?.content)
        assertEquals(publicKey, header["jwk"]?.jsonObject)
        assertFalse(header.toString().contains("private"))
        assertEquals("POST", claims["htm"]?.jsonPrimitive?.content)
        assertEquals("https://example.test/api/v1/sync/bootstrap", claims["htu"]?.jsonPrimitive?.content)
        assertEquals("1800000000", claims["iat"]?.jsonPrimitive?.content)
        assertEquals("unpredictable-unique-jti-123", claims["jti"]?.jsonPrimitive?.content)
        assertEquals("server-nonce", claims["nonce"]?.jsonPrimitive?.content)
        assertEquals(Base64.UrlSafe.encode(ByteArray(32) { 42 }).trimEnd('='), claims["ath"]?.jsonPrimitive?.content)
        assertContentEquals(ByteArray(64) { it.toByte() }, decodePart(parts[2]))
        assertContentEquals("${parts[0]}.${parts[1]}".encodeToByteArray(), signingInput.single())
    }

    @Test
    fun `each retry creates a fresh proof ID and unauthenticated proof has no ath`() = runBlocking {
        var sequence = 0
        val factory = DpopProofFactory(object : DpopDeviceKey {
            override val publicJwk = publicKey
            override suspend fun sign(input: ByteArray) = ByteArray(64)
        }, object : DpopProofPrimitives {
            override fun nowEpochSeconds() = 1_800_000_000L
            override fun nextJti() = "fresh-proof-${++sequence}-123456"
            override fun sha256(input: ByteArray) = ByteArray(32)
        })

        val first = Json.parseToJsonElement(decodePart(factory.create("GET",
            "https://example.test/path", null, null).split('.')[1]).decodeToString()).jsonObject
        val second = Json.parseToJsonElement(decodePart(factory.create("GET",
            "https://example.test/path", null, null).split('.')[1]).decodeToString()).jsonObject
        assertNotEquals(first["jti"], second["jti"])
        assertFalse("ath" in first)
        assertFalse("nonce" in first)
        assertTrue(first["jti"]!!.jsonPrimitive.content.length >= 16)
    }

    private fun decodePart(value: String): ByteArray =
        Base64.UrlSafe.decode(value + "=".repeat((4 - value.length % 4) % 4))
}
