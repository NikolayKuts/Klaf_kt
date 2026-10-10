package com.kuts.klaf.networking.klafServer

import com.kuts.domain.entities.AutocompleteWord
import com.kuts.domain.managers.AccountFailure
import com.kuts.domain.managers.AccountOperationException
import com.kuts.domain.repositories.IWordAutocompleteRepository
import com.kuts.klaf.server.contract.WordAutocompleteResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val AUTOCOMPLETE_REQUEST_TIMEOUT_MILLIS = 5_000L
private const val MAX_AUTOCOMPLETE_PREFIX_LENGTH = 64
private const val MAX_AUTOCOMPLETE_RESULTS = 10
private val VALID_AUTOCOMPLETE_PREFIX = Regex("[a-z]{1,64}")

@Serializable
private data class AutocompleteErrorBody(val code: String)

class KlafServerWordAutocompleteRepository(
    baseUrl: String,
    private val httpClient: HttpClient,
    private val signer: KlafAuthenticatedRequestSigner?,
    private val selectedAccountEmail: Flow<String?>,
) : IWordAutocompleteRepository {
    private val endpoint = "${baseUrl.trimEnd('/')}/api/v1/autocomplete"
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun fetchAutocomplete(prefix: String): List<AutocompleteWord> {
        val normalized = prefix.trim().lowercase()
        if (normalized.length > MAX_AUTOCOMPLETE_PREFIX_LENGTH ||
            !VALID_AUTOCOMPLETE_PREFIX.matches(normalized)) return emptyList()
        val email = selectedAccountEmail.first()?.takeIf(String::isNotBlank) ?: return emptyList()

        return klafServerRequest(AUTOCOMPLETE_REQUEST_TIMEOUT_MILLIS) {
            val response = requestWithProof(email, normalized)
            val words = json.decodeFromString<WordAutocompleteResponse>(response.bodyAsText()).words
            check(words.size <= MAX_AUTOCOMPLETE_RESULTS && words.all { word ->
                word.startsWith(normalized) && word.all { it in 'a'..'z' }
            }) { "Invalid autocomplete response" }
            words.map(::AutocompleteWord)
        }
    }

    private suspend fun requestWithProof(email: String, prefix: String): HttpResponse {
        var nonce: String? = null
        for (attempt in 0..1) {
            val proof = signer?.headers(email, "GET", endpoint, nonce)
            val response = httpClient.get(endpoint) {
                parameter("prefix", prefix)
                proof?.let {
                    headers.append("Authorization", it.authorization)
                    headers.append("DPoP", it.dpop)
                }
            }
            if (response.status.value == 200) return response
            val code = runCatching { json.decodeFromString<AutocompleteErrorBody>(response.bodyAsText()).code }
                .getOrDefault("HTTP_ERROR")
            val challenge = response.headers["DPoP-Nonce"]
            if (signer != null && attempt == 0 && response.status.value == 401 &&
                code == "DPOP_NONCE_REQUIRED" && challenge != null && challenge.length in 16..128) {
                nonce = challenge
                continue
            }
            throw AccountOperationException(accountHttpFailure(response.status.value, code))
        }
        throw AccountOperationException(AccountFailure.INVALID_RESPONSE)
    }
}
