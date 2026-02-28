package com.kuts.klaf.cardManagement.cardEditing

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
    val errorMessage: String = "",
    val isSheetVisible: Boolean = false,
) {
    val hasData: Boolean
        get() = meanings.isNotEmpty()

    val isLoading: Boolean
        get() = status == CardEditingInsightsStatus.Loading

    val isExpandable: Boolean
        get() = status == CardEditingInsightsStatus.Success
            || status == CardEditingInsightsStatus.Error
}
