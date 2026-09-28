package com.kuts.klaf.networking.klafServer

import com.kuts.domain.entities.MnemonicAssociation
import com.kuts.domain.entities.MnemonicAssociationResult
import com.kuts.domain.entities.MnemonicAssociationCandidate
import com.kuts.domain.managers.MnemonicGenerationSource
import com.kuts.domain.repositories.IMnemonicAssociationRepository
import com.kuts.klaf.server.contract.KlafServerErrorMessage
import com.kuts.klaf.server.contract.MnemonicAssociationCandidateDto
import com.kuts.klaf.server.contract.MnemonicAssociationDto
import com.kuts.klaf.server.contract.MnemonicAssociationGenerateRequest
import com.kuts.klaf.server.contract.MnemonicAssociationGeneratedMessage
import com.kuts.klaf.server.contract.MnemonicLaunchContextDto
import com.kuts.klaf.server.contract.MnemonicLaunchDestinationDto
import com.lib.lokdroid.core.logD
import com.lib.lokdroid.core.logE
import kotlinx.coroutines.CancellationException

class KlafServerMnemonicAssociationRepository(
    private val klafServerSession: IKlafServerSession,
) : IMnemonicAssociationRepository {

    override suspend fun fetchMnemonicAssociation(
        word: String,
        comment: String?,
        excludedSoundAnchors: List<String>,
        launchSource: MnemonicGenerationSource?,
    ): MnemonicAssociationResult {
        val requestedWord = word.trim()
        require(requestedWord.isNotBlank()) { "Mnemonic request word must not be blank." }

        val requestId = klafServerSession.nextRequestId(prefix = "mnemonic-association")
        val response = try {
            klafServerSession.request(
                message = MnemonicAssociationGenerateRequest(
                    requestId = requestId,
                    word = requestedWord,
                    comment = comment?.trim()?.ifBlank { null },
                    excludedSoundAnchors = excludedSoundAnchors
                        .mapNotNull { anchor -> anchor.trim().ifBlank { null } },
                    launchContext = launchSource?.toContractDto(),
                ),
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            logE("Klaf Server mnemonic association request failed: requestId=$requestId, failure=$throwable")
            throw IllegalArgumentException("Klaf Server request failed.", throwable)
        }

        return when (response) {
            is MnemonicAssociationGeneratedMessage -> {
                logD(
                    "Klaf Server mnemonic association received: requestId=$requestId, " +
                        "word=${response.association.word}, candidates=${response.association.candidates.size}, " +
                        "serverNotificationSent=${response.serverNotificationSent}",
                )
                MnemonicAssociationResult(
                    association = response.association.toDomainEntity(),
                    serverNotificationSent = response.serverNotificationSent,
                )
            }
            is KlafServerErrorMessage -> {
                logE(
                    "Klaf Server mnemonic association error: requestId=$requestId, " +
                        "code=${response.code}, message=${response.message}",
                )
                throw IllegalArgumentException(response.message)
            }
            else -> error("Unexpected Klaf Server response: ${response::class.simpleName}")
        }
    }
}

private fun MnemonicAssociationDto.toDomainEntity(): MnemonicAssociation =
    MnemonicAssociation(
        word = word,
        transcription = transcription,
        translations = translations,
        usageExample = usageExample,
        candidates = candidates.map { candidate -> candidate.toDomainEntity() },
    )

private fun MnemonicAssociationCandidateDto.toDomainEntity(): MnemonicAssociationCandidate =
    MnemonicAssociationCandidate(
        label = label,
        targetTranslation = targetTranslation,
        soundAnchor = soundAnchor,
        secondarySoundAnchor = secondarySoundAnchor,
        anchorCategory = anchorCategory,
        matchedPronunciationFragment = matchedPronunciationFragment,
        secondaryMatchedPronunciationFragment = secondaryMatchedPronunciationFragment,
        fingerMethodUsed = fingerMethodUsed,
        associationForm = associationForm,
        scene = scene,
        soundMapping = soundMapping,
        meaningMapping = meaningMapping,
    )

private fun MnemonicGenerationSource.toContractDto(): MnemonicLaunchContextDto =
    when (this) {
        is MnemonicGenerationSource.CardCreation -> {
            MnemonicLaunchContextDto(
                destination = MnemonicLaunchDestinationDto.CARD_ADDITION,
                deckId = deckId,
            )
        }

        is MnemonicGenerationSource.CardEditing -> {
            MnemonicLaunchContextDto(
                destination = MnemonicLaunchDestinationDto.CARD_EDITING,
                deckId = deckId,
                cardId = cardId,
            )
        }
    }
