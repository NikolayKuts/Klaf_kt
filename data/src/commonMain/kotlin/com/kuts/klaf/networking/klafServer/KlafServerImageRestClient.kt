package com.kuts.klaf.networking.klafServer

import com.kuts.domain.managers.AccountOperationException
import com.kuts.domain.managers.ImageSynchronizationException
import com.kuts.domain.managers.ImageSyncFailure
import com.kuts.klaf.mnemonic.MAX_MNEMONIC_IMAGE_BYTES
import com.kuts.klaf.mnemonic.isSafeMnemonicImageId
import com.kuts.klaf.mnemonic.mnemonicImageContentType
import io.ktor.client.HttpClient
import io.ktor.client.request.prepareGet
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray

private const val IMAGE_REQUEST_TIMEOUT_MILLIS = 15_000L

class KlafServerImageRestClient(baseUrl: String, private val http: HttpClient) {

    private val endpoint = "${baseUrl.trimEnd('/')}/api/v1/images"

    suspend fun upload(email: String, deviceId: String, assetId: String, bytes: ByteArray) = klafServerRequest(IMAGE_REQUEST_TIMEOUT_MILLIS) {
        require(isSafeMnemonicImageId(assetId)) { "Invalid image asset ID" }
        require(bytes.size in 1..MAX_MNEMONIC_IMAGE_BYTES && mnemonicImageContentType(bytes) != null) { "Invalid image data" }
        val response = http.put(endpoint) {
            parameter("email", email)
            parameter("deviceId", deviceId)
            parameter("assetId", assetId)
            contentType(ContentType.Application.OctetStream)
            setBody(bytes)
        }
        if (response.status.value != 200 && response.status.value != 201) {
            val code = response.bodyAsText()
            when (code) {
                "IMAGE_ID_REUSED" -> throw ImageSynchronizationException(ImageSyncFailure.ID_REUSED)
                "INVALID_IMAGE", "IMAGE_TOO_LARGE" -> throw ImageSynchronizationException(ImageSyncFailure.INVALID_FILE)
                else -> throw AccountOperationException(accountHttpFailure(response.status.value, code))
            }
        }
    }

    suspend fun download(email: String, deviceId: String, assetId: String): ByteArray? = klafServerRequest(IMAGE_REQUEST_TIMEOUT_MILLIS) {
        require(isSafeMnemonicImageId(assetId)) { "Invalid image asset ID" }
        http.prepareGet(endpoint) {
            parameter("email", email)
            parameter("deviceId", deviceId)
            parameter("assetId", assetId)
        }.execute { response ->
            if (response.status.value == 404) return@execute null
            check(response.status.value == 200) { "Klaf image HTTP ${response.status.value}" }
            val bytes = response.bodyAsChannel().readRemaining((MAX_MNEMONIC_IMAGE_BYTES + 1).toLong()).readByteArray()
            check(bytes.size in 1..MAX_MNEMONIC_IMAGE_BYTES && mnemonicImageContentType(bytes) != null) { "Invalid image response" }
            bytes
        }
    }
}
