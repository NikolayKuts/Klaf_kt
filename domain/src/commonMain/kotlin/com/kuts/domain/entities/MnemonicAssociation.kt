package com.kuts.domain.entities

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
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

@Serializable
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

@Serializable
data class MnemonicSelection(
    val word: String = "",
    val transcription: String = "",
    val translations: List<String> = emptyList(),
    val usageExample: String = "",
    val candidate: MnemonicAssociationCandidate = MnemonicAssociationCandidate(),
)

@Serializable
data class MnemonicIllustration(
    @SerialName("imageUrl")
    val imageAssetId: String = "",
)

@Serializable
data class CardMnemonic(
    val selectedAssociation: MnemonicSelection? = null,
    val selectedIllustration: MnemonicIllustration? = null,
) {

    companion object {
        val EMPTY = CardMnemonic()
    }
}

fun MnemonicAssociation.toSelections(): List<MnemonicSelection> {
    return candidates.map { candidate ->
        MnemonicSelection(
            word = word,
            transcription = transcription,
            translations = translations,
            usageExample = usageExample,
            candidate = candidate,
        )
    }
}

enum class MnemonicImageAssetStorage {
    Draft,
    Saved,
}

data class MnemonicImageAsset(
    val assetId: String,
    val filePath: String,
    val storage: MnemonicImageAssetStorage,
)
