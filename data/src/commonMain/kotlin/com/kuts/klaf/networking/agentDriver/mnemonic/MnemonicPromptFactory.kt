package com.kuts.klaf.networking.agentDriver.mnemonic

import com.kuts.domain.entities.MnemonicSelection
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.agentdriver.project.protocol.ResponseSchema

/** One mnemonic-text request: the whole prompt, plus the schema the answer must satisfy. */
internal data class MnemonicTextPrompt(
    val prompt: String,
    val responseSchema: ResponseSchema,
)

/**
 * Builds the two mnemonic prompts.
 *
 * Each is three things in one text: what to do, how the answer must be shaped, and what is being
 * asked for this time.
 */
internal object MnemonicPromptFactory {

    private val json = Json { isLenient = true }

    fun buildTextPrompt(
        word: String,
        comment: String?,
        excludedSoundAnchors: List<String>,
    ): MnemonicTextPrompt {
        val commentBlock = comment?.let { value ->
            """
            Highest-priority caller comment for this request:
            "$value"

            Follow this comment as the main directive for anchor choice, persona, scene logic, and context.
            If this comment names a preferred figure, persona, character, or object, use that requested figure directly in the mnemonic instead of replacing it with a different convenient anchor.
            """.trimIndent()
        }.orEmpty()
        val excludedAnchorsBlock = excludedSoundAnchors
            .takeIf(List<String>::isNotEmpty)
            ?.joinToString(separator = ", ") { anchor -> "\"$anchor\"" }
            ?.let { anchors ->
                """
                Already used primary Russian sound anchors for this word:
                $anchors

                Do not reuse any of those anchors in the new result.
                """.trimIndent()
            }
            .orEmpty()

        val userPrompt = """
            Generate a mnemonic association for the English word "$word".
            Follow the provided mnemonic instruction exactly.
            $commentBlock
            $excludedAnchorsBlock
            Return structured JSON only.
        """.trimIndent()

        return MnemonicTextPrompt(
            prompt = joinPromptSections(
                MnemonicTextContract.instruction,
                MnemonicTextContract.developerRules,
                userPrompt,
            ),
            responseSchema = ResponseSchema(
                definition = json.parseToJsonElement(MnemonicTextContract.outputSchema).jsonObject,
            ),
        )
    }

    fun buildImagePrompt(
        selection: MnemonicSelection,
        comment: String?,
    ): String {
        val candidate = selection.candidate
        val secondaryAnchorBlock = candidate.secondarySoundAnchor
            ?.takeIf(String::isNotBlank)
            ?.let { secondaryAnchor ->
                """
                - Secondary sound anchor: $secondaryAnchor
                - Secondary pronunciation fragment: ${candidate.secondaryMatchedPronunciationFragment.orEmpty()}
                """.trimIndent()
            }
            .orEmpty()
        val commentBlock = comment?.let { value ->
            """
            Highest-priority caller comment:
            "$value"

            Follow this comment as the main directive for the visual persona, scene framing, and stylistic details.
            If this comment names a requested figure, persona, character, or object, include that requested figure directly in the image.
            """.trimIndent()
        }.orEmpty()

        val userPrompt = """
            Generate exactly one mnemonic illustration for the selected candidate below.

            Selected mnemonic data:
            - English word: ${selection.word}
            - IPA: ${selection.transcription}
            - Main translation: ${candidate.targetTranslation}
            - Association form: ${candidate.associationForm}
            - Primary sound anchor: ${candidate.soundAnchor}
            $secondaryAnchorBlock
            - Scene to render: ${candidate.scene}
            - Sound mapping: ${candidate.soundMapping}
            - Meaning mapping: ${candidate.meaningMapping}
            - Usage example: ${selection.usageExample}

            $commentBlock
        """.trimIndent()

        return joinPromptSections(
            MnemonicImageContract.instruction,
            MnemonicImageContract.developerRules,
            userPrompt,
        )
    }

    private fun joinPromptSections(vararg sections: String): String = sections
        .map(String::trim)
        .filter(String::isNotBlank)
        .joinToString(separator = "\n\n")
}
