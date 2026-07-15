package com.kuts.klaf.networking.codexApp

import com.kuts.domain.entities.MnemonicSelection
import com.kuts.domain.repositories.IMnemonicImageRepository
import com.kuts.klaf.common.MnemonicImageRequestPayload
import com.kuts.klaf.common.toPayload
import io.ktor.client.call.body
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json

class CodexAppMnemonicImageRepository(
    private val client: HttpClient,
    private val codexServerUrl: String,
) : IMnemonicImageRepository {

    @Suppress("OPT_IN_USAGE")
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    override suspend fun fetchMnemonicImage(
        selection: MnemonicSelection,
        comment: String?,
    ): ByteArray {
        require(value = selection.word.trim().isNotBlank()) {
            "Mnemonic image request word must not be blank."
        }
        require(value = selection.candidate.scene.trim().isNotBlank()) {
            "Mnemonic image request scene must not be blank."
        }

        val response = client.post(urlString = resolveMnemonicImageUrl(codexServerUrl)) {
            contentType(ContentType.Application.Json)
            setBody(
                body = json.encodeToString(
                    serializer = MnemonicImageRequestPayload.serializer(),
                    value = MnemonicImageRequestPayload(
                        selection = selection.toPayload(),
                        comment = comment?.trim()?.ifBlank { null },
                    ),
                ),
            )
        }

        if (!response.status.isSuccess()) {
            val responseBody = response.bodyAsText().trim()
            throw IllegalArgumentException(
                responseBody.ifBlank { "Mnemonic image request failed." }.toSingleLineMessage()
            )
        }

        return response.body<ByteArray>().also { imageBytes ->
            require(imageBytes.isNotEmpty()) { "Mnemonic image response is empty." }
        }
    }
}

private fun resolveMnemonicImageUrl(serverUrl: String): String {
    val normalizedServerUrl = serverUrl.trim()
    require(value = normalizedServerUrl.isNotBlank()) {
        "Codex App server URL is not configured."
    }

    val httpBaseUrl = when {
        normalizedServerUrl.startsWith(prefix = "ws://") -> {
            "http://${normalizedServerUrl.removePrefix(prefix = "ws://").trimEnd('/')}"
        }

        normalizedServerUrl.startsWith(prefix = "wss://") -> {
            "https://${normalizedServerUrl.removePrefix(prefix = "wss://").trimEnd('/')}"
        }

        else -> normalizedServerUrl.trimEnd('/')
    }

    return "$httpBaseUrl/mnemonic/image"
}

private fun String.toSingleLineMessage(): String {
    return lineSequence()
        .firstOrNull()
        ?.trim()
        .orEmpty()
        .ifBlank { "Mnemonic image request failed." }
}
