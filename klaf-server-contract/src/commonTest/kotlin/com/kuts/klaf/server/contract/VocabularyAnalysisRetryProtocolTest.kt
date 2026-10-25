package com.kuts.klaf.server.contract

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class VocabularyAnalysisRetryProtocolTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun retryTokenSurvivesErrorAndAnalyzeRequestRoundTrips() {
        val error = json.decodeFromString<KlafServerErrorMessage>(
            """{"requestId":"r1","code":"ASSISTANT_FAILED","message":"Try again","analysisRetryToken":"opaque-token"}""",
        )
        val errorJson = json.parseToJsonElement(json.encodeToString(KlafServerErrorMessage.serializer(), error)).jsonObject
        assertEquals("opaque-token", errorJson["analysisRetryToken"]?.jsonPrimitive?.content)
        val request = json.decodeFromString<VocabularySourceAnalyzeRequest>(
            """{"requestId":"r2","cleanText":"Original text","analysisRetryToken":"opaque-token"}""",
        )
        val requestJson = json.parseToJsonElement(json.encodeToString(VocabularySourceAnalyzeRequest.serializer(), request)).jsonObject
        assertEquals("opaque-token", requestJson["analysisRetryToken"]?.jsonPrimitive?.content)
    }
}
