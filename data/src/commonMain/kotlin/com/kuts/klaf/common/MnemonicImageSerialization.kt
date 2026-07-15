package com.kuts.klaf.common

import com.kuts.domain.entities.MnemonicAssociationCandidate
import com.kuts.domain.entities.MnemonicSelection
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class MnemonicImageRequestPayload(
    val selection: MnemonicSelectionPayload = MnemonicSelectionPayload(),
    val comment: String? = null,
)

@Serializable
internal data class MnemonicSelectionPayload(
    val word: String = "",
    val transcription: String = "",
    val translations: List<String> = emptyList(),
    @SerialName("usage_example")
    val usageExample: String = "",
    val candidate: MnemonicAssociationCandidatePayload = MnemonicAssociationCandidatePayload(),
)

internal fun MnemonicSelection.toPayload(): MnemonicSelectionPayload {
    return MnemonicSelectionPayload(
        word = word,
        transcription = transcription,
        translations = translations,
        usageExample = usageExample,
        candidate = candidate.toPayload(),
    )
}

private fun MnemonicAssociationCandidate.toPayload(): MnemonicAssociationCandidatePayload {
    return MnemonicAssociationCandidatePayload(
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
}
