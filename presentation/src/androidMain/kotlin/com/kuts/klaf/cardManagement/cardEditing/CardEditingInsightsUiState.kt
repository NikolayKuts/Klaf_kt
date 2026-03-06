package com.kuts.klaf.cardManagement.cardEditing

import androidx.annotation.StringRes
import com.kuts.domain.entities.WordMeaningItem

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
    @StringRes val errorMessageResId: Int? = null,
    val refreshedMeanings: List<WordMeaningItem> = emptyList(),
    @StringRes val refreshedErrorMessageResId: Int? = null,
    val isRefreshing: Boolean = false,
    val isApplyingRefreshed: Boolean = false,
    val isSheetVisible: Boolean = false,
) {
    val hasData: Boolean
        get() = meanings.isNotEmpty()

    val isLoading: Boolean
        get() = status == CardEditingInsightsStatus.Loading

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
