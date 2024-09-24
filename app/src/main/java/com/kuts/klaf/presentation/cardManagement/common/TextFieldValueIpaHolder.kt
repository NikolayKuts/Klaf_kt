package com.kuts.klaf.presentation.cardManagement.common

import androidx.compose.ui.text.input.TextFieldValue

data class TextFieldValueIpaHolder(
    val letterGroup: String,
    val ipaTextFieldValue: TextFieldValue,
    val groupIndex: Int,
)