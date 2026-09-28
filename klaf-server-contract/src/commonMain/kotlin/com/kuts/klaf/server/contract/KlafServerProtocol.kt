package com.kuts.klaf.server.contract

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

const val KLAF_SERVER_WEBSOCKET_PATH = "/ws"
const val KLAF_SERVER_PROTOCOL_VERSION = 7

data class AudioUploadFrame(
    val requestId: String,
    val audioChunk: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AudioUploadFrame) return false
        if (requestId != other.requestId) return false
        return audioChunk.contentEquals(other.audioChunk)
    }

    override fun hashCode(): Int {
        var result = requestId.hashCode()
        result = 31 * result + audioChunk.contentHashCode()
        return result
    }
}

object AudioUploadFrameCodec {
    fun encode(requestId: String, audioChunk: ByteArray): ByteArray {
        val idBytes = requestId.encodeToByteArray()
        require(requestId.isNotBlank() && idBytes.size <= Short.MAX_VALUE) { "Invalid requestId length" }
        val result = ByteArray(2 + idBytes.size + audioChunk.size)
        result[0] = ((idBytes.size ushr 8) and 0xFF).toByte()
        result[1] = (idBytes.size and 0xFF).toByte()
        idBytes.copyInto(destination = result, destinationOffset = 2)
        audioChunk.copyInto(destination = result, destinationOffset = 2 + idBytes.size)
        return result
    }

    fun decode(frameBytes: ByteArray): AudioUploadFrame? {
        if (frameBytes.size < 2) return null
        val idLength = (((frameBytes[0].toInt() and 0xFF) shl 8) or (frameBytes[1].toInt() and 0xFF))
        if (idLength !in 1..Short.MAX_VALUE || frameBytes.size < 2 + idLength) return null
        val requestId = try {
            frameBytes.decodeToString(startIndex = 2, endIndex = 2 + idLength, throwOnInvalidSequence = true)
        } catch (_: CharacterCodingException) {
            return null
        }
        if (requestId.isBlank()) return null
        val chunk = frameBytes.copyOfRange(fromIndex = 2 + idLength, toIndex = frameBytes.size)
        return AudioUploadFrame(requestId = requestId, audioChunk = chunk)
    }
}

@Serializable
sealed interface KlafServerClientMessage {
    val requestId: String
}

@Serializable
@SerialName("wordInsights.generate")
data class WordInsightsGenerateRequest(
    override val requestId: String,
    val word: String,
    val launchContext: CardLaunchContextDto? = null,
) : KlafServerClientMessage

@Serializable
@SerialName("mnemonic.association.generate")
data class MnemonicAssociationGenerateRequest(
    override val requestId: String,
    val word: String,
    val comment: String? = null,
    val excludedSoundAnchors: List<String> = emptyList(),
    val launchContext: MnemonicLaunchContextDto? = null,
) : KlafServerClientMessage

@Serializable
@SerialName("mnemonic.image.generate")
data class MnemonicImageGenerateRequest(
    override val requestId: String,
    val selection: MnemonicSelectionDto,
    val comment: String? = null,
    val launchContext: MnemonicLaunchContextDto? = null,
) : KlafServerClientMessage

@Serializable
@SerialName("vocabularySource.analyze")
data class VocabularySourceAnalyzeRequest(
    override val requestId: String,
    val cleanText: String,
    val sourceId: Int? = null,
    val sourceTitle: String? = null,
) : KlafServerClientMessage

@Serializable
@SerialName("vocabularySource.transcribe.start")
data class VocabularySourceTranscribeStartRequest(
    override val requestId: String,
    val sourceId: Int? = null,
    val sourceTitle: String? = null,
    val fileName: String,
    val audioFormat: String,
    val declaredByteSize: Long,
    val clientSessionId: String? = null,
) : KlafServerClientMessage

@Serializable
@SerialName("vocabularySource.transcribe.complete")
data class VocabularySourceTranscribeCompleteRequest(
    override val requestId: String,
) : KlafServerClientMessage

@Serializable
@SerialName("request.cancel")
data class KlafServerCancelRequest(
    override val requestId: String,
    val targetRequestId: String,
) : KlafServerClientMessage

