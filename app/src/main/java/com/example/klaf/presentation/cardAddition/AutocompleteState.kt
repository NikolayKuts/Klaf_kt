package com.example.klaf.presentation.cardAddition

import com.example.klaf.domain.entities.AutocompleteWord

data class AutocompleteState(
    val prefix: String = "",
    val autocomplete: List<AutocompleteWord> = emptyList(),
    val isActive: Boolean = false,
)