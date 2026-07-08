package com.kuts.domain.entities

data class MnemonicAssociation(
    val word: String = "",
    val transcription: String = "",
    val translations: List<String> = emptyList(),
    val usageExample: String = "",
    val candidates: List<MnemonicAssociationCandidate> = emptyList(),
) {

    companion object {
        val EMPTY = MnemonicAssociation()
    }
}

data class MnemonicAssociationCandidate(
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
