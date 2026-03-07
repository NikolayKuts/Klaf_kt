package com.kuts.klaf.cardManagement.cardEditing

import com.kuts.domain.entities.WordMeaningItem
import org.jetbrains.compose.resources.StringResource

enum class CardEditingInsightsStatus {
    Idle,
    Loading,
    Success,
    Error,
}

data class CardEditingInsightsUiState(
    val word: String = "",
    val meanings: List<WordMeaningItem> = emptyList(),
    val status: CardEditingInsightsStatus = CardEditingInsightsStatus.Idle,
    val errorMessageResId: StringResource? = null,
    val refreshedMeanings: List<WordMeaningItem> = emptyList(),
    val refreshedErrorMessageResId: StringResource? = null,
    val isRefreshing: Boolean = false,
    val isApplyingRefreshed: Boolean = false,
    val isSheetVisible: Boolean = false,
) {
    val isExpandable: Boolean
        get() = status == CardEditingInsightsStatus.Success
            || status == CardEditingInsightsStatus.Error

    val hasRefreshedData: Boolean
        get() = refreshedMeanings.isNotEmpty()

    val canRequestRefreshedInsights: Boolean
        get() = status == CardEditingInsightsStatus.Success
            && !isRefreshing
            && !isApplyingRefreshed

    val canApplyRefreshedInsights: Boolean
        get() = hasRefreshedData
            && !isRefreshing
            && !isApplyingRefreshed
}
