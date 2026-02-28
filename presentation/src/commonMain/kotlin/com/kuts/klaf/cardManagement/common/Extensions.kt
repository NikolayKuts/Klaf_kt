package com.kuts.klaf.cardManagement.common

import androidx.compose.ui.text.input.TextFieldValue
import com.kuts.domain.ipa.IpaHolder

fun IpaHolder.toTextFieldValueIpaHolder(): TextFieldValueIpaHolder = TextFieldValueIpaHolder(
    letterGroup = letterGroup,
    ipaTextFieldValue = TextFieldValue(text = ipa),
    groupIndex = groupIndex,
    isFocused = false,
)

fun TextFieldValueIpaHolder.toDomainEntity(): IpaHolder = IpaHolder(
    letterGroup = letterGroup,
    ipa = ipaTextFieldValue.text,
    groupIndex = groupIndex

)