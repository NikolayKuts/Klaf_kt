package com.kuts.klaf.cardManagement.cardEditing

import com.kuts.domain.entities.WordMeaningItem

data class CardEditingInsightsUiState(
    val word: String = "",
    val meanings: List<WordMeaningItem> = emptyList(),
    val isSheetVisible: Boolean = false,
) {
    val hasData: Boolean
        get() = meanings.isNotEmpty()
}

