package com.kuts.klaf.networking.klafServer

import com.kuts.domain.entities.MnemonicImageResult
import com.kuts.domain.entities.MnemonicAssociationCandidate
import com.kuts.domain.entities.MnemonicSelection
import com.kuts.domain.managers.MnemonicGenerationSource
import com.kuts.domain.repositories.IMnemonicImageRepository
import com.kuts.klaf.server.contract.KlafServerErrorMessage
import com.kuts.klaf.server.contract.MnemonicAssociationCandidateDto
import com.kuts.klaf.server.contract.MnemonicImageGenerateRequest
import com.kuts.klaf.server.contract.MnemonicImageGeneratedMessage
import com.kuts.klaf.server.contract.MnemonicLaunchContextDto
import com.kuts.klaf.server.contract.MnemonicLaunchDestinationDto
import com.kuts.klaf.server.contract.MnemonicSelectionDto
import com.lib.lokdroid.core.logD
import com.lib.lokdroid.core.logE
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.coroutines.CancellationException

class KlafServerMnemonicImageRepository(
    private val klafServerSession: IKlafServerSession,
) : IMnemonicImageRepository {

    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun fetchMnemonicImage(
        selection: MnemonicSelection,
        comment: String?,
        launchSource: MnemonicGenerationSource?,
    ): MnemonicImageResult {
        val normalizedSelection = selection.normalized()
        require(normalizedSelection.word.isNotBlank()) {
            "Mnemonic image request word must not be blank."
        }
        require(normalizedSelection.candidate.scene.isNotBlank()) {
            "Mnemonic image request scene must not be blank."
        }

        val requestId = klafServerSession.nextRequestId(prefix = "mnemonic-image")
        val response = try {
            klafServerSession.request(
                message = MnemonicImageGenerateRequest(
                    requestId = requestId,
                    selection = normalizedSelection.toContractDto(),
                    comment = comment?.trim()?.ifBlank { null },
                    launchContext = launchSource?.toContractDto(),
                ),
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            logE("Klaf Server mnemonic image request failed: requestId=$requestId, failure=$throwable")
            throw IllegalArgumentException("Klaf Server request failed.", throwable)
        }

        return when (response) {
            is MnemonicImageGeneratedMessage -> {
                val bytes = Base64.Default.decode(response.imageBase64)
                require(bytes.isNotEmpty()) { "Mnemonic image response is empty." }
                logD(
                    "Klaf Server mnemonic image received: requestId=$requestId, " +
                        "word=${normalizedSelection.word}, bytes=${bytes.size}, " +
                        "serverNotificationSent=${response.serverNotificationSent}",
                )
                MnemonicImageResult(
                    imageBytes = bytes,
                    serverNotificationSent = response.serverNotificationSent,
                )
            }
            is KlafServerErrorMessage -> {
                logE(
                    "Klaf Server mnemonic image error: requestId=$requestId, " +
                        "code=${response.code}, message=${response.message}",
                )
                throw IllegalArgumentException(response.message)
            }
            else -> error("Unexpected Klaf Server response: ${response::class.simpleName}")
        }
    }
}

private fun MnemonicSelection.toContractDto(): MnemonicSelectionDto =
    MnemonicSelectionDto(
        word = word,
        transcription = transcription,
        translations = translations,
        usageExample = usageExample,
        candidate = candidate.toContractDto(),
    )

private fun MnemonicAssociationCandidate.toContractDto(): MnemonicAssociationCandidateDto =
    MnemonicAssociationCandidateDto(
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

private fun MnemonicSelection.normalized(): MnemonicSelection = copy(
    word = word.trim(),
    transcription = transcription.trim(),
    translations = translations.map(String::trim).filter(String::isNotBlank),
    usageExample = usageExample.trim(),
    candidate = candidate.copy(
        label = candidate.label.trim(),
        targetTranslation = candidate.targetTranslation.trim(),
        soundAnchor = candidate.soundAnchor.trim(),
        secondarySoundAnchor = candidate.secondarySoundAnchor?.trim()?.ifBlank { null },
        anchorCategory = candidate.anchorCategory.trim(),
        matchedPronunciationFragment = candidate.matchedPronunciationFragment.trim(),
        secondaryMatchedPronunciationFragment = candidate.secondaryMatchedPronunciationFragment
            ?.trim()
            ?.ifBlank { null },
        associationForm = candidate.associationForm.trim(),
        scene = candidate.scene.trim(),
        soundMapping = candidate.soundMapping.trim(),
        meaningMapping = candidate.meaningMapping.trim(),
    ),
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