@Serializable
@SerialName("pushToken.register")
data class PushTokenRegisterRequest(
    override val requestId: String,
    val token: String,
    val clientSessionId: String? = null,
    val platform: PushTokenPlatformDto = PushTokenPlatformDto.ANDROID,
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
    val serverNotificationSent: Boolean = false,
) : KlafServerMessage

@Serializable
@SerialName("mnemonic.association.generated")
data class MnemonicAssociationGeneratedMessage(
    val requestId: String,
    val association: MnemonicAssociationDto,
    val serverNotificationSent: Boolean = false,
) : KlafServerMessage

@Serializable
@SerialName("mnemonic.image.generated")
data class MnemonicImageGeneratedMessage(
    val requestId: String,
    val imageBase64: String,
    val serverNotificationSent: Boolean = false,
) : KlafServerMessage

@Serializable
@SerialName("vocabularySource.analyzed")
data class VocabularySourceAnalyzedMessage(
    val requestId: String,
    val analysis: VocabularySourceAnalysisDto,
    val serverNotificationSent: Boolean = false,
) : KlafServerMessage

@Serializable
@SerialName("vocabularySource.transcribe.uploadProgress")
data class VocabularySourceTranscribeUploadProgressMessage(
    val requestId: String,
    val uploadedBytes: Long,
    val totalBytes: Long,
) : KlafServerMessage

@Serializable
@SerialName("vocabularySource.transcribe.recognitionProgress")
data class VocabularySourceTranscribeRecognitionProgressMessage(
    val requestId: String,
    val completedChunks: Int,
    val totalChunks: Int,
) : KlafServerMessage

@Serializable
@SerialName("vocabularySource.transcribed")
data class VocabularySourceTranscribedMessage(
    val requestId: String,
    val transcript: String,
    val segments: List<SpeechToTextSegmentDto> = emptyList(),
    val serverNotificationSent: Boolean = false,
) : KlafServerMessage

@Serializable
@SerialName("pushToken.registered")
data class PushTokenRegisteredMessage(
    val requestId: String,
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
    CANCELLED,
    ASSISTANT_UNAVAILABLE,
    ASSISTANT_FAILED,
    INTERNAL_ERROR,
}

@Serializable
enum class PushTokenPlatformDto {
    ANDROID,
}

@Serializable
data class MnemonicLaunchContextDto(
    val destination: MnemonicLaunchDestinationDto,
    val deckId: Int,
    val cardId: Int? = null,
)

@Serializable
enum class MnemonicLaunchDestinationDto {
    CARD_ADDITION,
    CARD_EDITING,
}

@Serializable
data class CardLaunchContextDto(
    val deckId: Int,
    val cardId: Int,
)

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

@Serializable
data class VocabularySourceAnalysisDto(
    val language: String,
    val items: List<VocabularySourceAnalysisItemDto>,
)

@Serializable
data class VocabularySourceAnalysisItemDto(
    val foreignWord: String,
    val transcription: String = "",
    val nativeWord: String,
    val originalText: String,
    val partOfSpeech: VocabularySourceItemPartOfSpeechDto,
    val cefrLevel: String?,
    val confidence: VocabularySourceItemConfidenceDto,
    val sourceExample: String,
    val explanation: String,
    val occurrences: List<VocabularySourceItemOccurrenceDto>,
)

@Serializable
data class VocabularySourceItemOccurrenceDto(
    val timestamp: String = "",
    val startOffset: Int = 0,
    val endOffset: Int = 0,
    val sentence: String = "",
)

@Serializable
data class SpeechToTextSegmentDto(
    val startMillis: Long,
    val endMillis: Long,
    val text: String,
)

@Serializable
enum class VocabularySourceItemConfidenceDto {
    LOW,
    MEDIUM,
    HIGH,
}

@Serializable
enum class VocabularySourceItemPartOfSpeechDto {
    UNKNOWN,
    NOUN,
    VERB,
    ADJECTIVE,
    ADVERB,
    PHRASAL_VERB,
    PHRASE,
    IDIOM,
    OTHER,
}
