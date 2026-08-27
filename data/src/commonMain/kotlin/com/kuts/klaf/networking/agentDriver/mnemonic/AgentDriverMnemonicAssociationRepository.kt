package com.kuts.klaf.networking.agentDriver.mnemonic

import com.kuts.domain.entities.MnemonicAssociation
import com.kuts.domain.repositories.IMnemonicAssociationRepository
import com.kuts.klaf.common.MnemonicAssociationPayload
import com.kuts.klaf.common.toDomainEntity
import com.kuts.klaf.networking.agentDriver.AgentDriverSession
import com.kuts.klaf.networking.agentDriver.describeAgentDriverFailureForLog
import com.kuts.klaf.networking.agentDriver.toShortAgentDriverMessage
import com.lib.lokdroid.core.logD
import com.lib.lokdroid.core.logE
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import org.agentdriver.project.protocol.TextGenerationRequest

/** Asks the assistant for a mnemonic association, and checks that the answer is one. */
class AgentDriverMnemonicAssociationRepository(
    private val agentDriverSession: AgentDriverSession,
) : IMnemonicAssociationRepository {

    @Suppress("OPT_IN_USAGE")
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    override suspend fun fetchMnemonicAssociation(
        word: String,
        comment: String?,
        excludedSoundAnchors: List<String>,
    ): MnemonicAssociation {
        val requestedWord = word.trim()
        require(value = requestedWord.isNotBlank()) { "Mnemonic request word must not be blank." }

        val (prompt, responseSchema) = MnemonicPromptFactory.buildTextPrompt(
            word = requestedWord,
            comment = comment?.trim()?.ifBlank { null },
            excludedSoundAnchors = excludedSoundAnchors
                .mapNotNull { anchor -> anchor.trim().ifBlank { null } }
                .distinctBy(String::toMnemonicKey),
        )

        val rawResponse = try {
            agentDriverSession.generateText(
                request = TextGenerationRequest(
                    prompt = prompt,
                    responseSchema = responseSchema,
                ),
            ).also { response ->
                logD("Mnemonic association for \"$requestedWord\": ${response.length} characters")
            }
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (throwable: Throwable) {
            logE(
                "Mnemonic association request failed: " +
                    throwable.describeAgentDriverFailureForLog()
            )
            throw IllegalArgumentException(
                "Mnemonic association request failed. ${throwable.toShortAgentDriverMessage()}",
                throwable,
            )
        }

        // Plain JSON, no code fence to strip: the request carried a response schema, and an answer
        // to one arrives as the object it describes.
        val payload = json.decodeFromString(
            deserializer = MnemonicAssociationPayload.serializer(),
            string = rawResponse,
        )
        payload.validateForRequestedWord(requestedWord = requestedWord)
        return payload.toDomainEntity()
    }
}

private fun MnemonicAssociationPayload.validateForRequestedWord(requestedWord: String) {
    require(word.toWordKey() == requestedWord.toWordKey()) {
        "Mnemonic response word mismatch. expected=$requestedWord, actual=$word"
    }
    require(transcription.isNotBlank()) { "Mnemonic transcription must not be blank." }
    require(translations.size in 3..5) { "Mnemonic translations count is out of range." }
    require(usageExample.isNotBlank()) { "Mnemonic usage example must not be blank." }
    require(mnemonicCandidates.size in 1..2) { "Mnemonic candidates count is out of range." }
    mnemonicCandidates.forEach { candidate ->
        require(candidate.label.isNotBlank()) { "Mnemonic candidate label must not be blank." }
        require(candidate.targetTranslation.isNotBlank()) {
            "Mnemonic candidate target translation must not be blank."
        }
        require(candidate.soundAnchor.isNotBlank()) { "Mnemonic sound anchor must not be blank." }
        require(candidate.anchorCategory.isNotBlank()) { "Mnemonic anchor category must not be blank." }
        require(candidate.matchedPronunciationFragment.isNotBlank()) {
            "Mnemonic matched pronunciation fragment must not be blank."
        }
        require(candidate.associationForm.isNotBlank()) {
            "Mnemonic association form must not be blank."
        }
        require(candidate.scene.isNotBlank()) { "Mnemonic scene must not be blank." }
        require(candidate.soundMapping.isNotBlank()) { "Mnemonic sound mapping must not be blank." }
        require(candidate.meaningMapping.isNotBlank()) {
            "Mnemonic meaning mapping must not be blank."
        }
    }
}

private fun String.toWordKey(): String = trim().lowercase()
private fun String.toMnemonicKey(): String = trim().lowercase()
