package com.example.domain.ipa

import kotlinx.serialization.Serializable

@Serializable
data class IpaHolder(
    val letterGroup: String,
    val ipa: String,
    val groupIndex: Int,
)