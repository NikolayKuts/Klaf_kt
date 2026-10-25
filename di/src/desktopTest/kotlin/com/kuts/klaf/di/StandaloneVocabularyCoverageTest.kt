package com.kuts.klaf.di

import com.kuts.klaf.server.contract.VocabularySourceAnalysisDto
import com.kuts.klaf.server.contract.VocabularySourceAnalysisItemDto
import com.kuts.klaf.server.contract.VocabularySourceItemConfidenceDto
import com.kuts.klaf.server.contract.VocabularySourceItemOccurrenceDto
import com.kuts.klaf.server.contract.VocabularySourceItemPartOfSpeechDto
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assume.assumeTrue

internal data class StandaloneVocabularyControl(
    val label: String,
    val start: Int,
    val end: Int,
    val partOfSpeech: VocabularySourceItemPartOfSpeechDto,
)

internal object StandaloneVocabularyCoverage {
    private val boundaryCategories = setOf(
        CharCategory.CONNECTOR_PUNCTUATION, CharCategory.DASH_PUNCTUATION,
        CharCategory.START_PUNCTUATION, CharCategory.END_PUNCTUATION,
        CharCategory.INITIAL_QUOTE_PUNCTUATION, CharCategory.FINAL_QUOTE_PUNCTUATION,
        CharCategory.OTHER_PUNCTUATION,
    )

    fun missing(
        analysis: VocabularySourceAnalysisDto,
        controls: List<StandaloneVocabularyControl>,
        text: String,
    ): List<String> = controls.mapNotNull { control ->
        val covering = analysis.items.filter { item -> item.occurrences.any { occurrence ->
            occurrence.startOffset >= 0 && occurrence.endOffset <= text.length &&
                occurrence.startOffset <= control.start && occurrence.endOffset >= control.end &&
                text.substring(occurrence.startOffset, control.start).onlyBoundaries() &&
                text.substring(control.end, occurrence.endOffset).onlyBoundaries()
        } }
        when {
            covering.isEmpty() -> control.label
            covering.any { it.partOfSpeech == control.partOfSpeech } -> null
            else -> "${control.label} [part of speech: expected ${control.partOfSpeech}; got " +
                covering.map { it.partOfSpeech.name }.distinct().sorted().joinToString(", ") + "]"
        }
    }

    fun marthaControls(text: String): List<StandaloneVocabularyControl> = listOf(
        control(text, "button (campaign badge)", "They hand out buttons.", "buttons", VocabularySourceItemPartOfSpeechDto.NOUN),
        control(text, "tell (discern)", "Truman could tell which plants", "tell", VocabularySourceItemPartOfSpeechDto.VERB),
        control(text, "constantly", "They're constantly campaigning.", "constantly", VocabularySourceItemPartOfSpeechDto.ADVERB),
        control(text, "leader", "I'm the leader.", "leader", VocabularySourceItemPartOfSpeechDto.NOUN),
        control(text, "exercise", "exercise is one of my election issues.", "exercise", VocabularySourceItemPartOfSpeechDto.NOUN),
        control(text, "take (transport)", "take me to the park", "take", VocabularySourceItemPartOfSpeechDto.VERB),
    )

    private fun String.onlyBoundaries(): Boolean = all { it.isWhitespace() || it.category in boundaryCategories }

    private fun control(
        text: String,
        label: String,
        context: String,
        surface: String,
        part: VocabularySourceItemPartOfSpeechDto,
    ): StandaloneVocabularyControl {
        val contextStart = text.indexOf(context)
        require(contextStart >= 0 && contextStart == text.lastIndexOf(context)) {
            "Standalone controls require the preserved Martha transcript with unambiguous context: $label"
        }
        val start = contextStart + context.indexOf(surface)
        return StandaloneVocabularyControl(label, start, start + surface.length, part)
    }
}

