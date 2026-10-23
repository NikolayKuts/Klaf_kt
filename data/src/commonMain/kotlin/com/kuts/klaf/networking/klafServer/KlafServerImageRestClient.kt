package com.kuts.klaf.networking.klafServer

import com.kuts.domain.managers.AccountOperationException
import com.kuts.domain.managers.AccountFailure
import com.kuts.domain.managers.ImageSynchronizationException
import com.kuts.domain.managers.ImageSyncFailure
import com.kuts.klaf.mnemonic.MAX_MNEMONIC_IMAGE_BYTES
import com.kuts.klaf.mnemonic.isSafeMnemonicImageId
import com.kuts.klaf.mnemonic.mnemonicImageContentType
import com.kuts.klaf.server.contract.IMAGE_SIZE_HEADER
import io.ktor.client.HttpClient
import io.ktor.client.request.prepareGet
import io.ktor.client.request.preparePut
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.HttpStatement
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.utils.io.readAvailable
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// Saved images can be several MiB; their transfer budget is separate from small metadata requests.
private const val IMAGE_REQUEST_TIMEOUT_MILLIS = 60_000L

@Serializable
private data class ProtectedImageError(val code: String)

private sealed interface ProtectedImageResponse<out T> {
    data class Body<T>(val value: T) : ProtectedImageResponse<T>
    data class RetryNonce(val nonce: String) : ProtectedImageResponse<Nothing>
}

class KlafServerImageRestClient(
    baseUrl: String,
    private val http: HttpClient,
    private val signer: KlafAuthenticatedRequestSigner? = null,
) {

    private val endpoint = "${baseUrl.trimEnd('/')}/api/v1/images"

    suspend fun upload(
        email: String,
        deviceId: String,
        assetId: String,
        bytes: ByteArray,
    ) = klafServerRequest(IMAGE_REQUEST_TIMEOUT_MILLIS, operation = "PUT /api/v1/images") {
        require(isSafeMnemonicImageId(assetId)) { "Invalid image asset ID" }
        require(bytes.size in 1..MAX_MNEMONIC_IMAGE_BYTES && mnemonicImageContentType(bytes) != null) { "Invalid image data" }
        requestWithProof(email, "PUT", send = { proof ->
            http.preparePut(endpoint) {
                parameter("email", email)
                parameter("deviceId", deviceId)
                parameter("assetId", assetId)
                contentType(ContentType.Application.OctetStream)
                headers.append(IMAGE_SIZE_HEADER, bytes.size.toString())
                proof?.let {
                    headers.append("Authorization", it.authorization)
                    headers.append("DPoP", it.dpop)
                }
                setBody(bytes)
            }
        }) { response ->
            if (response.status.value != 200 && response.status.value != 201) {
                val code = response.bodyAsText()
                when (code) {
                    "IMAGE_ID_REUSED" -> throw ImageSynchronizationException(ImageSyncFailure.ID_REUSED)
                    "INVALID_IMAGE", "IMAGE_TOO_LARGE" -> throw ImageSynchronizationException(ImageSyncFailure.INVALID_FILE)
                    else -> throw AccountOperationException(accountHttpFailure(response.status.value, code))
                }
            }
        }
    }

    suspend fun download(
        email: String,
        deviceId: String,
        assetId: String,
    ): ByteArray? = klafServerRequest(IMAGE_REQUEST_TIMEOUT_MILLIS, operation = "GET /api/v1/images") {
        require(isSafeMnemonicImageId(assetId)) { "Invalid image asset ID" }
        requestWithProof(email, "GET", send = { proof ->
            http.prepareGet(endpoint) {
                parameter("email", email)
                parameter("deviceId", deviceId)
                parameter("assetId", assetId)
                proof?.let {
                    headers.append("Authorization", it.authorization)
                    headers.append("DPoP", it.dpop)
                }
            }
        }) { response ->
            if (response.status.value == 404) return@requestWithProof null
            check(response.status.value == 200) { "Klaf image HTTP ${response.status.value}" }
            val declaredBytes = response.expectedImageSize()
            val channel = response.bodyAsChannel()
            // Read one extra byte to detect excess data, without waiting past the declared limit.
            val buffer = ByteArray(declaredBytes.toInt() + 1)
            var received = 0
            while (received < buffer.size) {
                val count = channel.readAvailable(buffer, received, buffer.size - received)
                if (count < 0) break
                received += count
            }
            channel.closedCause?.let { throw it }
            if (received.toLong() != declaredBytes) {
                throw AccountOperationException(AccountFailure.INVALID_RESPONSE, "GET /api/v1/images: INCOMPLETE_RESPONSE")
            }
            val bytes = buffer.copyOf(received)
            check(bytes.size in 1..MAX_MNEMONIC_IMAGE_BYTES && mnemonicImageContentType(bytes) != null) { "Invalid image response" }
            bytes
        }
    }

    private suspend fun <T> requestWithProof(
        email: String,
        method: String,
        send: suspend (AccessProofHeaders?) -> HttpStatement,
        receive: suspend (HttpResponse) -> T,
    ): T {
        var nonce: String? = null
        for (attempt in 0..1) {
            val proof = signer?.headers(email, method, endpoint, nonce)
            val result: ProtectedImageResponse<T> = send(proof).execute { response ->
                if (signer != null && response.status.value == 401) {
                    val body = response.bodyAsText()
                    val code = runCatching { Json.decodeFromString<ProtectedImageError>(body).code }
                        .getOrDefault("HTTP_ERROR")
                    val challenge = response.headers["DPoP-Nonce"]
                    if (attempt == 0 && code == "DPOP_NONCE_REQUIRED" && challenge != null &&
                        challenge.length in 16..128) {
                        ProtectedImageResponse.RetryNonce(challenge)
                    } else {
                        throw AccountOperationException(accountHttpFailure(response.status.value, code))
                    }
                } else {
                    ProtectedImageResponse.Body(receive(response))
                }
            }
            when (result) {
                is ProtectedImageResponse.Body -> return result.value
                is ProtectedImageResponse.RetryNonce -> nonce = result.nonce
            }
        }
        throw AccountOperationException(AccountFailure.INVALID_RESPONSE)
    }

    private fun HttpResponse.expectedImageSize(): Long {
        val imageSizeHeader = headers[IMAGE_SIZE_HEADER]
        val contentSizeHeader = headers[HttpHeaders.ContentLength]
        val imageSize = imageSizeHeader?.toLongOrNull()
        val contentSize = contentSizeHeader?.toLongOrNull()
        val declaredBytes = imageSize ?: contentSize
        if (imageSizeHeader != null && imageSize == null || contentSizeHeader != null && contentSize == null ||
            imageSize != null && contentSize != null && imageSize != contentSize ||
            declaredBytes == null || declaredBytes !in 1..MAX_MNEMONIC_IMAGE_BYTES.toLong()) {
            throw AccountOperationException(AccountFailure.INVALID_RESPONSE, "GET /api/v1/images: INVALID_RESPONSE")
        }
        return declaredBytes
    }
}
