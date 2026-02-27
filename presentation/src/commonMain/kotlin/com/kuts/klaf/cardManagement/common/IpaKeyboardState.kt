package com.kuts.klaf.cardManagement.common

import androidx.compose.ui.text.input.TextFieldValue

data class IpaKeyboardState(
    val keys: List<String> = emptyList(),
    val enabled: Boolean = false,
    val holderIndex: Int? = null,
    val ipaTextFieldValue: TextFieldValue? = null
)