class StandaloneVocabularyCoverageTest {
    @Test
    fun boundaryPunctuationDoesNotHideTheWordButAdditionalWordsDoNotCount() {
        val text = "I'm the leader."
        val control = StandaloneVocabularyControl("leader", 8, 14, VocabularySourceItemPartOfSpeechDto.NOUN)
        assertEquals(emptyList(), StandaloneVocabularyCoverage.missing(VocabularySourceAnalysisDto("en",
            listOf(item("leader", VocabularySourceItemPartOfSpeechDto.NOUN, 8, 15))), listOf(control), text))
        assertEquals(listOf("leader"), StandaloneVocabularyCoverage.missing(VocabularySourceAnalysisDto("en",
            listOf(item("the leader", VocabularySourceItemPartOfSpeechDto.NOUN, 4, 15))), listOf(control), text))
    }

    @Test
    fun wrongPartOfSpeechIsReportedAsMetadataInsteadOfAnOmittedWord() {
        val text = "They hand out buttons."
        val control = StandaloneVocabularyControl("campaign button", 14, 21, VocabularySourceItemPartOfSpeechDto.NOUN)
        val analysis = VocabularySourceAnalysisDto("en", listOf(item("button", VocabularySourceItemPartOfSpeechDto.VERB, 14, 21)))
        assertEquals(listOf("campaign button [part of speech: expected NOUN; got VERB]"),
            StandaloneVocabularyCoverage.missing(analysis, listOf(control), text))
    }

    @Test
    fun aPhraseOrExampleMentionDoesNotReplaceAnIndependentLexicalOccurrence() {
        val control = StandaloneVocabularyControl("campaign button", 14, 21, VocabularySourceItemPartOfSpeechDto.NOUN)
        val analysis = VocabularySourceAnalysisDto("en", listOf(
            item("hand out", VocabularySourceItemPartOfSpeechDto.PHRASAL_VERB, 5, 21),
            item("button", VocabularySourceItemPartOfSpeechDto.NOUN, 100, 107),
        ))
        assertEquals(listOf("campaign button"), StandaloneVocabularyCoverage.missing(analysis, listOf(control), "They hand out buttons."))
    }

    @Test
    fun inflectedSurfaceAndAlternativeDictionarySpellingCanCoverTheExactWord() {
        val control = StandaloneVocabularyControl("campaign button", 14, 21, VocabularySourceItemPartOfSpeechDto.NOUN)
        val analysis = VocabularySourceAnalysisDto("en", listOf(item("button", VocabularySourceItemPartOfSpeechDto.NOUN, 14, 21)))
        assertEquals(emptyList(), StandaloneVocabularyCoverage.missing(analysis, listOf(control), "They hand out buttons."))
    }

    @Test
    fun controlsRequireTheirContextInsteadOfSilentlyTestingAnUnrelatedInput() {
        assertFailsWith<IllegalArgumentException> { StandaloneVocabularyCoverage.marthaControls("An unrelated transcript.") }
    }

    @Test
    fun evaluateRecordedRealResponseForStandaloneWordCoverage() {
        val configured = System.getenv("KLAF_VOCABULARY_COVERAGE_REPORT")
        assumeTrue("Opt-in evaluation of a real recorded response", !configured.isNullOrBlank())
        val report = Json.parseToJsonElement(Files.readString(Path.of(requireNotNull(configured)))).jsonObject
        val text = report.getValue("transcript").jsonPrimitive.content
        val analysis = Json.decodeFromJsonElement<VocabularySourceAnalysisDto>(report.getValue("analysis"))
        val missing = StandaloneVocabularyCoverage.missing(analysis, StandaloneVocabularyCoverage.marthaControls(text), text)
        assertEquals(emptyList(), missing, "Missing independent lexical controls (sense accuracy still needs review)")
    }

    private fun item(
        word: String,
        part: VocabularySourceItemPartOfSpeechDto,
        start: Int,
        end: Int,
    ): VocabularySourceAnalysisItemDto = VocabularySourceAnalysisItemDto(
        foreignWord = word, nativeWord = "значение", originalText = "buttons", partOfSpeech = part,
        cefrLevel = null, confidence = VocabularySourceItemConfidenceDto.HIGH,
        sourceExample = "They hand out buttons.", explanation = "Campaign context.",
        occurrences = listOf(VocabularySourceItemOccurrenceDto(startOffset = start, endOffset = end, sentence = "They hand out buttons.")),
    )
}
