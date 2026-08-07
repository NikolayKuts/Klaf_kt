package com.kuts.klaf.common

import com.kuts.domain.entities.MnemonicAssociation
import com.kuts.domain.entities.MnemonicAssociationCandidate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class MnemonicAssociationPayload(
    val word: String = "",
    val transcription: String = "",
    val translations: List<String> = emptyList(),
    @SerialName("usage_example")
    val usageExample: String = "",
    @SerialName("mnemonic_candidates")
    val mnemonicCandidates: List<MnemonicAssociationCandidatePayload> = emptyList(),
)

@Serializable
internal data class MnemonicAssociationCandidatePayload(
    val label: String = "",
    @SerialName("target_translation")
    val targetTranslation: String = "",
    @SerialName("sound_anchor")
    val soundAnchor: String = "",
    @SerialName("secondary_sound_anchor")
    val secondarySoundAnchor: String? = null,
    @SerialName("anchor_category")
    val anchorCategory: String = "",
    @SerialName("matched_pronunciation_fragment")
    val matchedPronunciationFragment: String = "",
    @SerialName("secondary_matched_pronunciation_fragment")
    val secondaryMatchedPronunciationFragment: String? = null,
    @SerialName("finger_method_used")
    val fingerMethodUsed: Boolean = false,
    @SerialName("association_form")
    val associationForm: String = "",
    val scene: String = "",
    @SerialName("sound_mapping")
    val soundMapping: String = "",
    @SerialName("meaning_mapping")
    val meaningMapping: String = "",
)

internal fun MnemonicAssociationPayload.toDomainEntity(): MnemonicAssociation {
    return MnemonicAssociation(
        word = word,
        transcription = transcription,
        translations = translations,
        usageExample = usageExample,
        candidates = mnemonicCandidates.map { candidate -> candidate.toDomainEntity() },
    )
}

private fun MnemonicAssociationCandidatePayload.toDomainEntity(): MnemonicAssociationCandidate {
    return MnemonicAssociationCandidate(
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
