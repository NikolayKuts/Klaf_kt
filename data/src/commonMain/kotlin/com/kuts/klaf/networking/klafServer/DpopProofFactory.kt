package com.kuts.klaf.networking.klafServer

import kotlin.io.encoding.Base64
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** Signs the JWS input with a device-bound P-256 key; the returned signature is JOSE raw R || S. */
internal interface DpopDeviceKey {
    val publicJwk: JsonObject
    suspend fun sign(input: ByteArray): ByteArray
}

/** Platform clock, CSPRNG identifier and SHA-256; implementations must never use a predictable JTI. */
internal interface DpopProofPrimitives {
    fun nowEpochSeconds(): Long
    fun nextJti(): String
    fun sha256(input: ByteArray): ByteArray
}

internal class DpopProofFactory(
    private val key: DpopDeviceKey,
    private val primitives: DpopProofPrimitives,
) {

    private val json = Json

    suspend fun create(method: String, url: String, nonce: String?, accessToken: String?): String {
        val publicKey = key.publicJwk
        require(publicKey.keys == setOf("kty", "crv", "x", "y") &&
            publicKey["kty"]?.jsonPrimitive?.content == "EC" &&
            publicKey["crv"]?.jsonPrimitive?.content == "P-256") { "Device key must expose only a P-256 public JWK" }
        val canonicalMethod = method.uppercase()
        require(canonicalMethod.isNotEmpty() && canonicalMethod.all { it in 'A'..'Z' })
        val canonicalUrl = url.substringBefore('#').substringBefore('?')
        require(canonicalUrl.startsWith("https://") || canonicalUrl.startsWith("http://"))
        val jti = primitives.nextJti()
        require(jti.length in 16..128)
        val header = buildJsonObject {
            put("typ", "dpop+jwt")
            put("alg", "ES256")
            put("jwk", publicKey)
        }
        val payload = buildJsonObject {
            put("htm", canonicalMethod)
            put("htu", canonicalUrl)
            put("iat", primitives.nowEpochSeconds())
            put("jti", jti)
            if (nonce != null) put("nonce", nonce)
            if (accessToken != null) {
                put("ath", base64Url(primitives.sha256(accessToken.encodeToByteArray())))
            }
        }
        val input = "${base64Url(json.encodeToString(header).encodeToByteArray())}." +
            base64Url(json.encodeToString(payload).encodeToByteArray())
        val signature = key.sign(input.encodeToByteArray())
        require(signature.size == 64) { "ES256 signature must be 64 JOSE bytes" }
        return "$input.${base64Url(signature)}"
    }

    private fun base64Url(bytes: ByteArray): String = Base64.UrlSafe.encode(bytes).trimEnd('=')
}
