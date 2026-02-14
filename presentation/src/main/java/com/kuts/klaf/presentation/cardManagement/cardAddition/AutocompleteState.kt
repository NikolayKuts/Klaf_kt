package com.kuts.klaf.presentation.cardManagement.cardAddition

import com.kuts.domain.entities.AutocompleteWord

data class AutocompleteState(
    val prefix: String = "",
    val autocomplete: List<AutocompleteWord> = emptyList(),
    val isActive: Boolean = false,
)