package com.kuts.klaf.cardManagement.common

import androidx.compose.ui.text.input.TextFieldValue

data class TextFieldValueIpaHolder(
    val letterGroup: String,
    val ipaTextFieldValue: TextFieldValue,
    val groupIndex: Int,
    val isFocused: Boolean,
)