package com.kuts.klaf.cardManagement.common

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
