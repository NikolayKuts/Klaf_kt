package com.kuts.domain.entities

data class MnemonicAssociationResult(
    val association: MnemonicAssociation,
    val serverNotificationSent: Boolean = false,
)

data class MnemonicImageResult(
    val imageBytes: ByteArray,
    val serverNotificationSent: Boolean = false,
)

data class VocabularySourceAnalysisResult(
    val analysis: VocabularySourceAnalysis,
    val serverNotificationSent: Boolean = false,
)
