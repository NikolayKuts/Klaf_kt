package com.kuts.klaf.networking.codexApp

import com.kuts.domain.entities.MnemonicAssociation
import com.kuts.domain.repositories.IMnemonicAssociationRepository
import com.kuts.klaf.common.MnemonicAssociationPayload
import com.kuts.klaf.common.MnemonicAssociationRequestPayload
import com.kuts.klaf.common.toDomainEntity
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json

class CodexAppMnemonicAssociationRepository(
    private val client: HttpClient,
    private val codexServerUrl: String,
    private val codexModel: String? = null,
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
        val trimmedComment = comment?.trim()?.ifBlank { null }
        val normalizedExcludedAnchors = excludedSoundAnchors
            .mapNotNull { anchor -> anchor.trim().ifBlank { null } }
            .distinctBy(String::toMnemonicKey)

        require(value = requestedWord.isNotBlank()) { "Mnemonic request word must not be blank." }

        val response = client.post(urlString = resolveMnemonicTextUrl(codexServerUrl)) {
            contentType(ContentType.Application.Json)
            setBody(
                body = json.encodeToString(
                    serializer = MnemonicAssociationRequestPayload.serializer(),
                    value = MnemonicAssociationRequestPayload(
                        word = requestedWord,
                        comment = trimmedComment,
                        excludedSoundAnchors = normalizedExcludedAnchors,
                        model = codexModel?.trim()?.ifBlank { null },
                    ),
                ),
            )
        }

        val responseBody = response.bodyAsText().trim()
        if (!response.status.isSuccess()) {
            throw IllegalArgumentException(
                responseBody.ifBlank { "Mnemonic association request failed." }.toSingleLineMessage()
            )
        }

        val payload = json.decodeFromString(
            deserializer = MnemonicAssociationPayload.serializer(),
            string = responseBody.unwrapMarkdownCodeFence(),
        )
        payload.validateForRequestedWord(requestedWord = requestedWord)
        return payload.toDomainEntity()
    }
}

private fun resolveMnemonicTextUrl(serverUrl: String): String {
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

    return "$httpBaseUrl/mnemonic/text"
}

private fun String.unwrapMarkdownCodeFence(): String = trim()
    .removePrefix("```json")
    .removePrefix("```")
    .removeSuffix("```")
    .trim()

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

private fun String.toSingleLineMessage(): String {
    return lineSequence()
        .firstOrNull()
        ?.trim()
        .orEmpty()
        .ifBlank { "Mnemonic association request failed." }
}
