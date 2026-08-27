package com.kuts.klaf.cardManagement.common

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.kuts.domain.entities.CardMnemonic
import com.kuts.domain.entities.MnemonicImageAssetStorage
import com.kuts.domain.entities.MnemonicIllustration
import com.kuts.domain.entities.MnemonicSelection

data class MnemonicManagementUiState(
    val requestComment: TextFieldValue = TextFieldValue(),
    val imageRequestComment: TextFieldValue = TextFieldValue(),
    val variants: List<MnemonicVariantUiState> = emptyList(),
    val selectedVariantId: String? = null,
    val isExplicitlyCleared: Boolean = false,
    val isAssociationLoading: Boolean = false,
    val isImageLoading: Boolean = false,
    val speechInput: MnemonicSpeechInputUiState = MnemonicSpeechInputUiState(),
) {
    val selectedVariant: MnemonicVariantUiState?
        get() = variants.firstOrNull { variant -> variant.id == selectedVariantId }

    val hasSavableContent: Boolean
        get() = variants.isNotEmpty() || isExplicitlyCleared
}

data class MnemonicVariantUiState(
    val id: String,
    val selection: MnemonicSelection,
    val requestComment: String = "",
    val imageVariants: List<MnemonicImageVariantUiState> = emptyList(),
    val selectedImageId: String? = null,
) {
    val selectedImage: MnemonicImageVariantUiState?
        get() = imageVariants.firstOrNull { image -> image.id == selectedImageId }
}

data class MnemonicImageVariantUiState(
    val id: String,
    val assetId: String,
    val imagePath: String,
    val storage: MnemonicImageAssetStorage,
)

internal fun MnemonicManagementUiState.associationRequestCommentOrNull(): String? {
    return requestComment.text.trim().ifBlank { null }
}

internal fun MnemonicManagementUiState.imageRequestCommentOrNull(): String? {
    return imageRequestComment.text.trim().ifBlank { null }
}

internal fun MnemonicManagementUiState.clearComment(
    field: MnemonicCommentField,
): MnemonicManagementUiState = when (field) {
    MnemonicCommentField.ASSOCIATION -> copy(requestComment = TextFieldValue())
    MnemonicCommentField.IMAGE -> copy(imageRequestComment = TextFieldValue())
}

/**
 * Dictation adds to a comment instead of replacing it, so several phrases can be spoken one after
 * another and typing already done is never lost.
 */
internal fun MnemonicManagementUiState.appendRecognizedText(
    field: MnemonicCommentField,
    recognizedText: String,
): MnemonicManagementUiState = when (field) {
    MnemonicCommentField.ASSOCIATION -> {
        copy(requestComment = requestComment.withAppendedText(addition = recognizedText))
    }

    MnemonicCommentField.IMAGE -> {
        copy(imageRequestComment = imageRequestComment.withAppendedText(addition = recognizedText))
    }
}

internal fun TextFieldValue.withAppendedText(addition: String): TextFieldValue {
    val trimmedAddition = addition.trim()

    if (trimmedAddition.isEmpty()) return this

    val currentText = text.trimEnd()
    val updatedText = if (currentText.isEmpty()) {
        trimmedAddition
    } else {
        "$currentText $trimmedAddition"
    }

    return TextFieldValue(text = updatedText, selection = TextRange(index = updatedText.length))
}

internal fun MnemonicManagementUiState.excludedSoundAnchors(): List<String> {
    return variants
        .mapNotNull { variant -> variant.selection.candidate.soundAnchor.trim().ifBlank { null } }
        .distinctBy(String::lowercase)
}

internal fun MnemonicManagementUiState.startAssociationLoading(): MnemonicManagementUiState {
    return copy(
        isAssociationLoading = true,
        isExplicitlyCleared = false,
    )
}

internal fun MnemonicManagementUiState.stopAssociationLoading(): MnemonicManagementUiState {
    return copy(isAssociationLoading = false)
}

internal fun MnemonicManagementUiState.startImageLoading(): MnemonicManagementUiState {
    return copy(isImageLoading = true)
}

internal fun MnemonicManagementUiState.stopImageLoading(): MnemonicManagementUiState {
    return copy(isImageLoading = false)
}

internal fun MnemonicManagementUiState.appendVariants(
    newVariants: List<MnemonicVariantUiState>,
): MnemonicManagementUiState {
    return copy(
        variants = variants + newVariants,
        selectedVariantId = newVariants.firstOrNull()?.id ?: selectedVariantId,
        isExplicitlyCleared = false,
    )
}

internal fun MnemonicManagementUiState.appendImageVariant(
    variantId: String,
    imageVariant: MnemonicImageVariantUiState,
): MnemonicManagementUiState {
    return copy(
        variants = variants.map { variant ->
            if (variant.id == variantId) {
                variant.copy(
                    imageVariants = variant.imageVariants + imageVariant,
                    selectedImageId = imageVariant.id,
                )
            } else {
                variant
            }
        },
    )
}

internal fun MnemonicManagementUiState.selectVariantIfPresent(
    variantId: String,
): MnemonicManagementUiState {
    return if (variants.any { variant -> variant.id == variantId }) {
        copy(
            selectedVariantId = variantId,
            isExplicitlyCleared = false,
        )
    } else {
        this
    }
}

internal fun MnemonicManagementUiState.selectImageIfPresent(
    variantId: String,
    imageId: String,
): MnemonicManagementUiState {
    return copy(
        variants = variants.map { variant ->
            if (variant.id == variantId && variant.imageVariants.any { image -> image.id == imageId }) {
                variant.copy(selectedImageId = imageId)
            } else {
                variant
            }
        },
    )
}

internal fun MnemonicManagementUiState.resetGeneratedContent(
    markExplicitlyCleared: Boolean,
): MnemonicManagementUiState {
    return copy(
        variants = emptyList(),
        selectedVariantId = null,
        imageRequestComment = TextFieldValue(),
        isExplicitlyCleared = markExplicitlyCleared,
        isAssociationLoading = false,
        isImageLoading = false,
    )
}

internal fun MnemonicManagementUiState.draftAssetIds(): Set<String> {
    return variants
        .flatMap { variant -> variant.imageVariants }
        .filter { imageVariant -> imageVariant.storage == MnemonicImageAssetStorage.Draft }
        .map(MnemonicImageVariantUiState::assetId)
        .toSet()
}

internal fun MnemonicVariantUiState.toCardMnemonicPreview(): CardMnemonic {
    return CardMnemonic(
        selectedAssociation = selection,
        selectedIllustration = selectedImage?.assetId
            ?.takeIf(String::isNotBlank)
            ?.let { imageAssetId -> MnemonicIllustration(imageAssetId = imageAssetId) },
    )
}
