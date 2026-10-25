package com.kuts.klaf.di

import com.kuts.klaf.SecretConstants
import com.kuts.klaf.networking.klafServer.KlafServerHttpClientFactory
import com.kuts.klaf.networking.klafServer.KlafServerSession
import com.kuts.klaf.networking.klafServer.createDesktopAuthenticatedRequestSigner
import com.kuts.klaf.room.databases.DesktopSelectedAccountStore
import com.kuts.klaf.server.contract.VocabularySourceAnalyzeRequest
import com.kuts.klaf.server.contract.VocabularySourceAnalyzedMessage
import com.kuts.klaf.server.contract.KlafServerErrorMessage
import com.kuts.klaf.server.contract.KlafServerMessage
import com.kuts.klaf.server.contract.KlafServerErrorCode
import com.kuts.klaf.server.contract.VocabularySourceAnalysisDto
import com.kuts.klaf.server.contract.VocabularySourceAnalysisItemDto
import com.kuts.klaf.server.contract.VocabularySourceItemConfidenceDto
import com.kuts.klaf.server.contract.VocabularySourceItemOccurrenceDto
import com.kuts.klaf.server.contract.VocabularySourceItemPartOfSpeechDto
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.add
import org.junit.Assume.assumeTrue

/** Explicit opt-in: uses the real desktop login and sends a real Analyze request. No Room/sync/Save. */
class RemoteVocabularyComparisonTest {
    @Test
    fun retainStandaloneCoverageFailuresBeforeFailingTheRealRun() {
        val text = "They hand out buttons. Truman could tell which plants. They're constantly campaigning. " +
            "I'm the leader. Well, exercise is one of my election issues. Please take me to the park."
        val start = text.indexOf("buttons")
        val analysis = VocabularySourceAnalysisDto("en", listOf(VocabularySourceAnalysisItemDto(
            foreignWord = "button", nativeWord = "значок", originalText = "buttons",
            partOfSpeech = VocabularySourceItemPartOfSpeechDto.NOUN, cefrLevel = "B1",
            confidence = VocabularySourceItemConfidenceDto.HIGH, sourceExample = "They hand out buttons.",
            explanation = "Campaign context.", occurrences = listOf(VocabularySourceItemOccurrenceDto(
                startOffset = start, endOffset = start + "buttons".length, sentence = "They hand out buttons.",
            )),
        )))
        val root = Files.createTempDirectory("klaf-standalone-report-test")
        try {
            val file = saveResponse(VocabularySourceAnalyzedMessage("sample", analysis), text,
                "whole-text", root, System.nanoTime(), requireStandaloneCoverage = true)
            val report = Json.parseToJsonElement(Files.readString(file)).jsonObject
            assertEquals("standalone-coverage-failed", report.getValue("status").jsonPrimitive.content)
            assertTrue(report.getValue("missingStandaloneControls").toString().contains("take (transport)"))
            assertTrue(!report.getValue("missingStandaloneControls").toString().contains("button (campaign badge)"))
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun preserveEmptyAnalysisAndServerErrorForDiagnosticComparison() {
        val root = Files.createTempDirectory("klaf-comparison-report-test")
        try {
            val empty = saveResponse(VocabularySourceAnalyzedMessage("sample",
                VocabularySourceAnalysisDto("en", emptyList())), "A text.", "whole-text", root, System.nanoTime())
            val error = saveResponse(KlafServerErrorMessage("sample-error", KlafServerErrorCode.ASSISTANT_FAILED,
                "Analysis failed."), "A text.", "whole-text", root, System.nanoTime())
            assertEquals("quality-check-failed", Json.parseToJsonElement(Files.readString(empty)).jsonObject
                .getValue("status").jsonPrimitive.content)
            assertEquals("server-error", Json.parseToJsonElement(Files.readString(error)).jsonObject
                .getValue("status").jsonPrimitive.content)
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun collectRemoteAnalysisUsingExistingDesktopLogin() = runBlocking<Unit> {
        assumeTrue("Real remote analysis requires explicit opt-in",
            System.getenv("KLAF_VOCABULARY_REMOTE_COMPARISON") == "true")
        val mode = requireNotNull(System.getenv("KLAF_VOCABULARY_COMPARISON_MODE"))
        require(mode in setOf("chunked", "whole-text"))
        val input = Path.of(requireNotNull(System.getenv("KLAF_VOCABULARY_COMPARISON_INPUT")))
        val output = Path.of(requireNotNull(System.getenv("KLAF_VOCABULARY_COMPARISON_OUTPUT")))
        require(input.isAbsolute && output.isAbsolute)
        val text = Files.readString(input).trim()
        require(text.isNotBlank())
        val requireStandaloneCoverage = System.getenv("KLAF_VOCABULARY_COMPARISON_REQUIRE_STANDALONE") == "true"
        if (requireStandaloneCoverage) StandaloneVocabularyCoverage.marthaControls(text)
        Files.createDirectories(output)
        require(Files.isWritable(output)) { "Local comparison output directory must be writable." }
        val directory = File(System.getProperty("user.home"), ".klaf_kt")
        val email = requireNotNull(DesktopSelectedAccountStore(directory).read()) {
            "Sign in to the Desktop application first; credentials must not be passed in test arguments."
        }
        val endpoint = KlafServerEndpointConfig(SecretConstants.KlafServer.HOST,
            SecretConstants.KlafServer.PORT, SecretConstants.KlafServer.IS_SECURE)
        val http = KlafServerHttpClientFactory().create()
        val session = KlafServerSession(endpoint.host, endpoint.port, endpoint.isSecure, http,
            createDesktopAuthenticatedRequestSigner(endpoint.restBaseUrl(), directory, http), MutableStateFlow(email))
        val start = System.nanoTime()
        try {
            val response = withTimeout(60 * 60_000L) {
                session.request(VocabularySourceAnalyzeRequest(
                    requestId = session.nextRequestId("vocabulary-comparison"), cleanText = text,
                ))
            }
            val file = saveResponse(response, text, mode, output, start, requireStandaloneCoverage)
            if (response is KlafServerErrorMessage) {
                error("Remote analysis failed: code=${response.code}; ${response.message}")
            }
            check(response is VocabularySourceAnalyzedMessage) { "Unexpected remote response type" }
            val analysis = response.analysis
            assertTrue(validAnalysis(analysis, text), "Result failed quality checks; complete response retained in local report.")
            if (requireStandaloneCoverage) {
                val missing = StandaloneVocabularyCoverage.missing(analysis, StandaloneVocabularyCoverage.marthaControls(text), text)
                assertEquals(emptyList(), missing, "Missing standalone words; result saved. Contextual meanings still require review.")
            }
            println("Remote vocabulary result saved: ${file.fileName}; items=${analysis.items.size}; " +
                "occurrences=${analysis.items.sumOf { it.occurrences.size }}")
        } finally {
            withContext(NonCancellable) {
                try { session.disconnect() } finally { http.close() }
            }
        }
    }

    private fun saveResponse(
        response: KlafServerMessage,
        text: String,
        mode: String,
        output: Path,
        startedNanos: Long,
        requireStandaloneCoverage: Boolean = false,
    ): Path {
        val analysis = (response as? VocabularySourceAnalyzedMessage)?.analysis
        val missing = if (requireStandaloneCoverage && analysis != null) {
            StandaloneVocabularyCoverage.missing(analysis, StandaloneVocabularyCoverage.marthaControls(text), text)
        } else emptyList()
        val report = buildJsonObject {
            // Existing protocol does not echo the mode: it is set in the remote Run Configuration.
            put("operatorDeclaredMode", mode)
            put("serverModeVerified", false)
            put("createdAt", Instant.now().toString())
            put("elapsedMillis", (System.nanoTime() - startedNanos) / 1_000_000)
            put("textSha256", MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) })
            put("transcript", text)
            put("status", when {
                response is KlafServerErrorMessage -> "server-error"
                analysis == null -> "unexpected-response"
                !validAnalysis(analysis, text) -> "quality-check-failed"
                missing.isNotEmpty() -> "standalone-coverage-failed"
                else -> "success"
            })
            put("standaloneCoverageRequired", requireStandaloneCoverage)
            if (requireStandaloneCoverage) put("missingStandaloneControls", buildJsonArray { missing.forEach { add(it) } })
            if (response is VocabularySourceAnalyzedMessage) put("requestId", response.requestId)
            if (analysis != null) put("analysis", Json.encodeToJsonElement(analysis))
            if (response is KlafServerErrorMessage) {
                put("requestId", response.requestId)
                put("errorCode", response.code.name)
                put("errorMessage", response.message)
            }
        }
        val file = output.resolve("remote-$mode-${UUID.randomUUID()}.json")
        Files.writeString(file, report.toString(), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
        return file
    }

    private fun validAnalysis(analysis: VocabularySourceAnalysisDto, text: String): Boolean =
        analysis.language == "en" && analysis.items.isNotEmpty() && analysis.items.all { item ->
            item.occurrences.isNotEmpty() && item.occurrences.all {
                it.startOffset >= 0 && it.endOffset > it.startOffset && it.endOffset <= text.length
            }
        }
}
