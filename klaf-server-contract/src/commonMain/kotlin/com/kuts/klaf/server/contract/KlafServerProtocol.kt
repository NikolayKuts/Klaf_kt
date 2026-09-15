package com.kuts.klaf.server.contract

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

const val KLAF_SERVER_WEBSOCKET_PATH = "/ws"

@Serializable
sealed interface KlafServerClientMessage {
    val requestId: String
}

@Serializable
@SerialName("wordInsights.generate")
data class WordInsightsGenerateRequest(
    override val requestId: String,
    val word: String,
) : KlafServerClientMessage

@Serializable
@SerialName("mnemonic.association.generate")
data class MnemonicAssociationGenerateRequest(
    override val requestId: String,
    val word: String,
    val comment: String? = null,
    val excludedSoundAnchors: List<String> = emptyList(),
) : KlafServerClientMessage

@Serializable
@SerialName("mnemonic.image.generate")
data class MnemonicImageGenerateRequest(
    override val requestId: String,
    val selection: MnemonicSelectionDto,
    val comment: String? = null,
) : KlafServerClientMessage

@Serializable
sealed interface KlafServerMessage

@Serializable
@SerialName("server.ready")
data class KlafServerReadyMessage(
    val protocolVersion: Int = KLAF_SERVER_PROTOCOL_VERSION,
) : KlafServerMessage

@Serializable
@SerialName("wordInsights.generated")
data class WordInsightsGeneratedMessage(
    val requestId: String,
    val insights: WordMeaningInsightsDto,
) : KlafServerMessage

@Serializable
@SerialName("mnemonic.association.generated")
data class MnemonicAssociationGeneratedMessage(
    val requestId: String,
    val association: MnemonicAssociationDto,
) : KlafServerMessage

@Serializable
@SerialName("mnemonic.image.generated")
data class MnemonicImageGeneratedMessage(
    val requestId: String,
    val imageBase64: String,
) : KlafServerMessage

@Serializable
@SerialName("request.error")
data class KlafServerErrorMessage(
    val requestId: String? = null,
    val code: KlafServerErrorCode,
    val message: String,
) : KlafServerMessage

@Serializable
enum class KlafServerErrorCode {
    INVALID_MESSAGE,
    VALIDATION_FAILED,
    ASSISTANT_UNAVAILABLE,
    ASSISTANT_FAILED,
    INTERNAL_ERROR,
}

@Serializable
data class WordMeaningInsightsDto(
    val word: String,
    val language: String,
    val meanings: List<WordMeaningItemDto>,
)

@Serializable
data class WordMeaningItemDto(
    val translation: String,
    val cefr: String,
    val context: String,
    val frequencyRank: Int,
    val examples: List<String>,
)

@Serializable
data class MnemonicAssociationDto(
    val word: String = "",
    val transcription: String = "",
    val translations: List<String> = emptyList(),
    val usageExample: String = "",
    val candidates: List<MnemonicAssociationCandidateDto> = emptyList(),
)

@Serializable
data class MnemonicAssociationCandidateDto(
    val label: String = "",
    val targetTranslation: String = "",
    val soundAnchor: String = "",
    val secondarySoundAnchor: String? = null,
    val anchorCategory: String = "",
    val matchedPronunciationFragment: String = "",
    val secondaryMatchedPronunciationFragment: String? = null,
    val fingerMethodUsed: Boolean = false,
    val associationForm: String = "",
    val scene: String = "",
    val soundMapping: String = "",
    val meaningMapping: String = "",
)

@Serializable
data class MnemonicSelectionDto(
    val word: String = "",
    val transcription: String = "",
    val translations: List<String> = emptyList(),
    val usageExample: String = "",
    val candidate: MnemonicAssociationCandidateDto = MnemonicAssociationCandidateDto(),
)

const val KLAF_SERVER_PROTOCOL_VERSION = 1
