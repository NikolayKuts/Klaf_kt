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

fun TextFieldValueIpaHolder.withTrimmedIpaText(): TextFieldValueIpaHolder {
    val trimmedIpa = ipaTextFieldValue.text.trim()

    return copy(ipaTextFieldValue = ipaTextFieldValue.copy(text = trimmedIpa))
}

fun List<TextFieldValueIpaHolder>.withTrimmedIpaText(): List<TextFieldValueIpaHolder> {
    return map(TextFieldValueIpaHolder::withTrimmedIpaText)
}

fun List<TextFieldValueIpaHolder>.toTrimmedDomainEntities(): List<IpaHolder> {
    return withTrimmedIpaText().map(TextFieldValueIpaHolder::toDomainEntity)
}
