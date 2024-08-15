package com.kuts.klaf.data.networking

import com.kuts.klaf.common.SecretConstants
import com.kuts.klaf.data.networking.nativeWordSuggestionsEntities.NativeWordSuggestions
import com.lib.lokdroid.core.logD
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import javax.inject.Inject

class NativeWordSuggestionClient @Inject constructor() {

    companion object {

        private const val BASE_URL = "https://dictionary.yandex.net/"
        private const val TIMEOUT = 10000L
    }

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(
                Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                }
            )
        }
        defaultRequest { url(urlString = BASE_URL) }
        engine { requestTimeout = TIMEOUT }
    }

    suspend fun retrieveSuggestions(word: String): List<String> {
        val suggestionsApiKey = SecretConstants.NATIVE_WORD_SUGGESTION_API_KEY
        val url = buildUrl(apiKey = suggestionsApiKey, word = word)
        val suggestionsAsString = client.get(url).body<String>()
        val suggestion = client.get(url).body<NativeWordSuggestions>()

        logD {
            message("retrieveSuggestions() called")
            message("suggestionsAsString = $suggestionsAsString")
        }

        return suggestion.def?.flatMap { def -> def.tr.map { tr -> tr.text } } ?: emptyList()
    }

    private fun buildUrl(apiKey: String, word: String): String {
        return "api/v1/dicservice.json/lookup?key=$apiKey&lang=en-ru&text=$word"
    }
}