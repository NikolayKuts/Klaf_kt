package com.kuts.klaf.networking.agentDriver.mnemonic

import com.kuts.domain.entities.MnemonicSelection
import com.kuts.domain.repositories.IMnemonicImageRepository
import com.kuts.klaf.networking.agentDriver.AgentDriverSession
import com.kuts.klaf.networking.agentDriver.toShortAgentDriverMessage
import com.lib.lokdroid.core.logD
import com.lib.lokdroid.core.logE
import kotlinx.coroutines.CancellationException
import org.agentdriver.project.protocol.ImageGenerationRequest

/** Asks the assistant to draw the mnemonic the user picked. */
class AgentDriverMnemonicImageRepository(
    private val agentDriverSession: AgentDriverSession,
) : IMnemonicImageRepository {

    override suspend fun fetchMnemonicImage(
        selection: MnemonicSelection,
        comment: String?,
    ): ByteArray {
        val normalizedSelection = selection.normalized()
        require(value = normalizedSelection.word.isNotBlank()) {
            "Mnemonic image request word must not be blank."
        }
        require(value = normalizedSelection.candidate.scene.isNotBlank()) {
            "Mnemonic image request scene must not be blank."
        }

        val prompt = MnemonicPromptFactory.buildImagePrompt(
            selection = normalizedSelection,
            comment = comment?.trim()?.ifBlank { null },
        )

        val imageBytes = try {
            agentDriverSession.generateImage(
                request = ImageGenerationRequest(prompt = prompt),
            ).also { bytes ->
                logD("Mnemonic image for \"${normalizedSelection.word}\": ${bytes.size} bytes")
            }
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (throwable: Throwable) {
            logE("Mnemonic image request failed: ${throwable::class.simpleName} -- $throwable")
            throw IllegalArgumentException(
                "Mnemonic image request failed. ${throwable.toShortAgentDriverMessage()}",
                throwable,
            )
        }

        require(value = imageBytes.isNotEmpty()) { "Mnemonic image response is empty." }
        return imageBytes
    }
}

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
